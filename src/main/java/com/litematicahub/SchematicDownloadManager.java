package com.litematicahub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SchematicDownloadManager {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final String API_BASE = System.getProperty(
            "litematicahub.api", "https://litematica-online-hub.shop/api/v1");
    private static final String SITE_ORIGIN = API_BASE.endsWith("/api/v1")
            ? API_BASE.substring(0, API_BASE.length() - "/api/v1".length())
            : API_BASE;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /** Resolved thumbnail URL -> registered texture (render-thread read, any-thread write). */
    private static final Map<String, Identifier> THUMBNAILS = new ConcurrentHashMap<>();
    private static final Set<String> THUMBNAIL_LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> THUMBNAIL_FAILED = ConcurrentHashMap.newKeySet();

    private SchematicDownloadManager() {
    }

    public static final class SchematicEntry {
        public String id = "";
        public String title = "Untitled";
        public String author = "Unknown";
        public String tag = "";
        public String version = "";
        public String description = "";
        public int downloads;
        public int likes;
        public String downloadUrl = "";
        public String thumbnailUrl = "";
        /** all cover images for this entry (from the site's screenshots array) */
        public final List<String> covers = new ArrayList<>();
    }

    public interface DownloadCallback {
        void onResult(boolean success, String message);
    }

    /** Reports how much of a download has already arrived; -1 when the size is unknown. */
    public interface ProgressCallback {
        void onProgress(long bytesRead, long totalBytes);
    }

    private record DownloadResult(boolean ok, String message) {
    }

    public static Path getSchematicsDirectory() {
        try {
            Class<?> dataManager = Class.forName("fi.dy.masa.litematica.data.DataManager");
            Object directory = dataManager.getMethod("getSchematicsBaseDirectory").invoke(null);
            if (directory instanceof Path path) {
                return path;
            }
        } catch (ReflectiveOperationException exception) {
            ModInit.LOGGER.warn("Could not ask Litematica for its schematics directory", exception);
        }
        return Minecraft.getInstance().gameDirectory.toPath().resolve("schematics");
    }

    public static void initCacheDirectory() {
        try {
            Files.createDirectories(getSchematicsDirectory());
        } catch (Exception exception) {
            ModInit.LOGGER.error("Could not create the Litematica schematics directory", exception);
        }
    }

    /** Same grouped tag list the website uses, so filters match the site exactly. */
    public static final String[][] TAG_GROUPS = {
            {"Buildings", "House,Cottage,Mansion,Castle,Palace,Fortress,Tower,Dungeon,Temple,Church,Library,Market,Tavern,Arena,Stadium,Factory,Warehouse,Laboratory,Greenhouse,Stable,Lighthouse"},
            {"Exterior", "Bridge,Road,Wall,Gate,Roof,Fence,Statue,Fountain,Park,Garden"},
            {"Interior", "Interior,Furniture,Kitchen,Storage Room,Enchanting Room,Brewery"},
            {"Farms", "Farm,Crop Farm,Animal Farm,Mob Farm,Tree Farm,Fishing,Iron Farm,Gold Farm,XP Farm,Raid Farm,Trading Hall"},
            {"Redstone", "Redstone,Contraption,Sorting System,Smelter,Automatic Door,Elevator,Machine,Generator"},
            {"Style", "Medieval,Modern,Fantasy,Sci-Fi,Steampunk,Victorian,Gothic,Rustic,Oriental,Nordic,Elven,Dwarven"},
            {"Nature", "Terrain,Landscape,Mountain,Cave,Ocean,Desert,Jungle,Forest,Snow,Nether,End,Underwater"},
            {"Other", "Village,City,Starter Base,Base,Pixel Art,Minigame,Mega Build,Compact"}
    };

    /**
     * Live tag registry from the website, so tags users created themselves show up here too.
     * Falls back to the built-in list when the server is unreachable.
     */
    private static volatile String[][] liveTagGroups;
    private static volatile long tagFetchedAt;

    private static String[][] tagGroups() {
        String[][] live = liveTagGroups;
        return live != null ? live : TAG_GROUPS;
    }

    /** Pulls the current tag registry in the background. Safe to call repeatedly. */
    public static void refreshTags() {
        long now = System.currentTimeMillis();
        if (now - tagFetchedAt < 60_000L) return; // don't hammer the API
        tagFetchedAt = now;
        Thread t = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest req = HttpRequest.newBuilder(URI.create(API_BASE + "/tags"))
                        .timeout(Duration.ofSeconds(15)).GET().build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() / 100 != 2) return;

                JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
                JsonArray groups = root.getAsJsonArray("groups");
                if (groups == null) return;

                List<String[]> parsed = new ArrayList<>();
                for (JsonElement el : groups) {
                    if (!el.isJsonObject()) continue;
                    JsonObject g = el.getAsJsonObject();
                    String name = g.has("name") ? g.get("name").getAsString() : null;
                    JsonArray tags = g.getAsJsonArray("tags");
                    if (name == null || tags == null || tags.size() == 0) continue;
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement tg : tags) {
                        if (sb.length() > 0) sb.append(',');
                        sb.append(tg.getAsString());
                    }
                    parsed.add(new String[]{name, sb.toString()});
                }
                if (!parsed.isEmpty()) {
                    liveTagGroups = parsed.toArray(new String[0][]);
                    ModInit.LOGGER.info("Loaded " + parsed.size() + " tag groups from server");
                }
            } catch (Exception e) {
                ModInit.LOGGER.warn("Could not load tags from server: " + e.getClass().getSimpleName());
            }
        }, "litematicahub-tags");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Rows for the tag dropdown: {"group", name} headers and {"tag", value} selectable items.
     * When a filter is given, group headers are hidden and only matches are returned.
     */
    public static List<String[]> tagRows(String filter) {
        List<String[]> rows = new ArrayList<>();
        String f = filter == null ? "" : filter.trim().toLowerCase();
        if (f.isEmpty()) rows.add(new String[]{"tag", "All"});
        for (String[] group : tagGroups()) {
            List<String> matches = new ArrayList<>();
            for (String tag : group[1].split(",")) {
                if (f.isEmpty() || tag.toLowerCase().contains(f)) matches.add(tag);
            }
            if (matches.isEmpty()) continue;
            if (f.isEmpty()) rows.add(new String[]{"group", group[0]});
            for (String tag : matches) rows.add(new String[]{"tag", tag});
        }
        if (rows.isEmpty()) rows.add(new String[]{"empty", "No tags found"});
        return rows;
    }

    /** One page of results plus the total number of matching entries on the server. */
    public static final class Page {
        public final List<SchematicEntry> entries;
        public final int total;

        Page(List<SchematicEntry> entries, int total) {
            this.entries = entries;
            this.total = total;
        }
    }

    public static CompletableFuture<Page> fetchSchematics(String query, String tag, String version,
                                                          int limit, int offset) {
        StringBuilder url = new StringBuilder(API_BASE)
                .append("/schematics?q=").append(encode(query))
                .append("&tag=").append(encode(tag))
                .append("&version=").append(encode(version));
        if (limit > 0) url.append("&limit=").append(limit);
        if (offset > 0) url.append("&offset=").append(offset);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "LitematicaOnlineHub/1.0")
                .GET().build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("gallery returned HTTP " + response.statusCode());
                    }
                    return parsePage(response.body());
                });
    }

    private static Page parsePage(String body) {
        JsonElement root = JsonParser.parseString(body);
        JsonObject object = root.isJsonObject() ? root.getAsJsonObject() : null;
        JsonArray array = root.isJsonArray() ? root.getAsJsonArray()
                : (object != null ? object.getAsJsonArray("data") : null);
        List<SchematicEntry> entries = new ArrayList<>();
        if (array != null) entries.addAll(parseEntriesFrom(array));
        int total = entries.size();
        if (object != null) {
            JsonElement t = object.get("total");
            if (t != null && t.isJsonPrimitive() && t.getAsJsonPrimitive().isNumber()) {
                total = t.getAsInt();
            }
        }
        return new Page(entries, total);
    }

    public static CompletableFuture<List<String>> fetchVersions() {
        String url = API_BASE + "/versions";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "LitematicaOnlineHub/1.0")
                .GET().build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) return List.of();
                    try {
                        JsonElement root = JsonParser.parseString(response.body());
                        JsonArray array = root.isJsonArray() ? root.getAsJsonArray()
                                : root.getAsJsonObject().getAsJsonArray("data");
                        List<String> list = new ArrayList<>();
                        if (array != null) {
                            for (JsonElement el : array) {
                                if (el.isJsonPrimitive()) list.add(el.getAsString());
                            }
                        }
                        return list;
                    } catch (Exception e) {
                        return List.of();
                    }
                });
    }

    private static List<SchematicEntry> parseEntriesFrom(JsonArray array) {
        List<SchematicEntry> result = new ArrayList<>();
        if (array == null) return result;
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            SchematicEntry entry = new SchematicEntry();
            entry.id = value(object, "id", "schematic");
            entry.title = value(object, "title", value(object, "name", entry.id));
            entry.author = value(object, "author", "Unknown");
            entry.tag = value(object, "tag", "");
            entry.version = value(object, "version", "");
            entry.description = value(object, "description", "");
            entry.downloads = intValue(object, "downloads");
            entry.likes = intValue(object, "likes");
            entry.downloadUrl = value(object, "download_url", value(object, "downloadUrl", ""));
            entry.thumbnailUrl = value(object, "thumbnail_url", value(object, "thumbnailUrl", ""));
            // every cover of this publication, so the gallery can flip between them
            JsonElement shots = object.get("screenshots");
            if (shots != null && shots.isJsonArray()) {
                for (JsonElement shot : shots.getAsJsonArray()) {
                    if (shot.isJsonPrimitive()) entry.covers.add(shot.getAsString());
                }
            }
            if (entry.covers.isEmpty() && !entry.thumbnailUrl.isBlank()) {
                entry.covers.add(entry.thumbnailUrl);
            }
            if (!entry.downloadUrl.isBlank()) result.add(entry);
        }
        return result;
    }

    private static String value(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        return value != null && !value.isJsonNull() ? value.getAsString() : fallback;
    }

    private static int intValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return value.getAsInt();
        }
        return 0;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    // ---- cover loading (PNG) + offline cache ---------------------------------

    /**
     * Absolute URL of a PNG version of the given cover.
     * Stored images on the site are AVIF/WebP, which Minecraft cannot decode,
     * so we always go through the server-side PNG conversion endpoint.
     */
    private static String pngProxyUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return "";
        String url = rawUrl.trim();
        String path = url;
        if (url.startsWith("http")) {
            try {
                path = URI.create(url).getPath();
            } catch (Exception ignored) {
                path = url;
            }
        }
        return SITE_ORIGIN + "/api/v1/image.png?src=" + encode(path);
    }

    /** Returns the cover texture at the given index, or null when not ready yet. */
    public static Identifier getCover(SchematicEntry entry, int coverIndex) {
        if (entry == null || entry.covers.isEmpty()) return null;
        if (coverIndex < 0 || coverIndex >= entry.covers.size()) return null;
        String url = pngProxyUrl(entry.covers.get(coverIndex));
        return url.isEmpty() ? null : THUMBNAILS.get(url);
    }

    public static int coverCount(SchematicEntry entry) {
        return entry == null ? 0 : entry.covers.size();
    }

    /** Starts loading the first cover of every entry (deduplicated, failures remembered). */
    public static void requestThumbnails(List<SchematicEntry> entries) {
        for (SchematicEntry entry : entries) {
            if (entry.covers.isEmpty()) continue;
            requestCoverUrl(pngProxyUrl(entry.covers.get(0)));
        }
    }

    /** Starts loading a specific cover (called when the user flips to the next image). */
    public static void requestCover(SchematicEntry entry, int coverIndex) {
        if (entry == null || coverIndex < 0 || coverIndex >= entry.covers.size()) return;
        requestCoverUrl(pngProxyUrl(entry.covers.get(coverIndex)));
    }

    private static void requestCoverUrl(String url) {
        if (url.isEmpty()) return;
        if (THUMBNAILS.containsKey(url) || THUMBNAIL_FAILED.contains(url)) return;
        if (!THUMBNAIL_LOADING.add(url)) return;
        loadThumbnail(url);
    }

    private static void loadThumbnail(String url) {
        CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(REQUEST_TIMEOUT)
                        .header("User-Agent", "LitematicaOnlineHub/1.0")
                        .GET().build();
                HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() / 100 != 2) {
                    throw new IllegalStateException("cover returned HTTP " + response.statusCode());
                }
                byte[] bytes = response.body();
                if (bytes == null || bytes.length == 0) {
                    throw new IllegalStateException("cover was empty");
                }
                return NativeImage.read(bytes);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).whenComplete((image, error) -> {
            if (error != null || image == null) {
                THUMBNAIL_FAILED.add(url);
                THUMBNAIL_LOADING.remove(url);
                ModInit.LOGGER.warn("Could not load cover {}: {}", url, describeError(error));
                return;
            }
            // GPU texture creation + registration must happen on the render thread.
            Minecraft.getInstance().execute(() -> {
                try {
                    DynamicTexture texture = new DynamicTexture(() -> "litematicahub_cover", image);
                    Identifier id = Identifier.fromNamespaceAndPath(
                            ModInit.MOD_ID, "cover_" + Integer.toHexString(url.hashCode()));
                    Minecraft.getInstance().getTextureManager().register(id, texture);
                    THUMBNAILS.put(url, id);
                } catch (Exception exception) {
                    THUMBNAIL_FAILED.add(url);
                    ModInit.LOGGER.warn("Could not register cover {}", url, exception);
                } finally {
                    THUMBNAIL_LOADING.remove(url);
                }
            });
        });
    }

    public static void downloadSchematic(SchematicEntry entry, DownloadCallback callback) {
        downloadSchematic(entry, callback, null);
    }

    public static void downloadSchematic(SchematicEntry entry, DownloadCallback callback,
                                         ProgressCallback progress) {
        CompletableFuture.supplyAsync(() -> {
            Path temporary = null;
            try {
                Path directory = getSchematicsDirectory();
                Files.createDirectories(directory);

                HttpRequest request = HttpRequest.newBuilder(URI.create(entry.downloadUrl))
                        .timeout(REQUEST_TIMEOUT)
                        .header("User-Agent", "LitematicaOnlineHub/1.0")
                        .GET().build();
                HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() / 100 != 2) {
                    throw new IllegalStateException("server returned HTTP " + response.statusCode());
                }

                long declared = response.headers().firstValueAsLong("content-length").orElse(-1L);
                report(progress, 0L, declared);

                temporary = Files.createTempFile(directory, ".hub-", ".download");
                try (InputStream stream = response.body()) {
                    // Count bytes as they arrive so the screen can show a moving bar
                    // instead of freezing for several seconds on a big file.
                    java.io.BufferedInputStream buffered = new java.io.BufferedInputStream(stream, 64 * 1024);
                    byte[] chunk = new byte[64 * 1024];
                    long total = 0L;
                    int read;
                    try (java.io.OutputStream out = Files.newOutputStream(temporary)) {
                        while ((read = buffered.read(chunk)) != -1) {
                            out.write(chunk, 0, read);
                            total += read;
                            report(progress, total, declared);
                        }
                    }
                }
                if (Files.size(temporary) == 0) {
                    throw new IllegalStateException("server sent an empty file");
                }

                DownloadResult result = placeFile(directory, temporary, fileNameBase(entry));
                temporary = null;
                return result;
            } catch (Exception exception) {
                ModInit.LOGGER.warn("Failed to download schematic {}", entry.title, exception);
                return new DownloadResult(false, "download failed: " + describeError(exception));
            } finally {
                if (temporary != null) {
                    try {
                        Files.deleteIfExists(temporary);
                    } catch (Exception ignored) {
                    }
                }
            }
        }).whenComplete((result, error) -> {
            DownloadResult finalResult = error != null
                    ? new DownloadResult(false, "download failed: " + describeError(error))
                    : result;
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (finalResult.ok() && client.player != null) {
                    client.player.sendSystemMessage(Component.literal("[Litematica Hub] " + finalResult.message()));
                }
                callback.onResult(finalResult.ok(), finalResult.message());
            });
        });
    }

    private static void report(ProgressCallback progress, long read, long total) {
        if (progress == null) return;
        try {
            progress.onProgress(read, total);
        } catch (Exception ignored) {
            // never let progress bookkeeping break a download
        }
    }

    private static DownloadResult placeFile(Path directory, Path temporary, String base) throws Exception {
        for (int copy = 1; copy <= 99; copy++) {
            String name = copy == 1 ? base + ".schem" : base + " (" + copy + ").schem";
            Path target = directory.resolve(name);
            if (!Files.exists(target)) {
                move(temporary, target);
                return new DownloadResult(true, "saved to schematics/" + name);
            }
            if (Files.mismatch(target, temporary) == -1L) {
                Files.delete(temporary);
                return new DownloadResult(true, "already in schematics: " + name);
            }
        }
        throw new IllegalStateException("too many copies of \"" + base + "\" already exist");
    }

    private static void move(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

        // ---- offline cache + update check ---------------------------------------

    private static final String MOD_VERSION = "1.0.0";
    private static final String CURRENT_VERSION = "1.1.0";

    /** Version of this mod build — shown in the update notification. */
    public static String currentVersion() {
        return CURRENT_VERSION;
    }

    private static Path cacheFile() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("litematicahub-gallery-cache.json");
    }

    /** Reads the last gallery snapshot that was saved while the site was reachable. */
    public static List<SchematicEntry> loadCachedPage() {
        try {
            Path file = cacheFile();
            if (!Files.exists(file)) return List.of();
            String body = Files.readString(file, StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(body);
            JsonArray array = root.isJsonArray() ? root.getAsJsonArray()
                    : root.getAsJsonObject().getAsJsonArray("data");
            return parseEntriesFrom(array);
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Stores the current page so the gallery still opens without a connection. */
    public static void saveCachedPage(List<SchematicEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        try {
            Path file = cacheFile();
            Files.createDirectories(file.getParent());
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            for (SchematicEntry entry : entries) {
                com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                o.addProperty("id", entry.id);
                o.addProperty("title", entry.title);
                o.addProperty("author", entry.author);
                o.addProperty("tag", entry.tag);
                o.addProperty("version", entry.version);
                o.addProperty("description", entry.description);
                o.addProperty("downloads", entry.downloads);
                o.addProperty("likes", entry.likes);
                o.addProperty("download_url", entry.downloadUrl);
                o.addProperty("thumbnail_url", entry.thumbnailUrl);
                com.google.gson.JsonArray shots = new com.google.gson.JsonArray();
                for (String c : entry.covers) shots.add(c);
                o.add("screenshots", shots);
                array.add(o);
            }
            Files.writeString(cacheFile(), array.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            ModInit.LOGGER.warn("Could not save gallery cache", e);
        }
    }

    /**
     * Asks the site for the newest published mod version.
     * Returns an empty string when the user already runs the latest one.
     */
    public static CompletableFuture<String> fetchLatestVersion() {
        String url = SITE_ORIGIN + "/api/v1/mod/version";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "LitematicaOnlineHub/" + CURRENT_VERSION)
                .GET().build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) return "";
                    try {
                        JsonElement root = JsonParser.parseString(response.body());
                        String latest = root.isJsonObject()
                                ? value(root.getAsJsonObject(), "version", "")
                                : root.getAsString();
                        return newerThan(CURRENT_VERSION, latest) ? latest : "";
                    } catch (Exception e) {
                        return "";
                    }
                })
                .exceptionally(e -> "");
    }

    /** true when {@code candidate} is a strictly newer version than {@code current} */
    private static boolean newerThan(String current, String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        String[] a = current.split("[.-]");
        String[] b = candidate.split("[.-]");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? parseIntSafe(a[i]) : 0;
            int y = i < b.length ? parseIntSafe(b[i]) : 0;
            if (y != x) return y > x;
        }
        return false;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static String fileNameBase(SchematicEntry entry) {
        String base = sanitizeFileName(entry.title);
        if (base.isEmpty()) base = sanitizeFileName(entry.id);
        if (base.isEmpty()) base = "schematic_" + System.currentTimeMillis();
        if (base.length() > 60) base = base.substring(0, 60).trim();
        return base;
    }

    private static String sanitizeFileName(String value) {
        if (value == null) return "";
        return value.replaceAll("[^\\p{L}\\p{N} _.-]", "").trim().replaceAll("[ .]+$", "");
    }

    private static String describeError(Throwable error) {
        if (error == null) return "unknown error";
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        String message = cause.getMessage();
        if (message == null || message.isBlank()) message = cause.getClass().getSimpleName();
        if (message.length() > 120) message = message.substring(0, 120);
        return message;
    }
}

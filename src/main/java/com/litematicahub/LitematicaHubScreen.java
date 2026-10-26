package com.litematicahub;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern in-game browser for Litematica Online Hub.
 * Styled to perfectly match the sleek dark-emerald web interface of litematica-online-hub.shop.
 */
public final class LitematicaHubScreen extends Screen {

    // =========================================================================
    // COLOR PALETTE (Matches the litematica-online-hub modern web design)
    // =========================================================================
    private static final int C_PAGE_BG        = 0xF8070B14; // Deep dark navy backdrop (#070b14)
    private static final int C_HEADER_BG      = 0xFA0B101D; // Top bar glass background (#0b101d)
    private static final int C_HEADER_BORDER  = 0xFF192237; // Top bar bottom border
    private static final int C_BOTTOM_BG      = 0xFA0B101D; // Bottom bar background
    private static final int C_BOTTOM_BORDER  = 0xFF192237; // Bottom bar top border

    // Cards
    private static final int C_CARD           = 0xF40F172A; // Slate dark card (#0f172a)
    private static final int C_CARD_HOVER     = 0xF8142036; // Hovered card (#142036)
    private static final int C_CARD_BORDER    = 0xFF1E293B; // 1px card border (#1e293b)
    private static final int C_CARD_BORDER_H  = 0xFF334155; // Hovered border (#334155)
    private static final int C_COVER_BG       = 0xFF090E1A; // Thumbnail frame background
    private static final int C_COVER_BORDER   = 0xFF1E293B;

    // Controls & Inputs
    private static final int C_FIELD          = 0xF0090E1A; // Input background
    private static final int C_FIELD_HOVER    = 0xF40E1626;
    private static final int C_FIELD_BORDER   = 0xFF1E293B;
    private static final int C_FIELD_FOCUS    = 0xFF10B981; // Emerald focus outline

    // Brand Accents
    private static final int C_EMERALD        = 0xFF10B981; // Primary emerald (#10b981)
    private static final int C_EMERALD_LIGHT  = 0xFF34D399; // Emerald glow / hover (#34d399)
    private static final int C_EMERALD_DARK   = 0xFF059669; // Emerald deep gradient (#059669)
    private static final int C_EMERALD_TINT   = 0x2810B981; // Emerald badge background
    private static final int C_EMERALD_BORDER = 0x6010B981; // Emerald subtle border
    private static final int C_SELECTED       = 0xFF0F3327; // Deep emerald selected item

    // Badges & Pills
    private static final int C_PILL_TAG_BG    = 0x2810B981;
    private static final int C_PILL_TAG_TXT   = 0xFF6EE7B7;
    private static final int C_PILL_VER_BG    = 0xFF141F33;
    private static final int C_PILL_VER_TXT   = 0xFF94A3B8;

    // Typography
    private static final int C_WHITE          = 0xFFFFFFFF;
    private static final int C_TEXT           = 0xFFF1F5F9; // Slate 100
    private static final int C_TEXT_DIM       = 0xFFCBD5E1; // Slate 300
    private static final int C_MUTED          = 0xFF94A3B8; // Slate 400
    private static final int C_DARK           = 0xFF64748B; // Slate 500
    private static final int C_ERROR          = 0xFFF43F5E; // Rose 500
    private static final int C_HEART          = 0xFFFB7185; // Rose 400
    private static final int C_DOWNLOAD_SKY   = 0xFF38BDF8; // Sky 400
    private static final int C_AMBER          = 0xFFF59E0B; // Amber 500
    private static final int C_HOVER          = 0xFF1E293B;

    // =========================================================================
    // LAYOUT CONSTANTS
    // =========================================================================
    private static final int CARD_H        = 82;
    private static final int CARD_STEP     = CARD_H + 8; // 90px
    private static final int COVER_W       = 96;
    private static final int COVER_H       = 64;
    private static final int DESC_LINES    = 2;
    private static final int BTN_W         = 92;
    private static final int BTN_H         = 22;

    private static final int Y_HEADER      = 0;
    private static final int HEADER_H      = 34;

    private static final int Y_TOOLBAR     = 42;
    private static final int TOOLBAR_H     = 22;

    private static final int Y_LIST        = 72;
    private static final int BOTTOM_BAR_H  = 30;

    private static final int PAGE_SIZE     = 15;

    // URLs
    private static final String SITE_URL = "https://litematica-online-hub.shop/";
    private static final String MOD_DOWNLOAD_URL = "https://litematica-online-hub.shop/";

    // =========================================================================
    // STATE
    // =========================================================================
    private final Screen parent;
    private EditBox search;
    private EditBox tagSearch;

    private String tag = "All";
    private String version = "Any";
    private final List<String> availableVersions = new ArrayList<>();

    private int page;
    private int total;
    private boolean offline;
    private String updateVersion = "";

    // update toast state
    private boolean updateDismissed;
    private long updateShownAt;
    private long updateCloseAt;
    private int toastCloseX, toastCloseY, toastDlX, toastDlY, toastDlW, toastDlH;

    /** 0 = closed, 1 = tags, 2 = versions */
    private int openDropdown;
    private int dropdownScroll;
    private int panelX, panelY, panelW, panelH;
    private int panelRowsCache = 9;

    private List<SchematicDownloadManager.SchematicEntry> entries = List.of();
    private boolean loading;
    private boolean downloading;
    private int scroll;
    private String status = "Loading gallery...";
    private int statusColor = C_MUTED;

    // Live download progress
    private volatile String downloadTitle = null;
    private volatile long downloadRead = 0L;
    private volatile long downloadTotal = -1L;

    /** Flip through screenshot covers */
    private final java.util.Map<String, Integer> coverIndices = new java.util.HashMap<>();

    // Click hitboxes
    private int searchFieldX, searchFieldY, searchFieldW, searchFieldH;
    private int tagButtonX, tagButtonY, tagButtonW, tagButtonH;
    private int verButtonX, verButtonY, verButtonW, verButtonH;
    private int relButtonX, relButtonY, relButtonW, relButtonH;
    private int webHeaderX, webHeaderY, webHeaderW, webHeaderH;
    private int closeHeaderX, closeHeaderY, closeHeaderW, closeHeaderH;
    private int pagerPrevX, pagerPrevY, pagerPrevW, pagerPrevH;
    private int pagerNextX, pagerNextY, pagerNextW, pagerNextH;
    private int clearSearchX, clearSearchY, clearSearchW, clearSearchH;

    private static Identifier logoAvatarId;
    private static boolean logoAvatarAttempted;

    private static Identifier getLogoAvatar() {
        if (logoAvatarAttempted) return logoAvatarId;
        logoAvatarAttempted = true;
        String[] candidates = {
            "/assets/litematicahub/avatar.png",
            "/assets/litematicahub/icon.png",
            "/assets/litematicahub/textures/gui/avatar.png",
            "/assets/litematicahub/textures/gui/icon.png"
        };
        for (String path : candidates) {
            try (InputStream in = LitematicaHubScreen.class.getResourceAsStream(path)) {
                if (in != null) {
                    byte[] bytes = in.readAllBytes();
                    NativeImage image = NativeImage.read(bytes);
                    DynamicTexture texture = new DynamicTexture(() -> "litematicahub_logo_avatar", image);
                    logoAvatarId = Identifier.fromNamespaceAndPath("litematicahub", "gui_avatar");
                    Minecraft.getInstance().getTextureManager().register(logoAvatarId, texture);
                    return logoAvatarId;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private final List<ItemBox> downloadButtons = new ArrayList<>();
    private final List<ItemBox> dropdownItems = new ArrayList<>();

    private static class ItemBox {
        int x, y, width, height;
        String value;
        SchematicDownloadManager.SchematicEntry entry;
    }

    public LitematicaHubScreen(Screen parent) {
        super(Component.literal("Litematica Online Hub"));
        this.parent = parent;
        SchematicDownloadManager.refreshTags();
    }

    private int getContentWidth() {
        return Math.min(540, Math.max(460, width - 40));
    }

    @Override
    protected void init() {
        int contentW = getContentWidth();
        int cx = width / 2 - contentW / 2;

        // Toolbar math
        int searchW = contentW - 240;
        searchFieldX = cx;
        searchFieldY = Y_TOOLBAR;
        searchFieldW = searchW;
        searchFieldH = TOOLBAR_H;

        // Search text box
        search = addRenderableWidget(new EditBox(font, cx + 22, Y_TOOLBAR + 5, searchW - 38, 14, Component.literal("")));
        search.setMaxLength(128);
        search.setBordered(false);
        search.setTextColor(C_TEXT);
        search.setTextColorUneditable(C_DARK);

        clearSearchX = cx + searchW - 16;
        clearSearchY = Y_TOOLBAR + 4;
        clearSearchW = 14;
        clearSearchH = 14;

        tagButtonX = cx + searchW + 6;
        tagButtonY = Y_TOOLBAR;
        tagButtonW = 108;
        tagButtonH = TOOLBAR_H;

        verButtonX = tagButtonX + tagButtonW + 6;
        verButtonY = Y_TOOLBAR;
        verButtonW = 86;
        verButtonH = TOOLBAR_H;

        relButtonX = verButtonX + verButtonW + 6;
        relButtonY = Y_TOOLBAR;
        relButtonW = 28;
        relButtonH = TOOLBAR_H;

        // Header buttons
        closeHeaderW = 56;
        closeHeaderH = 20;
        closeHeaderX = width - closeHeaderW - 12;
        closeHeaderY = 7;

        webHeaderW = 76;
        webHeaderH = 20;
        webHeaderX = closeHeaderX - webHeaderW - 8;
        webHeaderY = 7;

        // In-dropdown search field for tags
        tagSearch = addRenderableWidget(new EditBox(font, 0, 0, 100, 14, Component.literal("")));
        tagSearch.setMaxLength(32);
        tagSearch.setBordered(false);
        tagSearch.setTextColor(C_TEXT);
        tagSearch.setTextColorUneditable(C_DARK);
        tagSearch.visible = false;
        tagSearch.setResponder(v -> dropdownScroll = 0);

        SchematicDownloadManager.fetchVersions().whenComplete((res, err) -> {
            if (err == null && res != null) {
                Minecraft.getInstance().execute(() -> {
                    availableVersions.clear();
                    for (String v : res) {
                        String cv = SchematicDownloadManager.cleanVersion(v);
                        if (!cv.isEmpty() && !availableVersions.contains(cv)) {
                            availableVersions.add(cv);
                        }
                    }
                    if (!availableVersions.contains("26.3")) {
                        availableVersions.add(0, "26.3");
                    }
                });
            }
        });

        SchematicDownloadManager.fetchLatestVersion().whenComplete((latest, err) -> {
            if (err == null && latest != null && !latest.isBlank()) {
                Minecraft.getInstance().execute(() -> {
                    if (updateVersion.isBlank()) updateShownAt = System.currentTimeMillis();
                    updateVersion = latest;
                });
            }
        });

        loadEntries();
    }

    private void loadEntries() {
        if (loading || search == null) return;
        loading = true;
        status = "Loading gallery...";
        statusColor = C_MUTED;
        String queryTag = "All".equalsIgnoreCase(tag) ? "" : tag;
        String queryVer = "Any".equalsIgnoreCase(version) ? "" : SchematicDownloadManager.cleanVersion(version);
        SchematicDownloadManager.fetchSchematics(search.getValue(), queryTag, queryVer, PAGE_SIZE, page * PAGE_SIZE)
                .whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                    loading = false;
                    if (error != null) {
                        List<SchematicDownloadManager.SchematicEntry> cached = SchematicDownloadManager.loadCachedPage();
                        offline = true;
                        if (!cached.isEmpty()) {
                            entries = cached;
                            total = cached.size();
                            page = 0;
                            SchematicDownloadManager.requestThumbnails(entries);
                            status = "Offline - showing cached (" + cached.size() + ")";
                            statusColor = C_AMBER;
                        } else {
                            entries = List.of();
                            status = "Failed to load: " + describeError(error);
                            statusColor = C_ERROR;
                        }
                    } else {
                        offline = false;
                        entries = result == null ? List.of() : result.entries;
                        total = result == null ? 0 : result.total;
                        int pages = totalPages();
                        if (page >= pages) {
                            page = Math.max(0, pages - 1);
                            loadEntries();
                            return;
                        }
                        scroll = 0;
                        SchematicDownloadManager.requestThumbnails(entries);
                        SchematicDownloadManager.saveCachedPage(entries);
                        if (entries.isEmpty()) {
                            status = "No schematics found";
                            statusColor = C_MUTED;
                        } else {
                            int from = page * PAGE_SIZE + 1;
                            int to = from + entries.size() - 1;
                            status = "Showing " + from + "-" + to + " of " + total;
                            statusColor = C_MUTED;
                        }
                    }
                }));
    }

    private int totalPages() {
        return Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private int coverIndex(SchematicDownloadManager.SchematicEntry entry) {
        Integer idx = coverIndices.get(entry.id);
        return idx == null ? 0 : idx;
    }

    private void flipCover(SchematicDownloadManager.SchematicEntry entry, int delta) {
        int count = SchematicDownloadManager.coverCount(entry);
        if (count <= 1) return;
        int next = Math.floorMod(coverIndex(entry) + delta, count);
        coverIndices.put(entry.id, next);
        SchematicDownloadManager.requestCover(entry, next);
    }

    private void changePage(int delta) {
        int next = page + delta;
        if (next < 0 || next >= totalPages()) return;
        page = next;
        loadEntries();
    }

    private void reloadFromFirstPage() {
        page = 0;
        loadEntries();
    }

    private String describeError(Throwable error) {
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        String message = cause.getMessage();
        if (message == null || message.isBlank()) message = cause.getClass().getSimpleName();
        if (message.length() > 80) message = message.substring(0, 80);
        return message;
    }

    private String cleanText(String s) {
        if (s == null) return "";
        return s.replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .replaceAll("[\\p{Cntrl}]", " ")
                .replaceAll(" {2,}", " ")
                .trim();
    }

    private String trim(String text, int maxWidth) {
        String clean = cleanText(text);
        if (clean.isEmpty()) return "";
        if (font.width(clean) <= maxWidth) return clean;
        String cut = clean;
        while (!cut.isEmpty() && font.width(cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    private List<String> wrapLines(String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String rest = cleanText(text);
        if (rest.isEmpty()) return lines;

        while (!rest.isEmpty() && lines.size() < maxLines) {
            if (font.width(rest) <= maxWidth) {
                lines.add(rest);
                rest = "";
                break;
            }
            String fit = font.plainSubstrByWidth(rest, maxWidth);
            if (fit.isEmpty()) break;
            int sp = fit.lastIndexOf(' ');
            String line;
            if (sp > 0) {
                line = rest.substring(0, sp);
                rest = rest.substring(sp + 1);
            } else {
                line = fit;
                rest = rest.substring(fit.length());
            }
            line = line.trim();
            if (line.isEmpty()) break;
            lines.add(line);
            rest = rest.trim();
        }
        if (!rest.isEmpty() && lines.size() == maxLines) {
            String last = lines.get(maxLines - 1);
            String withDots = last + "...";
            while (!last.isEmpty() && font.width(withDots) > maxWidth) {
                last = last.substring(0, last.length() - 1);
                withDots = last + "...";
            }
            lines.set(maxLines - 1, withDots);
        }
        return lines;
    }

    private void drawCover(Graphics g, Identifier cover, int x0, int y0, int w, int h) {
        int[] dims = SchematicDownloadManager.getCoverDimensions(cover);
        float u0 = 0f, u1 = 1f, v0 = 0f, v1 = 1f;
        if (dims != null && dims[0] > 0 && dims[1] > 0) {
            float imgW = dims[0];
            float imgH = dims[1];
            float imgAspect = imgW / imgH;
            float targetAspect = (float) w / (float) h;
            if (imgAspect > targetAspect) {
                // Image is wider than container: crop left and right (object-fit: cover)
                float visibleRatio = targetAspect / imgAspect;
                float crop = (1f - visibleRatio) * 0.5f;
                u0 = crop;
                u1 = 1f - crop;
            } else if (imgAspect < targetAspect) {
                // Image is taller than container: crop top and bottom
                float visibleRatio = imgAspect / targetAspect;
                float crop = (1f - visibleRatio) * 0.5f;
                v0 = crop;
                v1 = 1f - crop;
            }
        }
        g.blit(cover, x0, y0, x0 + w, y0 + h, u0, u1, v0, v1);
    }

    private static String formatCount(int n) {
        if (n >= 1000000) return String.format("%.1fM", n / 1000000.0);
        if (n >= 1000) return String.format("%.1fk", n / 1000.0);
        return String.valueOf(n);
    }

    // =========================================================================
    // GRAPHICS ENGINE & VECTOR ICONS
    // =========================================================================
    private interface Graphics {
        void fill(int x1, int y1, int x2, int y2, int color);
        void fillGradient(int x1, int y1, int x2, int y2, int colorTop, int colorBottom);
        void text(net.minecraft.client.gui.Font f, String s, int x, int y, int color, boolean shadow);
        void blit(Identifier id, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1);
    }

    private void text(Graphics g, String s, int x, int y, int color) {
        g.text(font, s, x, y, color, false);
    }

    private void textCentered(Graphics g, String s, int cx, int y, int color) {
        g.text(font, s, cx - font.width(s) / 2, y, color, false);
    }

    private void frame(Graphics g, int x, int y, int w, int h, int fill, int border) {
        if (fill != 0) g.fill(x, y, x + w, y + h, fill);
        if (border != 0) {
            g.fill(x, y, x + w, y + 1, border);
            g.fill(x, y + h - 1, x + w, y + h, border);
            g.fill(x, y + 1, x + 1, y + h - 1, border);
            g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
        }
    }

    private void fillRounded(Graphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w / 2, h / 2));
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + r, y + h - r, color);
        g.fill(x + w - r, y + r, x + w, y + h - r, color);
        if (r >= 2) {
            g.fill(x + 1, y + 1, x + r, y + r, color);
            g.fill(x + w - r, y + 1, x + w - 1, y + r, color);
            g.fill(x + 1, y + h - r, x + r, y + h - 1, color);
            g.fill(x + w - r, y + h - r, x + w - 1, y + h - 1, color);
        }
    }

    private void frameRounded(Graphics g, int x, int y, int w, int h, int r, int fill, int border) {
        if (fill != 0) fillRounded(g, x, y, w, h, r, fill);
        if (border != 0) {
            g.fill(x + r, y, x + w - r, y + 1, border);
            g.fill(x + r, y + h - 1, x + w - r, y + h, border);
            g.fill(x, y + r, x + 1, y + h - r, border);
            g.fill(x + w - 1, y + r, x + w, y + h - r, border);
            if (r >= 2) {
                g.fill(x + 1, y + 1, x + r, y + 2, border);
                g.fill(x + w - r, y + 1, x + w - 1, y + 2, border);
                g.fill(x + 1, y + h - 2, x + r, y + h - 1, border);
                g.fill(x + w - r, y + h - 2, x + w - 1, y + h - 1, border);
            }
        }
    }

    private void drawPill(Graphics g, int x, int y, int w, int h, int fill, int border, String text, int textColor) {
        frameRounded(g, x, y, w, h, 3, fill, border);
        int tx = x + (w - font.width(text)) / 2;
        int ty = y + (h - 8) / 2 + 1;
        text(g, text, tx, ty, textColor);
    }

    private enum IconType { HEART, DOWNLOAD, SEARCH, CHECK, CHEVRON_DOWN, REFRESH, EXTERNAL, CLOSE }

    private void drawIcon(Graphics g, IconType type, int x, int y, int color) {
        switch (type) {
            case HEART -> {
                g.fill(x + 1, y, x + 3, y + 1, color);
                g.fill(x + 4, y, x + 6, y + 1, color);
                g.fill(x, y + 1, x + 7, y + 3, color);
                g.fill(x + 1, y + 3, x + 6, y + 4, color);
                g.fill(x + 2, y + 4, x + 5, y + 5, color);
                g.fill(x + 3, y + 5, x + 4, y + 6, color);
            }
            case DOWNLOAD -> {
                g.fill(x + 3, y, x + 4, y + 5, color);
                g.fill(x + 2, y + 3, x + 5, y + 4, color);
                g.fill(x + 1, y + 4, x + 6, y + 5, color);
                g.fill(x, y + 6, x + 7, y + 7, color);
            }
            case SEARCH -> {
                g.fill(x + 1, y, x + 5, y + 1, color);
                g.fill(x, y + 1, x + 1, y + 5, color);
                g.fill(x + 5, y + 1, x + 6, y + 5, color);
                g.fill(x + 1, y + 4, x + 5, y + 5, color);
                g.fill(x + 4, y + 4, x + 7, y + 7, color);
            }
            case CHECK -> {
                g.fill(x, y + 2, x + 1, y + 4, color);
                g.fill(x + 1, y + 3, x + 2, y + 5, color);
                g.fill(x + 2, y + 2, x + 3, y + 4, color);
                g.fill(x + 3, y + 1, x + 4, y + 3, color);
                g.fill(x + 4, y, x + 5, y + 2, color);
            }
            case CHEVRON_DOWN -> {
                g.fill(x, y, x + 5, y + 1, color);
                g.fill(x + 1, y + 1, x + 4, y + 2, color);
                g.fill(x + 2, y + 2, x + 3, y + 3, color);
            }
            case REFRESH -> {
                g.fill(x + 1, y, x + 6, y + 1, color);
                g.fill(x, y + 1, x + 1, y + 6, color);
                g.fill(x + 1, y + 6, x + 6, y + 7, color);
                g.fill(x + 6, y + 3, x + 7, y + 6, color);
                g.fill(x + 4, y + 1, x + 7, y + 2, color);
            }
            case EXTERNAL -> {
                g.fill(x + 3, y, x + 6, y + 1, color);
                g.fill(x + 5, y + 1, x + 6, y + 4, color);
                g.fill(x + 2, y + 3, x + 5, y + 4, color);
                g.fill(x, y + 2, x + 1, y + 6, color);
                g.fill(x, y + 5, x + 4, y + 6, color);
            }
            case CLOSE -> {
                for (int i = 0; i < 5; i++) {
                    g.fill(x + i, y + i, x + i + 1, y + i + 1, color);
                    g.fill(x + 4 - i, y + i, x + 5 - i, y + i + 1, color);
                }
            }
        }
    }

    private void drawCubeBadge(Graphics g, int x, int y, int size) {
        g.fillGradient(x, y, x + size, y + size, 0xFF10B981, 0xFF059669);
        frameRounded(g, x, y, size, size, 2, 0x00000000, 0x40FFFFFF);
        int cx = x + size / 2;
        int cy = y + size / 2;
        g.fill(cx - 3, cy - 3, cx + 4, cy - 2, 0xFFE6FFFA);
        g.fill(cx - 5, cy - 1, cx + 6, cy, 0xFFCCFBF1);
        g.fill(cx - 5, cy + 1, cx, cy + 6, 0xFF5EEAD4);
        g.fill(cx, cy + 1, cx + 5, cy + 6, 0xFF2DD4BF);
        g.fill(cx, cy - 1, cx + 1, cy + 6, 0xFF0F766E);
    }

    private void drawBlueprintPlaceholder(Graphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF090E1A);
        for (int gy = y + 8; gy < y + h; gy += 10) {
            g.fill(x, gy, x + w, gy + 1, 0xFF111A2E);
        }
        for (int gx = x + 8; gx < x + w; gx += 10) {
            g.fill(gx, y, gx + 1, y + h, 0xFF111A2E);
        }
        drawCubeBadge(g, x + w / 2 - 8, y + h / 2 - 12, 16);
        textCentered(g, "No cover", x + w / 2, y + h / 2 + 8, C_DARK);
    }

    // =========================================================================
    // RENDERING
    // =========================================================================
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        final Graphics g = new Graphics() {
            public void fill(int x1, int y1, int x2, int y2, int color) {
                graphics.fill(x1, y1, x2, y2, color);
            }
            public void fillGradient(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
                graphics.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
            }
            public void text(net.minecraft.client.gui.Font f, String s, int x, int y, int color, boolean shadow) {
                graphics.drawString(f, s, x, y, color, shadow);
            }
            public void blit(Identifier id, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1) {
                graphics.blit(id, x0, y0, x1, y1, u0, u1, v0, v1);
            }
        };

        int contentW = getContentWidth();
        int cx = width / 2 - contentW / 2;

        // 1. Deep Page Background
        graphics.fill(0, 0, width, height, C_PAGE_BG);

        // 2. Cards Scissor Region
        int listTop = Y_LIST;
        int listBottom = height - BOTTOM_BAR_H - 4;
        downloadButtons.clear();
        dropdownItems.clear();

        graphics.enableScissor(0, listTop, width, listBottom);
        int y = listTop - scroll;
        for (SchematicDownloadManager.SchematicEntry entry : entries) {
            if (y + CARD_H >= listTop && y <= listBottom) {
                boolean cardHover = over(mouseX, mouseY, cx, y, contentW, CARD_H);
                int cardBg = cardHover ? C_CARD_HOVER : C_CARD;
                int cardBorder = cardHover ? C_CARD_BORDER_H : C_CARD_BORDER;
                frameRounded(g, cx, y, contentW, CARD_H, 4, cardBg, cardBorder);

                // Subtle left emerald hairline indicator
                if (cardHover) {
                    g.fill(cx + 1, y + 6, cx + 3, y + CARD_H - 6, C_EMERALD);
                }

                // Thumbnail Cover (Widescreen 96x64 with object-fit: cover)
                int covX = cx + 8;
                int covY = y + (CARD_H - COVER_H) / 2;
                frameRounded(g, covX, covY, COVER_W, COVER_H, 3, C_COVER_BG, C_COVER_BORDER);
                int coverIdx = coverIndex(entry);
                Identifier cover = SchematicDownloadManager.getCover(entry, coverIdx);
                if (cover != null) {
                    drawCover(g, cover, covX + 1, covY + 1, COVER_W - 2, COVER_H - 2);
                } else {
                    drawBlueprintPlaceholder(g, covX + 1, covY + 1, COVER_W - 2, COVER_H - 2);
                }

                int coverTotal = SchematicDownloadManager.coverCount(entry);
                if (coverTotal > 1) {
                    g.fill(covX + 2, covY + 2, covX + 14, covY + 14, 0x99000000);
                    textCentered(g, "<", covX + 8, covY + 4, C_WHITE);
                    g.fill(covX + COVER_W - 14, covY + 2, covX + COVER_W - 2, covY + 14, 0x99000000);
                    textCentered(g, ">", covX + COVER_W - 8, covY + 4, C_WHITE);
                    String counter = (coverIdx + 1) + "/" + coverTotal;
                    int cw = font.width(counter) + 6;
                    drawPill(g, covX + COVER_W - cw - 3, covY + COVER_H - 13, cw, 11, 0x99000000, 0x40FFFFFF, counter, C_WHITE);
                }

                // Text details
                int tx = cx + COVER_W + 16;
                int btnX = cx + contentW - BTN_W - 10;
                int maxTitleW = btnX - 10 - tx;

                // Title
                text(g, trim(entry.title, maxTitleW), tx, y + 8, C_WHITE);

                // Badges row
                int badgeX = tx;
                if (entry.tag != null && !entry.tag.isEmpty() && !entry.tag.equalsIgnoreCase("All")) {
                    int pw = font.width(entry.tag) + 10;
                    drawPill(g, badgeX, y + 21, pw, 12, C_PILL_TAG_BG, C_EMERALD_BORDER, entry.tag, C_PILL_TAG_TXT);
                    badgeX += pw + 5;
                }
                if (entry.version != null && !entry.version.isEmpty()) {
                    String vLabel = "MC " + SchematicDownloadManager.cleanVersion(entry.version);
                    int pw = font.width(vLabel) + 10;
                    drawPill(g, badgeX, y + 21, pw, 12, C_PILL_VER_BG, C_CARD_BORDER, vLabel, C_PILL_VER_TXT);
                    badgeX += pw + 6;
                }
                text(g, "by " + entry.author, badgeX, y + 23, C_MUTED);

                // Description
                List<String> desc = wrapLines(entry.description, btnX - 8 - tx, DESC_LINES);
                for (int i = 0; i < desc.size(); i++) {
                    text(g, desc.get(i), tx, y + 36 + i * 11, C_MUTED);
                }

                // Stats row
                int statY = y + 64;
                drawIcon(g, IconType.DOWNLOAD, tx, statY + 1, C_DOWNLOAD_SKY);
                String dlStr = formatCount(entry.downloads);
                text(g, dlStr, tx + 10, statY, C_DOWNLOAD_SKY);

                int likeX = tx + 12 + font.width(dlStr) + 10;
                drawIcon(g, IconType.HEART, likeX, statY + 1, C_HEART);
                String likeStr = formatCount(entry.likes);
                text(g, likeStr, likeX + 10, statY, C_HEART);

                int linkX = likeX + 14 + font.width(likeStr) + 14;
                boolean linkHover = over(mouseX, mouseY, linkX, statY - 1, font.width("view on site в†—"), 10);
                text(g, "view on site в†—", linkX, statY, linkHover ? C_EMERALD_LIGHT : C_DARK);

                // Action Button
                boolean isDownloaded = SchematicDownloadManager.isSchematicDownloaded(entry);
                int btnY = y + 29;
                boolean btnHover = over(mouseX, mouseY, btnX, btnY, BTN_W, BTN_H);

                if (downloading && downloadTitle != null && downloadTitle.equals(entry.title)) {
                    frameRounded(g, btnX, btnY, BTN_W, BTN_H, 4, 0xFF192438, C_CARD_BORDER);
                    textCentered(g, "Saving...", btnX + BTN_W / 2, btnY + 7, C_EMERALD_LIGHT);
                } else if (isDownloaded) {
                    int fill = btnHover ? 0xFF163528 : 0xFF101D2C;
                    int border = btnHover ? C_EMERALD : C_EMERALD_BORDER;
                    frameRounded(g, btnX, btnY, BTN_W, BTN_H, 4, fill, border);
                    if (btnHover) {
                        textCentered(g, "Redownload", btnX + BTN_W / 2, btnY + 7, C_EMERALD_LIGHT);
                    } else {
                        drawIcon(g, IconType.CHECK, btnX + 12, btnY + 8, C_EMERALD);
                        text(g, "In Folder", btnX + 24, btnY + 7, C_EMERALD_LIGHT);
                    }
                } else {
                    int fillTop = btnHover ? 0xFF10B981 : 0xFF0D9668;
                    int fillBottom = btnHover ? 0xFF059669 : 0xFF047857;
                    g.fillGradient(btnX, btnY, btnX + BTN_W, btnY + BTN_H, fillTop, fillBottom);
                    frameRounded(g, btnX, btnY, BTN_W, BTN_H, 4, 0x00000000, btnHover ? 0xFF34D399 : C_EMERALD_BORDER);
                    drawIcon(g, IconType.DOWNLOAD, btnX + 12, btnY + 8, C_WHITE);
                    text(g, "Download", btnX + 24, btnY + 7, C_WHITE);
                }

                ItemBox dlBox = new ItemBox();
                dlBox.x = btnX; dlBox.y = btnY; dlBox.width = BTN_W; dlBox.height = BTN_H;
                dlBox.entry = entry;
                downloadButtons.add(dlBox);

                ItemBox siteBox = new ItemBox();
                siteBox.x = linkX; siteBox.y = statY - 1;
                siteBox.width = font.width("view on site в†—"); siteBox.height = 11;
                siteBox.entry = entry;
                siteBox.value = "site";
                downloadButtons.add(siteBox);
            }
            y += CARD_STEP;
        }
        graphics.disableScissor();

        if (entries.isEmpty() && !loading) {
            textCentered(g, "No schematics found", width / 2, listTop + 40, C_MUTED);
        }
        if (loading) {
            textCentered(g, "Loading schematics...", width / 2, listTop + 40, C_EMERALD_LIGHT);
        }

        // 3. Top Header Bar
        g.fill(0, 0, width, HEADER_H, C_HEADER_BG);
        g.fill(0, HEADER_H - 1, width, HEADER_H, C_HEADER_BORDER);

        // Logo & Avatar in top-left corner
        // Logo & Avatar in top-left corner
        int avX = 14;
        int avY = 5;
        int avSize = 24;
        Identifier avId = getLogoAvatar();
        int titleTextX = avX + avSize + 8;
        int titleW = font.width("LITEMATICA HUB");
        boolean logoHover = over(mouseX, mouseY, avX, avY, avSize + 8 + titleW, avSize);

        if (avId != null) {
            g.blit(avId, avX, avY, avX + avSize, avY + avSize, 0f, 1f, 0f, 1f);
        } else {
            drawCubeBadge(g, avX, avY + 3, 18);
        }

        text(g, "LITEMATICA HUB", titleTextX, 13, logoHover ? C_EMERALD_LIGHT : C_WHITE);
        int gallX = titleTextX + titleW + 8;
        drawPill(g, gallX, 10, 52, 14, C_EMERALD_TINT, C_EMERALD_BORDER, "GALLERY", C_EMERALD_LIGHT);

        if (total > 0) {
            String totalText = total + " Schematics";
            int countX = gallX + 58;
            if (countX + font.width(totalText) + 16 < webHeaderX - 10) {
                drawPill(g, countX, 10, font.width(totalText) + 12, 14, 0xFF141D30, C_CARD_BORDER, totalText, C_MUTED);
            }
        }

        if (offline) {
            drawPill(g, width / 2 - 40, 10, 80, 14, 0x30F59E0B, 0x60F59E0B, "OFFLINE", C_AMBER);
        }

        // Header buttons: Website & Close
        boolean webHover = over(mouseX, mouseY, webHeaderX, webHeaderY, webHeaderW, webHeaderH);
        frameRounded(g, webHeaderX, webHeaderY, webHeaderW, webHeaderH, 3, webHover ? C_HOVER : C_FIELD, webHover ? C_EMERALD : C_CARD_BORDER);
        drawIcon(g, IconType.EXTERNAL, webHeaderX + webHeaderW - 14, webHeaderY + 7, webHover ? C_EMERALD_LIGHT : C_MUTED);
        text(g, "Website", webHeaderX + 8, webHeaderY + 6, webHover ? C_EMERALD_LIGHT : C_TEXT);

        boolean closeHover = over(mouseX, mouseY, closeHeaderX, closeHeaderY, closeHeaderW, closeHeaderH);
        frameRounded(g, closeHeaderX, closeHeaderY, closeHeaderW, closeHeaderH, 3, closeHover ? 0xFF3B1E28 : C_FIELD, closeHover ? C_ERROR : C_CARD_BORDER);
        textCentered(g, "Close", closeHeaderX + closeHeaderW / 2, closeHeaderY + 6, closeHover ? C_WHITE : C_TEXT_DIM);

        // 4. Toolbar: Search Bar & Filters
        boolean searchHover = over(mouseX, mouseY, searchFieldX, searchFieldY, searchFieldW, searchFieldH);
        boolean searchFocus = search != null && search.isFocused();
        int searchBorder = searchFocus ? C_FIELD_FOCUS : (searchHover ? C_CARD_BORDER_H : C_FIELD_BORDER);
        frameRounded(g, searchFieldX, searchFieldY, searchFieldW, searchFieldH, 3, searchHover ? C_FIELD_HOVER : C_FIELD, searchBorder);
        drawIcon(g, IconType.SEARCH, searchFieldX + 8, searchFieldY + 7, searchFocus ? C_EMERALD : C_MUTED);

        if (search != null && search.getValue().isEmpty()) {
            text(g, "Search schematics, tags, authors...", searchFieldX + 22, searchFieldY + 7, C_DARK);
        } else if (search != null && !search.getValue().isEmpty()) {
            boolean clearHover = over(mouseX, mouseY, clearSearchX, clearSearchY, clearSearchW, clearSearchH);
            drawIcon(g, IconType.CLOSE, clearSearchX + 4, clearSearchY + 4, clearHover ? C_WHITE : C_DARK);
        }

        // Tag Filter Button
        boolean tagHover = over(mouseX, mouseY, tagButtonX, tagButtonY, tagButtonW, tagButtonH);
        boolean tagActive = !"All".equalsIgnoreCase(tag);
        int tagBg = openDropdown == 1 ? C_SELECTED : (tagHover ? C_HOVER : (tagActive ? 0xFF142C23 : C_FIELD));
        int tagBrd = (openDropdown == 1 || tagActive) ? C_EMERALD : (tagHover ? C_CARD_BORDER_H : C_FIELD_BORDER);
        frameRounded(g, tagButtonX, tagButtonY, tagButtonW, tagButtonH, 3, tagBg, tagBrd);
        text(g, trim("Tag: " + tag, tagButtonW - 22), tagButtonX + 8, tagButtonY + 7, tagActive ? C_EMERALD_LIGHT : C_TEXT);
        drawIcon(g, IconType.CHEVRON_DOWN, tagButtonX + tagButtonW - 12, tagButtonY + 9, tagActive ? C_EMERALD_LIGHT : C_MUTED);

        // Version Filter Button
        boolean verHover = over(mouseX, mouseY, verButtonX, verButtonY, verButtonW, verButtonH);
        String cleanVer = SchematicDownloadManager.cleanVersion(version);
        boolean verActive = !"Any".equalsIgnoreCase(cleanVer);
        int verBg = openDropdown == 2 ? C_SELECTED : (verHover ? C_HOVER : (verActive ? 0xFF142C23 : C_FIELD));
        int verBrd = (openDropdown == 2 || verActive) ? C_EMERALD : (verHover ? C_CARD_BORDER_H : C_FIELD_BORDER);
        frameRounded(g, verButtonX, verButtonY, verButtonW, verButtonH, 3, verBg, verBrd);
        text(g, trim("MC: " + cleanVer, verButtonW - 20), verButtonX + 8, verButtonY + 7, verActive ? C_EMERALD_LIGHT : C_TEXT);
        drawIcon(g, IconType.CHEVRON_DOWN, verButtonX + verButtonW - 12, verButtonY + 9, verActive ? C_EMERALD_LIGHT : C_MUTED);

        // Reload Button
        boolean relHover = over(mouseX, mouseY, relButtonX, relButtonY, relButtonW, relButtonH);
        frameRounded(g, relButtonX, relButtonY, relButtonW, relButtonH, 3, relHover ? C_HOVER : C_FIELD, relHover ? C_CARD_BORDER_H : C_FIELD_BORDER);
        drawIcon(g, IconType.REFRESH, relButtonX + 10, relButtonY + 7, relHover ? C_EMERALD_LIGHT : C_MUTED);

        // 5. Bottom Navigation Bar
        int bottomY = height - BOTTOM_BAR_H;
        g.fill(0, bottomY, width, height, C_BOTTOM_BG);
        g.fill(0, bottomY, width, bottomY + 1, C_BOTTOM_BORDER);

        drawPager(g, mouseX, mouseY, bottomY);

        if (status != null && !status.isEmpty()) {
            textCentered(g, status, width / 2, bottomY + 11, statusColor);
        }

        String verInfo = "Litematica Hub v" + SchematicDownloadManager.currentVersion();
        text(g, verInfo, width - font.width(verInfo) - 14, bottomY + 11, C_DARK);

        // 6. Live Download Progress (Floating bottom card)
        drawDownloadProgress(g);

        // 7. Update Toast Notification
        drawUpdateToast(g, mouseX, mouseY);

        // 8. Tag Search Box visibility
        if (tagSearch != null) tagSearch.visible = (openDropdown == 1);

        // 9. Dropdowns on top of everything
        if (openDropdown != 0) {
            drawDropdown(g, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void drawPager(Graphics g, int mouseX, int mouseY, int bottomY) {
        int pages = totalPages();
        int contentW = getContentWidth();
        int cx = width / 2 - contentW / 2;

        pagerPrevX = cx;
        pagerPrevY = bottomY + 6;
        pagerPrevW = 28;
        pagerPrevH = 18;
        boolean prevHover = over(mouseX, mouseY, pagerPrevX, pagerPrevY, pagerPrevW, pagerPrevH);
        boolean canPrev = page > 0 && !loading;
        frameRounded(g, pagerPrevX, pagerPrevY, pagerPrevW, pagerPrevH, 3,
                canPrev ? (prevHover ? C_HOVER : C_FIELD) : 0xFF080D18, C_CARD_BORDER);
        textCentered(g, "<", pagerPrevX + pagerPrevW / 2, pagerPrevY + 5, canPrev ? C_WHITE : C_DARK);

        String pageLabel = (page + 1) + " / " + pages;
        int pw = font.width(pageLabel) + 16;
        int pagePillX = pagerPrevX + pagerPrevW + 6;
        drawPill(g, pagePillX, pagerPrevY, pw, pagerPrevH, C_FIELD, C_CARD_BORDER, pageLabel, C_TEXT_DIM);

        pagerNextX = pagePillX + pw + 6;
        pagerNextY = bottomY + 6;
        pagerNextW = 28;
        pagerNextH = 18;
        boolean nextHover = over(mouseX, mouseY, pagerNextX, pagerNextY, pagerNextW, pagerNextH);
        boolean canNext = (page + 1) < pages && !loading;
        frameRounded(g, pagerNextX, pagerNextY, pagerNextW, pagerNextH, 3,
                canNext ? (nextHover ? C_HOVER : C_FIELD) : 0xFF080D18, C_CARD_BORDER);
        textCentered(g, ">", pagerNextX + pagerNextW / 2, pagerNextY + 5, canNext ? C_WHITE : C_DARK);
    }

    private void drawDownloadProgress(Graphics g) {
        String title = downloadTitle;
        if (title == null) return;

        long tot = downloadTotal;
        long read = downloadRead;
        int barW = 260;
        int cardH = 38;
        int cx = width / 2 - barW / 2;
        int cy = height - BOTTOM_BAR_H - cardH - 10;

        frameRounded(g, cx - 12, cy - 6, barW + 24, cardH + 12, 4, 0xF6090E1A, C_EMERALD_BORDER);
        textCentered(g, "Downloading: " + trim(title, barW - 20), width / 2, cy, C_WHITE);

        int barH = 5;
        int by = cy + 14;
        frameRounded(g, cx, by, barW, barH, 2, 0xFF141D2D, C_CARD_BORDER);

        if (tot > 0L) {
            int filled = (int) Math.max(2L, Math.min(barW, (int) (barW * (double) read / tot)));
            g.fill(cx, by, cx + filled, by + barH, C_EMERALD);
            String info = fmtSize(read) + " / " + fmtSize(tot) + " (" + (int) ((read * 100) / tot) + "%)";
            textCentered(g, info, width / 2, by + barH + 4, C_MUTED);
        } else {
            int phase = (int) ((System.currentTimeMillis() / 4L) % (barW * 2L));
            int marker = phase < barW ? phase : (barW * 2 - phase);
            g.fill(cx, by, cx + marker, by + barH, C_EMERALD);
            textCentered(g, fmtSize(read), width / 2, by + barH + 4, C_MUTED);
        }
    }

    private void drawDropdown(Graphics g, int mouseX, int mouseY) {
        List<String[]> rows = openDropdown == 1
                ? SchematicDownloadManager.tagRows(tagSearch == null ? "" : tagSearch.getValue())
                : versionRows();
        String current = openDropdown == 1 ? tag : version;

        panelX = openDropdown == 1 ? tagButtonX : verButtonX;
        panelW = Math.max(140, openDropdown == 1 ? tagButtonW + 30 : verButtonW);
        int rowH = 17;
        int topPad = 4;
        boolean withSearch = openDropdown == 1;
        int searchH = withSearch ? 22 : 0;

        int available = Math.max(60, height - (openDropdown == 1 ? tagButtonY : verButtonY) - 70);
        int maxVisible = Math.max(3, (available - searchH - topPad - 4) / rowH);
        int visibleRows = Math.min(rows.size(), maxVisible);
        panelRowsCache = Math.max(1, visibleRows);

        panelH = searchH + topPad + visibleRows * rowH + 4;
        panelY = (openDropdown == 1 ? tagButtonY : verButtonY) + TOOLBAR_H + 3;
        if (panelY + panelH > height - 28) {
            panelY = Math.max(4, (openDropdown == 1 ? tagButtonY : verButtonY) - panelH - 3);
        }

        int maxScroll = Math.max(0, rows.size() - visibleRows);
        dropdownScroll = Math.max(0, Math.min(maxScroll, dropdownScroll));

        if (tagSearch != null && withSearch) {
            tagSearch.setX(panelX + 22);
            tagSearch.setY(panelY + 8);
            tagSearch.setWidth(Math.max(20, panelW - 32));
        }

        // Panel card with top emerald hairline
        frameRounded(g, panelX, panelY, panelW, panelH, 3, 0xF80B101D, C_CARD_BORDER);
        g.fill(panelX + 3, panelY, panelX + panelW - 3, panelY + 2, C_EMERALD);

        // Tags panel search box
        if (withSearch && tagSearch != null) {
            int fx = panelX + 5;
            int fy = panelY + 5;
            int fw = panelW - 10;
            int fh = 18;
            boolean fhOver = over(mouseX, mouseY, fx, fy, fw, fh);
            frameRounded(g, fx, fy, fw, fh, 2, fhOver ? C_FIELD_HOVER : C_FIELD, C_CARD_BORDER);
            drawIcon(g, IconType.SEARCH, fx + 5, fy + 5, C_MUTED);
            if (tagSearch.getValue().isEmpty()) {
                text(g, "Search tags...", fx + 18, fy + 5, C_DARK);
            }
        }

        int listTop = panelY + searchH + topPad;
        for (int i = 0; i < visibleRows; i++) {
            int idx = dropdownScroll + i;
            if (idx >= rows.size()) break;
            String[] row = rows.get(idx);
            String kind = row[0];
            String textVal = row[1];
            int ry = listTop + i * rowH;

            if ("group".equals(kind)) {
                g.fill(panelX + 4, ry + 1, panelX + panelW - 4, ry + rowH - 1, 0xFF141C2C);
                text(g, trim(textVal.toUpperCase(), panelW - 16), panelX + 8, ry + 5, C_EMERALD_LIGHT);
                continue;
            }
            if ("empty".equals(kind)) {
                text(g, textVal, panelX + 8, ry + 5, C_DARK);
                continue;
            }

            boolean hov = over(mouseX, mouseY, panelX + 3, ry, panelW - 6, rowH);
            boolean sel = textVal.equalsIgnoreCase(current);
            int bg = sel ? C_SELECTED : (hov ? C_HOVER : 0);
            if (bg != 0) {
                frameRounded(g, panelX + 3, ry, panelW - 6, rowH, 2, bg, sel ? C_EMERALD_BORDER : 0);
            }
            int color = sel ? C_EMERALD_LIGHT : (hov ? C_WHITE : C_TEXT_DIM);
            text(g, trim(textVal, panelW - 24), panelX + 8, ry + 5, color);

            if (sel) {
                drawIcon(g, IconType.CHECK, panelX + panelW - 14, ry + 6, C_EMERALD);
            }

            ItemBox box = new ItemBox();
            box.x = panelX + 3;
            box.y = ry;
            box.width = panelW - 6;
            box.height = rowH;
            box.value = textVal;
            dropdownItems.add(box);
        }

        if (rows.size() > visibleRows) {
            String scrollIndicator = (dropdownScroll + 1) + "/" + rows.size();
            text(g, scrollIndicator, panelX + panelW - font.width(scrollIndicator) - 6, panelY + panelH + 3, C_DARK);
        }
    }

    private List<String[]> versionRows() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"tag", "Any"});
        for (String v : availableVersions) {
            String cv = SchematicDownloadManager.cleanVersion(v);
            if (!cv.isEmpty()) {
                boolean exists = false;
                for (String[] r : rows) {
                    if (r[1].equalsIgnoreCase(cv)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    rows.add(new String[]{"tag", cv});
                }
            }
        }
        return rows;
    }

    private static int fade(int color, float t) {
        if (t >= 1f) return C_PAGE_BG;
        int br = (C_PAGE_BG >> 16) & 0xFF, bg = (C_PAGE_BG >> 8) & 0xFF, bb = C_PAGE_BG & 0xFF;
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int nr = (int) (r + (br - r) * t);
        int ng = (int) (g + (bg - g) * t);
        int nb = (int) (b + (bb - b) * t);
        return (nr << 16) | (ng << 8) | nb;
    }

    private void drawUpdateToast(Graphics g, int mouseX, int mouseY) {
        if (updateVersion.isBlank() || updateDismissed) return;

        float fadeT = 0f;
        if (updateCloseAt != 0L) {
            float p = Math.min(1f, (System.currentTimeMillis() - updateCloseAt) / 260f);
            fadeT = p * p;
            if (p >= 1f) {
                updateDismissed = true;
                updateCloseAt = 0L;
                return;
            }
        }

        String title = "Update Available";
        String line1 = "Current: v" + SchematicDownloadManager.currentVersion();
        String line2 = "Latest: v" + updateVersion;

        int padX = 14;
        int textW = Math.max(font.width(title), Math.max(font.width(line1), font.width(line2)));
        int btnW = font.width("Download Update") + 20;
        int w = Math.max(textW, btnW) + padX * 2 + 14;
        int h = 68;

        float t = Math.min(1f, (System.currentTimeMillis() - updateShownAt) / 340f);
        float e = 1f - (1f - t) * (1f - t);
        if (e <= 0f) return;
        int x = (int) (width - w - 14 + (1f - e) * (w + 30) + fadeT * 12);
        int y = (int) (height - h - 42 + (1f - e) * 26 + fadeT * 8);

        frameRounded(g, x, y, w, h, 4, fade(0xF80B101D, fadeT), fade(C_EMERALD_BORDER, fadeT));
        g.fill(x + 1, y + 6, x + 3, y + h - 6, fade(C_EMERALD, fadeT));

        text(g, title, x + padX, y + 10, fade(C_EMERALD_LIGHT, fadeT));
        text(g, line1 + " -> " + line2, x + padX, y + 24, fade(C_MUTED, fadeT));

        int cx = x + w - 16;
        int cy = y + 8;
        boolean closeHover = over(mouseX, mouseY, cx - 3, cy - 3, 14, 14);
        drawIcon(g, IconType.CLOSE, cx, cy, fade(closeHover ? C_WHITE : C_DARK, fadeT));
        toastCloseX = cx - 3;
        toastCloseY = cy - 3;

        int bh = 18;
        int bx = x + padX;
        int by = y + h - bh - 8;
        boolean dlHover = over(mouseX, mouseY, bx, by, btnW, bh);
        frameRounded(g, bx, by, btnW, bh, 3,
                fade(dlHover ? C_EMERALD : C_EMERALD_DARK, fadeT),
                fade(C_EMERALD_BORDER, fadeT));
        textCentered(g, "Download Update", bx + btnW / 2, by + 5, fade(C_WHITE, fadeT));
        toastDlX = bx; toastDlY = by; toastDlW = btnW; toastDlH = bh;
    }

    private void openWebsite() {
        openUrl(SITE_URL);
    }

    private void openUrl(String url) {
        boolean opened = false;
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                opened = true;
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
                opened = true;
            } else {
                new ProcessBuilder("xdg-open", url).start();
                opened = true;
            }
        } catch (Exception e) {
            ModInit.LOGGER.warn("Could not open url via shell", e);
        }
        if (!opened) {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                }
            } catch (Exception e) {
                ModInit.LOGGER.warn("Could not open url", e);
            }
        }
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (openDropdown != 0 && over(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            List<String[]> rows = openDropdown == 1
                    ? SchematicDownloadManager.tagRows(tagSearch == null ? "" : tagSearch.getValue())
                    : versionRows();
            int visibleRows = Math.min(rows.size(), panelRowsCache);
            int maxScroll = Math.max(0, rows.size() - visibleRows);
            dropdownScroll = Math.max(0, Math.min(maxScroll, dropdownScroll - (int) (verticalAmount * 2)));
            return true;
        }
        int maxScroll = Math.max(0, entries.size() * CARD_STEP - (height - Y_LIST - BOTTOM_BAR_H - 10));
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (verticalAmount * 24)));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        // Logo / Avatar click -> open site
        if (over(mouseX, mouseY, 14, 5, 24 + 8 + font.width("LITEMATICA HUB"), 24)) {
            openWebsite();
            return true;
        }

        // Header buttons
        if (over(mouseX, mouseY, webHeaderX, webHeaderY, webHeaderW, webHeaderH)) {
            openWebsite();
            return true;
        }
        if (over(mouseX, mouseY, closeHeaderX, closeHeaderY, closeHeaderW, closeHeaderH)) {
            onClose();
            return true;
        }

        // Clear search 'x' button
        if (search != null && !search.getValue().isEmpty() &&
                over(mouseX, mouseY, clearSearchX, clearSearchY, clearSearchW, clearSearchH)) {
            search.setValue("");
            reloadFromFirstPage();
            return true;
        }

        // Reload button
        if (over(mouseX, mouseY, relButtonX, relButtonY, relButtonW, relButtonH)) {
            reloadFromFirstPage();
            return true;
        }

        // Update toast actions
        if (!updateVersion.isBlank() && !updateDismissed) {
            if (over(mouseX, mouseY, toastCloseX, toastCloseY, 14, 14)) {
                if (updateCloseAt == 0L) updateCloseAt = System.currentTimeMillis();
                return true;
            }
            if (over(mouseX, mouseY, toastDlX, toastDlY, toastDlW, toastDlH)) {
                openUrl(MOD_DOWNLOAD_URL);
                return true;
            }
        }

        // Pagination buttons
        if (over(mouseX, mouseY, pagerPrevX, pagerPrevY, pagerPrevW, pagerPrevH)) {
            changePage(-1);
            return true;
        }
        if (over(mouseX, mouseY, pagerNextX, pagerNextY, pagerNextW, pagerNextH)) {
            changePage(1);
            return true;
        }

        // Dropdown selection
        if (openDropdown != 0) {
            for (ItemBox box : dropdownItems) {
                if (over(mouseX, mouseY, box.x, box.y, box.width, box.height)) {
                    if (openDropdown == 1) tag = box.value;
                    else version = SchematicDownloadManager.cleanVersion(box.value);
                    openDropdown = 0;
                    dropdownScroll = 0;
                    reloadFromFirstPage();
                    return true;
                }
            }
            if (!over(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
                boolean onFilterBtn = over(mouseX, mouseY, tagButtonX, tagButtonY, tagButtonW, tagButtonH)
                                   || over(mouseX, mouseY, verButtonX, verButtonY, verButtonW, verButtonH);
                openDropdown = 0;
                dropdownScroll = 0;
                if (!onFilterBtn) return true;
            }
        }

        // Filter toggles
        if (over(mouseX, mouseY, tagButtonX, tagButtonY, tagButtonW, tagButtonH)) {
            openDropdown = openDropdown == 1 ? 0 : 1;
            dropdownScroll = 0;
            if (tagSearch != null && openDropdown == 1) tagSearch.setValue("");
            return true;
        }
        if (over(mouseX, mouseY, verButtonX, verButtonY, verButtonW, verButtonH)) {
            openDropdown = openDropdown == 2 ? 0 : 2;
            dropdownScroll = 0;
            return true;
        }

        // Cover flipping (< and > arrows on thumbnail)
        int contentW = getContentWidth();
        int cx = width / 2 - contentW / 2;
        int y = Y_LIST - scroll;
        for (SchematicDownloadManager.SchematicEntry entry : entries) {
            if (y + CARD_H >= Y_LIST && y <= height - BOTTOM_BAR_H) {
                if (SchematicDownloadManager.coverCount(entry) > 1) {
                    int covX = cx + 8;
                    int covY = y + (CARD_H - COVER_H) / 2;
                    if (over(mouseX, mouseY, covX + 2, covY + 2, 14, 14)) {
                        flipCover(entry, -1);
                        return true;
                    }
                    if (over(mouseX, mouseY, covX + COVER_W - 16, covY + 2, 14, 14)) {
                        flipCover(entry, 1);
                        return true;
                    }
                }
            }
            y += CARD_STEP;
        }

        // Download & Site Buttons
        if (!downloading) {
            for (ItemBox info : downloadButtons) {
                if (!over(mouseX, mouseY, info.x, info.y, info.width, info.height)) continue;

                if ("site".equals(info.value)) {
                    openUrl(SITE_URL + "schematic/" + urlEncode(info.entry.id));
                    return true;
                }

                downloading = true;
                status = "Downloading \"" + info.entry.title + "\"...";
                statusColor = C_TEXT_DIM;
                downloadTitle = info.entry.title;
                downloadRead = 0L;
                downloadTotal = -1L;
                SchematicDownloadManager.downloadSchematic(info.entry, (success, message) -> {
                    downloading = false;
                    downloadTitle = null;
                    status = (success ? "" : "Error: ") + message;
                    statusColor = success ? C_EMERALD_LIGHT : C_ERROR;
                }, (read, totalBytes) -> {
                    downloadRead = read;
                    downloadTotal = totalBytes;
                });
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private static String fmtSize(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        if (bytes < 1024L * 1024L) return (bytes / 1024.0) + " KB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (openDropdown != 0 && event.isEscape()) {
            openDropdown = 0;
            dropdownScroll = 0;
            return true;
        }
        if (event.isConfirmation() && search != null && search.isFocused()) {
            reloadFromFirstPage();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Minecraft client = Minecraft.getInstance();
        if (parent != null) {
            client.setScreenAndShow(parent);
        } else if (client.level != null) {
            client.setScreenAndShow(null);
        } else {
            client.setScreenAndShow(new TitleScreen());
        }
    }
}

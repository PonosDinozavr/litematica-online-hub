package com.litematicahub;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class LitematicaHubScreen extends Screen {
    // --- site-like palette ---
    private static final int C_PAGE_BG = 0xF00B1220;
    private static final int C_CARD = 0xF0131B2E;
    private static final int C_CARD_BORDER = 0xFF1F293D;
    private static final int C_FIELD = 0xFF0B1220;
    private static final int C_TEXT = 0xFFE5E7EB;
    private static final int C_TEXT_DIM = 0xFFD1D5DB;
    private static final int C_MUTED = 0xFF6B7280;
    private static final int C_ACCENT = 0xFF34D399;
    private static final int C_ACCENT_DIM = 0xFF10B981;
    private static final int C_ERROR = 0xFFFF5555;
    private static final int C_HOVER = 0xFF1A2438;
    private static final int C_SELECTED = 0xFF16352B;

    // --- layout ---
    private static final int W_CONTENT = 460;
    private static final int CARD_H = 78;
    private static final int CARD_STEP = CARD_H + 8;
    private static final int COVER = 58;
    private static final int DESC_LINES = 2;
    private static final int BTN_W = 92;
    private static final int BTN_H = 18;

    private static final int Y_SEARCH = 30;
    private static final int Y_FILTERS = 56;
    private static final int Y_LIST = 86;
    private static final int FILTERS_H = 20;

    private final Screen parent;
    private EditBox search;
    private EditBox tagSearch;

    private String tag = "All";
    private String version = "Any";
    private final List<String> availableVersions = new ArrayList<>();

    private static final int PAGE_SIZE = 15;
    private int page;
    private int total;

    /** current cover index per schematic id (so flipping survives re-renders) */
    private final java.util.Map<String, Integer> coverIndices = new java.util.HashMap<>();
    private boolean offline;
    private String updateVersion = "";

    // update toast state (slides in from the bottom-right corner)
    private boolean updateDismissed;
    private long updateShownAt;
    private long updateCloseAt;
    private int toastCloseX, toastCloseY, toastDlX, toastDlY, toastDlW, toastDlH;

    /** 0 = closed, 1 = tags, 2 = versions */
    private int openDropdown;
    private int dropdownScroll;

    private List<SchematicDownloadManager.SchematicEntry> entries = List.of();
    private boolean loading;
    private boolean downloading;
    private int scroll;
    private String status = "Loading gallery...";
    private int statusColor = C_TEXT_DIM;
    // Live download progress for the entry currently being fetched.
    private volatile String downloadTitle = null;
    private volatile long downloadRead = 0L;
    private volatile long downloadTotal = -1L;

    private int tagButtonX, tagButtonY, tagButtonW, tagButtonH;
    private int verButtonX, verButtonY, verButtonW, verButtonH;
    private int panelX, panelY, panelW, panelH;
    private int panelRowsCache = 9;

    private static final String SITE_URL = "https://litematica-online-hub.shop/";
    private static final String SITE_LABEL = "Site";
    private static final String SITE_LABEL_HOVER = "Open site";
    private static final String SITE_LINK_TEXT = "open in browser";
    // where the "Download" button in the update toast sends you
    private static final String MOD_DOWNLOAD_URL = "https://explorer.com";
    private int linkX, linkY, linkW, linkH;
    private int pagerPrevX, pagerPrevY, pagerPrevW, pagerPrevH;
    private int pagerNextX, pagerNextY, pagerNextW, pagerNextH;
    private final List<ItemBox> downloadButtons = new ArrayList<>();
    private final List<ItemBox> dropdownItems = new ArrayList<>();

    private int searchFieldX, searchFieldY, searchFieldW, searchFieldH;
    private final List<UiButton> uiButtons = new ArrayList<>();

    /** A button we draw ourselves so it matches the mod's dark style. */
    private static class UiButton {
        int x, y, width, height;
        String label;
        int action; // 0 = search, 1 = back, 2 = reload
    }

    private UiButton makeButton(int x, int y, int w, int h, String label, int action) {
        UiButton b = new UiButton();
        b.x = x; b.y = y; b.width = w; b.height = h;
        b.label = label; b.action = action;
        return b;
    }

    private static class ItemBox {
        int x, y, width, height;
        String value;
        SchematicDownloadManager.SchematicEntry entry;
    }

    public LitematicaHubScreen(Screen parent) {
        super(Component.literal("Litematica Online Hub"));
        this.parent = parent;
        // pull the live tag list so user-created tags are selectable here too
        SchematicDownloadManager.refreshTags();
    }

    @Override
    protected void init() {
        int cx = width / 2 - W_CONTENT / 2;

        // search field: no MC chrome — we draw our own frame behind it
        search = addRenderableWidget(new EditBox(font, cx + 10, Y_SEARCH + 5, 282, 14, Component.literal("Search")));
        search.setMaxLength(128);
        search.setBordered(false);
        search.setTextColor(0xFFE5E7EB);
        search.setTextColorUneditable(0xFF6B7280);
        searchFieldX = cx;
        searchFieldY = Y_SEARCH;
        searchFieldW = 300;
        searchFieldH = 22;

        // custom buttons (drawn by us, not vanilla widgets)
        uiButtons.clear();
        uiButtons.add(makeButton(cx + 304, Y_SEARCH, 70, 22, "Search", 0));
        uiButtons.add(makeButton(cx + 378, Y_SEARCH, 82, 22, "Back", 1));

        tagButtonX = cx;
        verButtonX = cx + 156;
        tagButtonY = Y_FILTERS;
        verButtonY = Y_FILTERS;
        tagButtonW = 152;
        tagButtonH = FILTERS_H;
        verButtonW = 152;
        verButtonH = FILTERS_H;

        uiButtons.add(makeButton(cx + 312, Y_FILTERS, 148, FILTERS_H, "Reload", 2));

        // search field that lives inside the tags dropdown
        tagSearch = addRenderableWidget(new EditBox(font, 0, 0, 100, 14, Component.literal("Find tag")));
        tagSearch.setMaxLength(32);
        tagSearch.setBordered(false);
        tagSearch.setTextColor(0xFFE5E7EB);
        tagSearch.setTextColorUneditable(0xFF6B7280);
        tagSearch.visible = false;
        tagSearch.setResponder(v -> dropdownScroll = 0);

        SchematicDownloadManager.fetchVersions().whenComplete((res, err) -> {
            if (err == null && res != null) {
                Minecraft.getInstance().execute(() -> {
                    availableVersions.clear();
                    availableVersions.addAll(res);
                    if (!availableVersions.isEmpty() && version.equals("Any")
                            && !availableVersions.contains(version)) {
                        version = availableVersions.get(0);
                    }
                });
            }
        });

        // silently check whether a newer mod build was published
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
        statusColor = C_TEXT_DIM;
        String queryTag = "All".equals(tag) ? "" : tag;
        String queryVer = "Any".equals(version) ? "" : version;
        SchematicDownloadManager.fetchSchematics(search.getValue(), queryTag, queryVer, PAGE_SIZE, page * PAGE_SIZE)
                .whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                    loading = false;
                    if (error != null) {
                        // fall back to the last saved snapshot so the screen is still usable
                        List<SchematicDownloadManager.SchematicEntry> cached =
                                SchematicDownloadManager.loadCachedPage();
                        offline = true;
                        if (!cached.isEmpty()) {
                            entries = cached;
                            total = cached.size();
                            page = 0;
                            SchematicDownloadManager.requestThumbnails(entries);
                            status = "Offline - showing last saved gallery (" + cached.size() + ")";
                            statusColor = 0xFFF59E0B;
                        } else {
                            entries = List.of();
                            status = "Failed to load gallery: " + describeError(error);
                            statusColor = C_ERROR;
                        }
                    } else {
                        offline = false;
                        entries = result == null ? List.of() : result.entries;
                        total = result == null ? 0 : result.total;
                        // drop back to a valid page if the result set shrank
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
                            statusColor = C_TEXT_DIM;
                        } else {
                            int from = page * PAGE_SIZE + 1;
                            int to = from + entries.size() - 1;
                            status = from + "-" + to + " of " + total
                                    + (totalPages() > 1 ? "   (page " + (page + 1) + "/" + totalPages() + ")" : "");
                            statusColor = C_TEXT_DIM;
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

    /** Loads the current page; call with reset=true when a filter changed. */
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

    private String trim(String text, int maxWidth) {
        if (text == null) return "";
        if (font.width(text) <= maxWidth) return text;
        String cut = text;
        while (!cut.isEmpty() && font.width(cut + "...") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    private List<String> wrapLines(String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        String rest = text.replace("\r", "").trim();
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

    // ---------- small drawing helpers ----------

    private void text(Graphics g, String s, int x, int y, int color) {
        g.text(font, s, x, y, color, false);
    }

    private void textCentered(Graphics g, String s, int cx, int y, int color) {
        g.text(font, s, cx - font.width(s) / 2, y, color, false);
    }

    private void frame(Graphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
    }

    private interface Graphics {
        void fill(int x1, int y1, int x2, int y2, int color);
        void text(net.minecraft.client.gui.Font f, String s, int x, int y, int color, boolean shadow);
        void blit(Identifier id, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        final Graphics g = new Graphics() {
            public void fill(int x1, int y1, int x2, int y2, int color) {
                graphics.fill(x1, y1, x2, y2, color);
            }

            public void text(net.minecraft.client.gui.Font f, String s, int x, int y, int color, boolean shadow) {
                graphics.text(f, s, x, y, color, shadow);
            }

            public void blit(Identifier id, int x0, int y0, int x1, int y1,
                             float u0, float u1, float v0, float v1) {
                graphics.blit(id, x0, y0, x1, y1, u0, u1, v0, v1);
            }
        };

        // page background like the site
        graphics.fill(0, 0, width, height, C_PAGE_BG);

        textCentered(g, title.getString(), width / 2, 12, C_TEXT);

        if (offline) {
            textCentered(g, "offline", width / 2, 20, 0xFFF59E0B);
        }

        drawUpdateToast(g, mouseX, mouseY);

        downloadButtons.clear();
        dropdownItems.clear();

        // ---- filter dropdown buttons ----
        boolean tagHover = over(mouseX, mouseY, tagButtonX, tagButtonY, tagButtonW, tagButtonH);
        boolean verHover = over(mouseX, mouseY, verButtonX, verButtonY, verButtonW, verButtonH);
        int tagBg = openDropdown == 1 ? C_SELECTED : (tagHover ? C_HOVER : C_CARD);
        int verBg = openDropdown == 2 ? C_SELECTED : (verHover ? C_HOVER : C_CARD);
        frame(g, tagButtonX, tagButtonY, tagButtonW, tagButtonH, tagBg, C_CARD_BORDER);
        frame(g, verButtonX, verButtonY, verButtonW, verButtonH, verBg, C_CARD_BORDER);
        text(g, trim("Tag: " + tag, tagButtonW - 12), tagButtonX + 6, tagButtonY + 6, C_TEXT);
        text(g, trim("Version: " + version, verButtonW - 12), verButtonX + 6, verButtonY + 6, C_TEXT);

        // ---- our own search field frame + placeholder ----
        boolean searchHover = over(mouseX, mouseY, searchFieldX, searchFieldY, searchFieldW, searchFieldH);
        frame(g, searchFieldX, searchFieldY, searchFieldW, searchFieldH,
                searchHover ? C_HOVER : C_FIELD, C_CARD_BORDER);
        if (search != null && search.getValue().isEmpty()) {
            text(g, "Search by title, author or keyword...", searchFieldX + 10,
                    searchFieldY + 7, C_MUTED);
        }

        // ---- our own buttons ----
        for (UiButton b : uiButtons) {
            boolean hov = over(mouseX, mouseY, b.x, b.y, b.width, b.height);
            int bg = hov ? C_HOVER : C_CARD;
            if (b.action == 2) bg = hov ? C_ACCENT_DIM : C_ACCENT;
            frame(g, b.x, b.y, b.width, b.height, bg, C_CARD_BORDER);
            int color = b.action == 2 ? 0xFF06281C : (hov ? C_TEXT : C_TEXT_DIM);
            textCentered(g, b.label, b.x + b.width / 2, b.y + (b.height - 8) / 2 + 1, color);
        }

        // ---- cards (clipped so they never overlap the filter row) ----
        int cx = width / 2 - W_CONTENT / 2;
        int listTop = Y_LIST;
        int listBottom = height - 34;
        int visibleTop = listTop;
        int visibleBottom = listBottom;

        graphics.enableScissor(0, listTop, width, listBottom);
        int y = listTop - scroll;
        for (SchematicDownloadManager.SchematicEntry entry : entries) {
            if (y + CARD_H >= visibleTop && y <= visibleBottom) {
                frame(g, cx, y, W_CONTENT, CARD_H, C_CARD, C_CARD_BORDER);

                // cover (flip through all screenshots of this publication)
                int covX = cx + 8;
                int covY = y + 9;
                frame(g, covX - 1, covY - 1, COVER + 2, COVER + 2, C_FIELD, C_CARD_BORDER);
                int coverIdx = coverIndex(entry);
                Identifier cover = SchematicDownloadManager.getCover(entry, coverIdx);
                if (cover != null) {
                    g.blit(cover, covX, covY, covX + COVER, covY + COVER, 0f, 1f, 0f, 1f);
                } else {
                    g.fill(covX, covY, covX + COVER, covY + COVER, C_FIELD);
                    textCentered(g, "no cover", covX + COVER / 2, covY + COVER / 2 - 4, C_MUTED);
                }
                int coverTotal = SchematicDownloadManager.coverCount(entry);
                if (coverTotal > 1) {
                    // small arrows + counter on the cover
                    g.fill(covX, covY, covX + 10, covY + 10, 0x88000000);
                    textCentered(g, "<", covX + 5, covY + 2, C_TEXT);
                    g.fill(covX + COVER - 10, covY, covX + COVER, covY + 10, 0x88000000);
                    textCentered(g, ">", covX + COVER - 5, covY + 2, C_TEXT);
                    String counter = (coverIdx + 1) + "/" + coverTotal;
                    int cw = font.width(counter);
                    g.fill(covX + COVER - cw - 4, covY + COVER - 10, covX + COVER, covY + COVER, 0x88000000);
                    text(g, counter, covX + COVER - cw - 2, covY + COVER - 9, C_TEXT_DIM);
                }

                int tx = cx + COVER + 18;
                int right = cx + W_CONTENT - 8;
                int btnX = right - BTN_W;
                int topW = btnX - 8 - tx;

                text(g, trim(entry.title, topW), tx, y + 9, C_TEXT);
                text(g, trim(metaLine(entry), topW), tx, y + 21, C_TEXT_DIM);

                List<String> desc = wrapLines(entry.description, right - tx, DESC_LINES);
                for (int i = 0; i < desc.size(); i++) {
                    text(g, desc.get(i), tx, y + 33 + i * 10, C_MUTED);
                }

                text(g, entry.downloads + " downloads   " + entry.likes + " likes", tx, y + 58, C_MUTED);

                // small link that opens this publication on the website (bottom-right corner)
                int linkWidth = font.width(SITE_LINK_TEXT);
                int linkX = cx + W_CONTENT - 7 - linkWidth;
                boolean linkHover = over(mouseX, mouseY, linkX, y + CARD_H - 14, linkWidth, 11);
                text(g, SITE_LINK_TEXT, linkX, y + CARD_H - 12, linkHover ? C_TEXT_DIM : C_MUTED);
                g.fill(linkX, y + CARD_H - 3, linkX + linkWidth, y + CARD_H - 2,
                        linkHover ? C_TEXT_DIM : C_MUTED);

                // download button with visible label
                boolean bHover = over(mouseX, mouseY, btnX, y + 9, BTN_W, BTN_H);
                int btnColor = downloading ? 0xFF555B66 : bHover ? C_ACCENT : C_ACCENT_DIM;
                g.fill(btnX, y + 9, btnX + BTN_W, y + 9 + BTN_H, btnColor);
                String label = downloading ? "..." : "Download";
                textCentered(g, label, btnX + BTN_W / 2, y + 9 + (BTN_H - 8) / 2 + 1, 0xFFFFFFFF);

                ItemBox info = new ItemBox();
                info.x = btnX;
                info.y = y + 9;
                info.width = BTN_W;
                info.height = BTN_H;
                info.entry = entry;
                downloadButtons.add(info);

                ItemBox siteBox = new ItemBox();
                siteBox.x = cx + W_CONTENT - 7 - font.width(SITE_LINK_TEXT);
                siteBox.y = y + CARD_H - 14;
                siteBox.width = font.width(SITE_LINK_TEXT);
                siteBox.height = 11;
                siteBox.entry = entry;
                siteBox.value = "site";
                downloadButtons.add(siteBox);
            }
            y += CARD_STEP;
        }
        graphics.disableScissor();

        if (entries.isEmpty() && !loading) {
            textCentered(g, "Nothing found", width / 2, listTop + 20, C_MUTED);
        }
        if (loading) {
            textCentered(g, "Loading...", width / 2, listTop + 4, C_ACCENT);
        }

        // ---- the in-dropdown search box must follow the dropdown state ----
        if (tagSearch != null) tagSearch.visible = (openDropdown == 1);

        // ---- dropdown panel on top (scrollable, never clipped) ----
        if (openDropdown != 0) {
            drawDropdown(g, mouseX, mouseY);
        }

        // ---- pagination arrows (bottom-left) ----
        drawPager(g, mouseX, mouseY);

        // ---- text link to the website (bottom-right corner) ----
        boolean linkHoverLast = over(mouseX, mouseY, width - font.width(SITE_LABEL_HOVER) - 10,
                height - 22, font.width(SITE_LABEL_HOVER) + 2, 10);
        String linkLabel = linkHoverLast ? SITE_LABEL_HOVER : SITE_LABEL;
        linkW = font.width(linkLabel);
        linkX = width - linkW - 10;
        linkY = height - 22;
        linkH = 10;
        text(g, linkLabel, linkX, linkY, linkHoverLast ? C_ACCENT : C_MUTED);
        g.fill(linkX, linkY + 9, linkX + linkW, linkY + 10, linkHoverLast ? C_ACCENT : C_MUTED);

        if (status != null && !status.isEmpty()) {
            textCentered(g, status, width / 2, height - 24, statusColor);
        }

        drawDownloadProgress(g);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    /** Progress bar shown while a schematic is being written to disk. */
    private void drawDownloadProgress(Graphics g) {
        String title = downloadTitle;
        if (title == null) return;

        long total = downloadTotal;
        long read = downloadRead;
        int barW = 220;
        int barH = 5;
        int cx = width / 2 - barW / 2;
        int cy = height - 46;

        textCentered(g, title, width / 2, cy - 12, C_TEXT);

        frame(g, cx, cy, barW, barH, 0xFF1B2430, C_ACCENT);
        if (total > 0L) {
            int filled = (int) Math.max(1L, Math.min(barW, (int) (barW * (double) read / total)));
            g.fill(cx + 1, cy + 1, cx + filled, cy + barH - 1, C_ACCENT);
            textCentered(g, fmtSize(read) + " / " + fmtSize(total), width / 2, cy + barH + 4, C_MUTED);
        } else {
            // No content-length: sweep a marker back and forth so it still looks alive.
            int phase = (int) ((System.currentTimeMillis() / 4L) % (barW * 2L));
            int marker = phase < barW ? phase : (barW * 2 - phase);
            g.fill(cx + 1, cy + 1, cx + marker, cy + barH - 1, C_ACCENT);
            textCentered(g, fmtSize(read), width / 2, cy + barH + 4, C_MUTED);
        }
    }

    private void drawDropdown(Graphics g, int mouseX, int mouseY) {
        List<String[]> rows = openDropdown == 1
                ? SchematicDownloadManager.tagRows(tagSearch == null ? "" : tagSearch.getValue())
                : versionRows();
        String current = openDropdown == 1 ? tag : version;

        panelX = openDropdown == 1 ? tagButtonX : verButtonX;
        panelW = openDropdown == 1 ? tagButtonW : verButtonW;
        int rowH = 16;
        int topPad = 3;
        boolean withSearch = openDropdown == 1;
        int searchH = withSearch ? 20 : 0;

        // compute how many rows we can show
        int available = Math.max(60, height - (openDropdown == 1 ? tagButtonY : verButtonY) - 70);
        int maxVisible = Math.max(3, (available - searchH - topPad - 3) / rowH);
        int visibleRows = Math.min(rows.size(), maxVisible);
        panelRowsCache = Math.max(1, visibleRows);

        panelH = searchH + topPad + visibleRows * rowH + 3;
        panelY = (openDropdown == 1 ? tagButtonY : verButtonY) + FILTERS_H + 2;
        if (panelY + panelH > height - 28) {
            panelY = Math.max(4, (openDropdown == 1 ? tagButtonY : verButtonY) - panelH - 2);
        }

        int maxScroll = Math.max(0, rows.size() - visibleRows);
        dropdownScroll = Math.max(0, Math.min(maxScroll, dropdownScroll));

        // position the in-dropdown search box over the panel
        if (tagSearch != null && withSearch) {
            tagSearch.setX(panelX + 14);
            tagSearch.setY(panelY + 9);
            tagSearch.setWidth(Math.max(20, panelW - 22));
        }

        // panel background
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, C_CARD);
        g.fill(panelX, panelY, panelX + panelW, panelY + 1, C_ACCENT);
        g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, C_CARD_BORDER);
        g.fill(panelX, panelY, panelX + 1, panelY + panelH, C_CARD_BORDER);
        g.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, C_CARD_BORDER);

        // our own search field inside the tags panel (the EditBox itself has no chrome)
        if (withSearch && tagSearch != null) {
            int fx = panelX + 4;
            int fy = panelY + 4;
            int fw = panelW - 8;
            int fh = 18;
            boolean fhOver = over(mouseX, mouseY, fx, fy, fw, fh);
            frame(g, fx, fy, fw, fh, fhOver ? C_HOVER : C_FIELD, C_CARD_BORDER);
            // small magnifier drawn from two rectangles
            int lx = fx + 7, ly = fy + 9;
            g.fill(lx, ly - 4, lx + 1, ly + 3, C_MUTED);
            g.fill(lx, ly - 4, lx + 4, ly - 3, C_MUTED);
            g.fill(lx + 3, ly - 1, lx + 4, ly, C_MUTED);
            g.fill(lx + 1, ly + 3, lx + 2, ly + 4, C_MUTED);
            if (tagSearch.getValue().isEmpty()) {
                text(g, "find tag", fx + 16, fy + 6, C_MUTED);
            }
        }

        int listTop = panelY + searchH + topPad;
        for (int i = 0; i < visibleRows; i++) {
            int idx = dropdownScroll + i;
            if (idx >= rows.size()) break;
            String[] row = rows.get(idx);
            String kind = row[0];
            String text = row[1];
            int ry = listTop + i * rowH;

            if ("group".equals(kind)) {
                g.fill(panelX + 2, ry, panelX + panelW - 2, ry + rowH, C_FIELD);
                text(g, trim(text.toUpperCase(), panelW - 16), panelX + 8, ry + 5, C_ACCENT);
                continue;
            }
            if ("empty".equals(kind)) {
                text(g, text, panelX + 8, ry + 5, C_MUTED);
                continue;
            }

            boolean hov = over(mouseX, mouseY, panelX + 2, ry, panelW - 4, rowH);
            boolean sel = text.equals(current);
            int bg = sel ? C_SELECTED : (hov ? C_HOVER : C_CARD);
            g.fill(panelX + 2, ry, panelX + panelW - 2, ry + rowH, bg);
            int color = sel ? C_ACCENT : (hov ? C_TEXT : C_TEXT_DIM);
            text(g, trim(text, panelW - 16), panelX + 8, ry + 5, color);

            ItemBox box = new ItemBox();
            box.x = panelX + 2;
            box.y = ry;
            box.width = panelW - 4;
            box.height = rowH;
            box.value = text;
            dropdownItems.add(box);
        }

        if (rows.size() > visibleRows) {
            textRight(g, "v " + (dropdownScroll + 1) + "/" + rows.size(),
                    panelX + panelW - 4, panelY + panelH + 3, C_MUTED);
        }
    }

    private List<String[]> versionRows() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"tag", "Any"});
        for (String v : availableVersions) {
            rows.add(new String[]{"tag", v});
        }
        return rows;
    }

    private void textRight(Graphics g, String s, int rightX, int y, int color) {
        g.text(font, s, rightX - font.width(s), y, color, false);
    }

/** Blend a colour toward the page background — used for the fade-out. */
    private static int fade(int color, float t) {
        if (t >= 1f) return C_PAGE_BG;
        int br = (C_PAGE_BG >> 16) & 0xFF, bg = (C_PAGE_BG >> 8) & 0xFF, bb = C_PAGE_BG & 0xFF;
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int nr = (int) (r + (br - r) * t);
        int ng = (int) (g + (bg - g) * t);
        int nb = (int) (b + (bb - b) * t);
        return (nr << 16) | (ng << 8) | nb;
    }

    /** Update notification: slides up from the bottom-right corner, with close + download. */
    private void drawUpdateToast(Graphics g, int mouseX, int mouseY) {
        if (updateVersion.isBlank() || updateDismissed) return;

        // fade-out phase
        float fadeT = 0f;
        if (updateCloseAt != 0L) {
            float p = Math.min(1f, (System.currentTimeMillis() - updateCloseAt) / 260f);
            fadeT = p * p; // accelerate away
            if (p >= 1f) {
                updateDismissed = true;
                updateCloseAt = 0L;
                return;
            }
        }

        String title = "New version available";
        String line1 = "You have " + SchematicDownloadManager.currentVersion();
        String line2 = "Latest is " + updateVersion;

        int padX = 12;
        int textW = Math.max(font.width(title), Math.max(font.width(line1), font.width(line2)));
        int btnW = font.width("Download") + 22;
        int w = Math.max(textW, btnW) + padX * 2 + 14;
        int h = 68;

        float t = Math.min(1f, (System.currentTimeMillis() - updateShownAt) / 340f);
        float e = 1f - (1f - t) * (1f - t);
        if (e <= 0f) return;
        int x = (int) (width - w - 14 + (1f - e) * (w + 30) + fadeT * 12);
        int y = (int) (height - h - 46 + (1f - e) * 26 + fadeT * 8);

        frame(g, x, y, w, h, fade(C_CARD, fadeT), fade(C_CARD_BORDER, fadeT));
        g.fill(x + 1, y + 1, x + 3, y + h - 1, fade(C_ACCENT, fadeT));

        text(g, title, x + padX, y + 9, fade(C_ACCENT, fadeT));
        text(g, line1, x + padX, y + 23, fade(C_MUTED, fadeT));
        text(g, line2, x + padX, y + 34, fade(C_TEXT, fadeT));

        int cx = x + w - 15;
        int cy = y + 7;
        boolean closeHover = over(mouseX, mouseY, cx - 3, cy - 3, 14, 14);
        int closeCol = fade(closeHover ? C_TEXT : C_MUTED, fadeT);
        for (int i = 0; i < 5; i++) {
            g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, closeCol);
            g.fill(cx + 4 - i, cy + i, cx + 5 - i, cy + i + 1, closeCol);
        }
        toastCloseX = cx - 3;
        toastCloseY = cy - 3;

        int bh = 17;
        int bx = x + padX;
        int by = y + h - bh - 8;
        boolean dlHover = over(mouseX, mouseY, bx, by, btnW, bh);
        g.fill(bx, by, bx + btnW, by + bh,
                fade(dlHover ? C_ACCENT : C_ACCENT_DIM, fadeT));
        textCentered(g, "Download", bx + btnW / 2, by + 5, fade(0xFF06281C, fadeT));
        toastDlX = bx; toastDlY = by; toastDlW = btnW; toastDlH = bh;
    }

        private void drawPager(Graphics g, int mouseX, int mouseY) {
        int pages = totalPages();
        int cx = width / 2 - W_CONTENT / 2;
        int y = height - 26;

        pagerPrevX = cx;
        pagerPrevY = y;
        pagerPrevW = 34;
        pagerPrevH = 12;
        boolean prevHover = over(mouseX, mouseY, pagerPrevX, pagerPrevY, pagerPrevW, pagerPrevH);
        boolean canPrev = page > 0 && !loading;
        g.fill(pagerPrevX, pagerPrevY, pagerPrevX + pagerPrevW, pagerPrevY + pagerPrevH,
                canPrev ? (prevHover ? C_HOVER : C_CARD) : C_FIELD);
        textCentered(g, "<", pagerPrevX + pagerPrevW / 2, pagerPrevY + 3, canPrev ? C_TEXT : C_MUTED);

        String label = (page + 1) + " / " + pages;
        text(g, label, pagerPrevX + pagerPrevW + 8, pagerPrevY + 3, C_TEXT_DIM);

        pagerNextX = pagerPrevX + pagerPrevW + 12 + font.width(label) + 8;
        pagerNextY = y;
        pagerNextW = 34;
        pagerNextH = 12;
        boolean nextHover = over(mouseX, mouseY, pagerNextX, pagerNextY, pagerNextW, pagerNextH);
        boolean canNext = (page + 1) < pages && !loading;
        g.fill(pagerNextX, pagerNextY, pagerNextX + pagerNextW, pagerNextY + pagerNextH,
                canNext ? (nextHover ? C_HOVER : C_CARD) : C_FIELD);
        textCentered(g, ">", pagerNextX + pagerNextW / 2, pagerNextY + 3, canNext ? C_TEXT : C_MUTED);
    }

    private void openWebsite() {
        openUrl(SITE_URL);
    }

    /** Opens any http(s) URL in the system browser. */
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

    private String metaLine(SchematicDownloadManager.SchematicEntry e) {
        StringBuilder sb = new StringBuilder("by ").append(e.author);
        if (e.tag != null && !e.tag.isEmpty()) sb.append("  [").append(e.tag).append("]");
        if (e.version != null && !e.version.isEmpty()) sb.append("  |  v").append(e.version);
        return sb.toString();
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (openDropdown != 0) {
            List<String[]> rows = openDropdown == 1
                    ? SchematicDownloadManager.tagRows(tagSearch == null ? "" : tagSearch.getValue())
                    : versionRows();
            int visibleRows = Math.min(rows.size(), panelRowsCache);
            int maxScroll = Math.max(0, rows.size() - visibleRows);
            dropdownScroll = Math.max(0, Math.min(maxScroll, dropdownScroll - (int) (verticalAmount * 2)));
            return true;
        }
        int maxScroll = Math.max(0, entries.size() * CARD_STEP - (height - Y_LIST - 34));
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (verticalAmount * 24)));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        // our own buttons (search / back / reload)
        for (UiButton b : uiButtons) {
            if (!over(mouseX, mouseY, b.x, b.y, b.width, b.height)) continue;
            if (b.action == 0) reloadFromFirstPage();
            else if (b.action == 1) onClose();
            else reloadFromFirstPage();
            return true;
        }

        // update toast: close / download
        if (!updateVersion.isBlank() && !updateDismissed) {
            if (over(mouseX, mouseY, toastCloseX, toastCloseY, 14, 14)) {
                // fade out instead of vanishing instantly
                if (updateCloseAt == 0L) updateCloseAt = System.currentTimeMillis();
                return true;
            }
            if (over(mouseX, mouseY, toastDlX, toastDlY, toastDlW, toastDlH)) {
                openUrl(MOD_DOWNLOAD_URL);
                return true;
            }
        }

        // site link opens the website
        if (over(mouseX, mouseY, linkX, linkY, linkW, linkH)) {
            openWebsite();
            return true;
        }

        // pagination
        if (over(mouseX, mouseY, pagerPrevX, pagerPrevY, pagerPrevW, pagerPrevH)) {
            changePage(-1);
            return true;
        }
        if (over(mouseX, mouseY, pagerNextX, pagerNextY, pagerNextW, pagerNextH)) {
            changePage(1);
            return true;
        }

        // dropdown items first
        if (openDropdown != 0) {
            for (ItemBox box : dropdownItems) {
                if (over(mouseX, mouseY, box.x, box.y, box.width, box.height)) {
                    if (openDropdown == 1) tag = box.value;
                    else version = box.value;
                    openDropdown = 0;
                    dropdownScroll = 0;
                    reloadFromFirstPage();
                    return true;
                }
            }
            // clicking anywhere outside the panel closes it (and must not fall
            // through to the toggle below, or it would immediately reopen)
            if (!over(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
                openDropdown = 0;
                dropdownScroll = 0;
                return true;
            }
        }

        // filter toggles
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

        // cover arrows: flip through the publication's screenshots
        for (ItemBox info : downloadButtons) {
            if (SchematicDownloadManager.coverCount(info.entry) <= 1) continue;
            int covX = info.x + COVER + 10 - BTN_W;
            int covY = info.y - 1;
            if (over(mouseX, mouseY, covX, covY, 10, 10)) { flipCover(info.entry, -1); return true; }
            if (over(mouseX, mouseY, covX + COVER - 10, covY, 10, 10)) { flipCover(info.entry, 1); return true; }
        }

        // download buttons
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
                    statusColor = success ? C_ACCENT : C_ERROR;
                }, (read, total) -> {
                    // Arrives on a worker thread; the fields are volatile so the
                    // render thread always sees the latest numbers.
                    downloadRead = read;
                    downloadTotal = total;
                });
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Human readable byte count for the download progress readout. */
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
        if (openDropdown != 0 && (event.key() == 256 || event.key() == 1)) { // ESC
            openDropdown = 0;
            dropdownScroll = 0;
            return true;
        }
        // Enter inside the search box runs the search
        if (event.key() == 257 && search != null && search.isFocused()) {
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
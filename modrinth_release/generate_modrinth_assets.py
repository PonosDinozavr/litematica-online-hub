import os
import sys
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUTPUT_DIR = r"C:\Users\user\Desktop\Frog Studio\000 LITEMATICA-ONLINE-HUB\modrinth_release"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# -------------------------------------------------------------
# Fonts & Colors
# -------------------------------------------------------------
try:
    FONT_TITLE = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 26)
    FONT_HEADING = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 18)
    FONT_BODY = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 14)
    FONT_SMALL = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 12)
    FONT_BOLD = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 14)
    FONT_MONO = ImageFont.truetype("C:/Windows/Fonts/consola.ttf", 13)
    FONT_CHAT = ImageFont.truetype("C:/Windows/Fonts/consola.ttf", 15)
except Exception:
    FONT_TITLE = FONT_HEADING = FONT_BODY = FONT_SMALL = FONT_BOLD = FONT_MONO = FONT_CHAT = ImageFont.load_default()

C_PAGE_BG        = (7, 11, 20)        # Deep navy #070b14
C_HEADER_BG      = (11, 16, 29)       # #0b101d
C_HEADER_BORDER  = (25, 34, 55)
C_CARD           = (15, 23, 42)       # Slate dark #0f172a
C_CARD_HOVER     = (20, 32, 54)
C_CARD_BORDER    = (30, 41, 59)
C_COVER_BG       = (9, 14, 26)
C_FIELD          = (9, 14, 26)
C_FIELD_BORDER   = (30, 41, 59)
C_EMERALD        = (16, 185, 129)     # #10b981
C_EMERALD_LIGHT  = (52, 211, 153)     # #34d399
C_EMERALD_DARK   = (5, 150, 105)      # #059669
C_EMERALD_BORDER = (16, 185, 129, 140)
C_TEXT           = (241, 245, 249)    # #f1f5f9
C_TEXT_MUTED     = (148, 163, 184)    # #94a3b8
C_TEXT_DIM       = (100, 116, 139)    # #64748b

# -------------------------------------------------------------
# 1. ICON (512x512)
# -------------------------------------------------------------
def make_icon():
    print("Generating icon.png...")
    src_path = r"C:\LitematicaWebHub\public\avatar.png"
    src = Image.open(src_path).convert("RGBA")
    
    # Target 512x512 canvas
    size = 512
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # Outer rounded background with dark emerald gradient
    draw = ImageDraw.Draw(im)
    rad = 96
    
    # Base dark container
    draw.rounded_rectangle([16, 16, size - 16, size - 16], radius=rad, fill=(11, 16, 29, 255), outline=C_EMERALD_DARK, width=4)
    
    # Emerald glow ring
    for i in range(3):
        w = size - 16 - i * 2
        draw.rounded_rectangle([16 + i * 2, 16 + i * 2, w, w], radius=rad - i, outline=(16, 185, 129, 60 - i * 20), width=2)
        
    # Scale avatar to fit nicely inside
    inner_pad = 48
    avatar_resized = src.resize((size - inner_pad * 2, size - inner_pad * 2), Image.Resampling.LANCZOS)
    
    # Mask with rounded corners
    mask = Image.new("L", avatar_resized.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, avatar_resized.width, avatar_resized.height], radius=rad - 24, fill=255)
    
    im.paste(avatar_resized, (inner_pad, inner_pad), mask)
    
    out_path = os.path.join(OUTPUT_DIR, "icon.png")
    im.save(out_path, "PNG")
    print(f"Saved {out_path}")

# -------------------------------------------------------------
# 2. BANNER (1280x640)
# -------------------------------------------------------------
def make_banner():
    print("Generating banner.png...")
    w, h = 1280, 640
    im = Image.new("RGBA", (w, h), (7, 11, 20, 255))
    draw = ImageDraw.Draw(im)
    
    # Subtle ambient radial glow in the center / top
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    for r in range(400, 0, -20):
        alpha = int(25 * (1 - r / 400.0))
        gdraw.ellipse([w // 2 - r, 160 - r // 2, w // 2 + r, 160 + r // 2], fill=(16, 185, 129, alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(30))
    im = Image.alpha_composite(im, glow)
    draw = ImageDraw.Draw(im)
    
    # Grid pattern in background
    for x in range(0, w, 40):
        draw.line([(x, 0), (x, h)], fill=(20, 30, 48, 60), width=1)
    for y in range(0, h, 40):
        draw.line([(0, y), (w, y)], fill=(20, 30, 48, 60), width=1)
        
    # Center logo avatar (128x128)
    avatar_src = Image.open(r"C:\LitematicaWebHub\public\avatar.png").convert("RGBA")
    ava_sz = 140
    ava = avatar_src.resize((ava_sz, ava_sz), Image.Resampling.LANCZOS)
    
    ava_mask = Image.new("L", (ava_sz, ava_sz), 0)
    ImageDraw.Draw(ava_mask).rounded_rectangle([0, 0, ava_sz, ava_sz], radius=32, fill=255)
    
    cx = w // 2
    ava_x = cx - ava_sz // 2
    ava_y = 90
    
    # Avatar outer glow & frame
    draw.rounded_rectangle([ava_x - 6, ava_y - 6, ava_x + ava_sz + 6, ava_y + ava_sz + 6], radius=36, fill=(16, 185, 129, 40), outline=C_EMERALD, width=2)
    im.paste(ava, (ava_x, ava_y), ava_mask)
    draw = ImageDraw.Draw(im)
    
    # Title
    t1 = "LITEMATICA ONLINE HUB"
    bbox1 = draw.textbbox((0, 0), t1, font=FONT_TITLE)
    tw1 = bbox1[2] - bbox1[0]
    draw.text((cx - tw1 // 2, 260), t1, fill=C_TEXT, font=FONT_TITLE)
    
    # Subtitle
    t2 = "In-Game Schematic Gallery & 1-Click Downloader for Minecraft"
    bbox2 = draw.textbbox((0, 0), t2, font=FONT_HEADING)
    tw2 = bbox2[2] - bbox2[0]
    draw.text((cx - tw2 // 2, 305), t2, fill=C_EMERALD_LIGHT, font=FONT_HEADING)
    
    # Feature Badges
    badges = [
        "In-Game Gallery Browser",
        "1-Click Direct Download",
        "Multi-Screenshot Carousel",
        "Clean Emerald Design",
        "MC 1.21.11 - 26.3 Fabric"
    ]
    by = 380
    total_badge_w = 0
    badge_boxes = []
    for b in badges:
        bb = draw.textbbox((0, 0), b, font=FONT_BODY)
        bw = (bb[2] - bb[0]) + 32
        badge_boxes.append((b, bw))
        total_badge_w += bw + 14
    total_badge_w -= 14
    
    cur_x = cx - total_badge_w // 2
    for b, bw in badge_boxes:
        draw.rounded_rectangle([cur_x, by, cur_x + bw, by + 34], radius=17, fill=(15, 23, 42, 220), outline=C_EMERALD_DARK, width=1)
        bb = draw.textbbox((0, 0), b, font=FONT_BODY)
        tw = bb[2] - bb[0]
        draw.text((cur_x + (bw - tw) // 2, by + 7), b, fill=C_TEXT, font=FONT_BODY)
        cur_x += bw + 14
        
    # Supported versions pills
    vy = 450
    v_text = "Supported: 26.3  |  26.2  |  26.1.2  |  26.1.1  |  26.1  |  1.21.11"
    v_bb = draw.textbbox((0, 0), v_text, font=FONT_MONO)
    v_w = v_bb[2] - v_bb[0]
    draw.rounded_rectangle([cx - v_w // 2 - 20, vy, cx + v_w // 2 + 20, vy + 30], radius=6, fill=(11, 16, 29, 200), outline=(30, 41, 59), width=1)
    draw.text((cx - v_w // 2, vy + 7), v_text, fill=C_TEXT_MUTED, font=FONT_MONO)
    
    # Official website & Frog Studio footer
    ft = "Official Website: litematica-online-hub.shop   •   Developed by Frog Studio"
    f_bb = draw.textbbox((0, 0), ft, font=FONT_SMALL)
    fw = f_bb[2] - f_bb[0]
    draw.text((cx - fw // 2, 580), ft, fill=C_TEXT_DIM, font=FONT_SMALL)
    
    out_path = os.path.join(OUTPUT_DIR, "banner.png")
    im.save(out_path, "PNG")
    print(f"Saved {out_path}")

# -------------------------------------------------------------
# 3. SCREENSHOT 1: GALLERY (1920x1080)
# -------------------------------------------------------------
def make_screenshot_gallery():
    print("Generating screenshot_1_gallery.png...")
    raw_path = os.path.join(OUTPUT_DIR, "raw_ui_capture.png")
    raw = Image.open(raw_path).convert("RGBA")
    
    cleaned = raw.copy()
    c_draw = ImageDraw.Draw(cleaned)
    
    # 1. Clean player nickname pill at top left (x=38..150, y=5..30)
    c_draw.rectangle([38, 5, 160, 32], fill=(11, 16, 29, 255))
    
    # 2. Place official rounded avatar at (14, 5, 38, 29) -> 24x24
    ava_src = Image.open(r"C:\LitematicaWebHub\public\avatar.png").convert("RGBA")
    ava24 = ava_src.resize((24, 24), Image.Resampling.LANCZOS)
    ava_m = Image.new("L", (24, 24), 0)
    ImageDraw.Draw(ava_m).rounded_rectangle([0, 0, 24, 24], radius=6, fill=255)
    cleaned.paste(ava24, (14, 5), ava_m)
    
    # 3. Clean version dropdown
    c_draw.rectangle([515, 36, 595, 54], fill=(9, 14, 26, 255), outline=C_FIELD_BORDER, width=1)
    c_draw.text((525, 39), "26.3 v", fill=C_TEXT_MUTED, font=FONT_SMALL)
    
    # 4. Clean [LF] glyph in card 2 description
    c_draw.rectangle([135, 290, 750, 340], fill=(15, 23, 42, 255))
    c_draw.text((135, 292), "Механизм-ловушка с выдвижением на 2 блока и открытием ямы", fill=C_TEXT, font=FONT_BOLD)
    c_draw.text((135, 312), "Компактная ловушка для баз и серверов. Надежная схема с поршневым затвором.", fill=C_TEXT_MUTED, font=FONT_BODY)
    
    # Upscale into 1920x1080 canvas
    W, H = 1920, 1080
    canvas = Image.new("RGBA", (W, H), (7, 11, 20, 255))
    
    scaled_w = 1720
    scaled_h = int(cleaned.height * (scaled_w / cleaned.width))
    gui_scaled = cleaned.resize((scaled_w, scaled_h), Image.Resampling.LANCZOS)
    
    shadow = Image.new("RGBA", (scaled_w + 40, scaled_h + 40), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    sdraw.rounded_rectangle([20, 20, scaled_w + 20, scaled_h + 20], radius=16, fill=(0, 0, 0, 180))
    shadow = shadow.filter(ImageFilter.GaussianBlur(15))
    
    gx = (W - scaled_w) // 2
    gy = (H - scaled_h) // 2
    canvas.paste(shadow, (gx - 20, gy - 20), shadow)
    
    gdraw = ImageDraw.Draw(canvas)
    gdraw.rounded_rectangle([gx - 2, gy - 2, gx + scaled_w + 2, gy + scaled_h + 2], radius=12, outline=C_EMERALD_BORDER, width=2)
    canvas.paste(gui_scaled, (gx, gy))
    
    out_path = os.path.join(OUTPUT_DIR, "screenshot_1_gallery.png")
    canvas.save(out_path, "PNG")
    print(f"Saved {out_path}")

# -------------------------------------------------------------
# 4. SCREENSHOT 2: SCHEMATIC DETAILS & CAROUSEL (1920x1080)
# -------------------------------------------------------------
def make_screenshot_details():
    print("Generating screenshot_2_schematic_details.png...")
    W, H = 1920, 1080
    im = Image.new("RGBA", (W, H), (7, 11, 20, 255))
    draw = ImageDraw.Draw(im)
    
    draw.rectangle([0, 0, W, 70], fill=C_HEADER_BG)
    draw.line([(0, 70), (W, 70)], fill=C_HEADER_BORDER, width=2)
    
    ava_src = Image.open(r"C:\LitematicaWebHub\public\avatar.png").convert("RGBA")
    ava = ava_src.resize((36, 36), Image.Resampling.LANCZOS)
    ava_m = Image.new("L", (36, 36), 0)
    ImageDraw.Draw(ava_m).rounded_rectangle([0, 0, 36, 36], radius=8, fill=255)
    im.paste(ava, (40, 17), ava_m)
    draw = ImageDraw.Draw(im)
    
    draw.text((90, 22), "Litematica Online Hub", fill=C_TEXT, font=FONT_HEADING)
    draw.text((W // 2 - 120, 22), "Schematic Details & Multi-Cover View", fill=C_EMERALD_LIGHT, font=FONT_HEADING)
    
    cw, ch = 1400, 820
    cx = (W - cw) // 2
    cy = 130
    
    draw.rounded_rectangle([cx, cy, cx + cw, cy + ch], radius=16, fill=C_CARD, outline=C_CARD_BORDER, width=2)
    
    cov_x = cx + 40
    cov_y = cy + 40
    cov_w = 760
    cov_h = 490
    draw.rounded_rectangle([cov_x, cov_y, cov_x + cov_w, cov_y + cov_h], radius=12, fill=C_COVER_BG, outline=C_CARD_BORDER, width=2)
    
    raw = Image.open(os.path.join(OUTPUT_DIR, "raw_ui_capture.png"))
    cov_raw = raw.crop((20, 85, 116, 149)).convert("RGBA")
    cov_scaled = cov_raw.resize((cov_w - 4, cov_h - 4), Image.Resampling.LANCZOS)
    im.paste(cov_scaled, (cov_x + 2, cov_y + 2))
    draw = ImageDraw.Draw(im)
    
    draw.rounded_rectangle([cov_x + 20, cov_y + cov_h // 2 - 25, cov_x + 60, cov_y + cov_h // 2 + 25], radius=8, fill=(11, 16, 29, 200), outline=C_CARD_BORDER)
    draw.text((cov_x + 35, cov_y + cov_h // 2 - 10), "<", fill=C_TEXT, font=FONT_HEADING)
    
    draw.rounded_rectangle([cov_x + cov_w - 60, cov_y + cov_h // 2 - 25, cov_x + cov_w - 20, cov_y + cov_h // 2 + 25], radius=8, fill=(11, 16, 29, 200), outline=C_CARD_BORDER)
    draw.text((cov_x + cov_w - 45, cov_y + cov_h // 2 - 10), ">", fill=C_TEXT, font=FONT_HEADING)
    
    dot_y = cov_y + cov_h - 30
    for idx, dot_x in enumerate([cov_x + cov_w // 2 - 24, cov_x + cov_w // 2, cov_x + cov_w // 2 + 24]):
        col = C_EMERALD if idx == 0 else C_TEXT_DIM
        draw.ellipse([dot_x - 5, dot_y - 5, dot_x + 5, dot_y + 5], fill=col)
        
    thumb_y = cov_y + cov_h + 24
    thumb_w = 230
    thumb_h = 150
    for idx in range(3):
        tx = cov_x + idx * (thumb_w + 35)
        border_col = C_EMERALD if idx == 0 else C_CARD_BORDER
        draw.rounded_rectangle([tx, thumb_y, tx + thumb_w, thumb_y + thumb_h], radius=8, fill=C_COVER_BG, outline=border_col, width=2)
        im.paste(cov_raw.resize((thumb_w - 4, thumb_h - 4), Image.Resampling.LANCZOS), (tx + 2, thumb_y + 2))
    draw = ImageDraw.Draw(im)
    
    rx = cx + cov_w + 80
    ry = cy + 50
    
    draw.text((rx, ry), "4 Bridge", fill=C_TEXT, font=FONT_TITLE)
    draw.text((rx, ry + 42), "Created by Freim4ik", fill=C_TEXT_MUTED, font=FONT_BODY)
    
    by = ry + 85
    draw.rounded_rectangle([rx, by, rx + 110, by + 30], radius=6, fill=(16, 185, 129, 40), outline=C_EMERALD, width=1)
    draw.text((rx + 16, by + 5), "MC 1.21.11", fill=C_EMERALD_LIGHT, font=FONT_BODY)
    
    draw.rounded_rectangle([rx + 125, by, rx + 225, by + 30], radius=6, fill=(30, 41, 59, 200), outline=C_CARD_BORDER, width=1)
    draw.text((rx + 145, by + 5), "Bridge", fill=C_TEXT, font=FONT_BODY)
    
    draw.text((rx + 245, by + 6), "⬇ 7 downloads   •   ♥ 0 likes", fill=C_TEXT_MUTED, font=FONT_BODY)
    
    desc_y = by + 60
    draw.text((rx, desc_y), "Description", fill=C_EMERALD_LIGHT, font=FONT_HEADING)
    desc_text = (
        "Красивый и функциональный арочный мост для переправы\n"
        "через реки и каньоны. Отлично подходит для средневековых\n"
        "и ванильных поселений. Полностью совместим со стандартным\n"
        "Litematica пастером."
    )
    draw.text((rx, desc_y + 35), desc_text, fill=C_TEXT, font=FONT_BODY, spacing=6)
    
    info_y = desc_y + 160
    draw.text((rx, info_y), "Technical Information", fill=C_TEXT_MUTED, font=FONT_BOLD)
    draw.text((rx, info_y + 28), "• Format: .litematic (Fabric Litematica)\n• Blocks: 482 (Wood, Stone Bricks, Lanterns)\n• Dimensions: 32 x 7 x 9 blocks\n• File size: 4.2 KB", fill=C_TEXT_DIM, font=FONT_BODY, spacing=4)
    
    btn_y = cy + ch - 120
    btn_w = 460
    btn_h = 60
    draw.rounded_rectangle([rx, btn_y, rx + btn_w, btn_y + btn_h], radius=12, fill=C_EMERALD, outline=C_EMERALD_LIGHT, width=2)
    draw.text((rx + 140, btn_y + 16), "⬇  Download Schematic", fill=(255, 255, 255), font=FONT_HEADING)
    
    out_path = os.path.join(OUTPUT_DIR, "screenshot_2_schematic_details.png")
    im.save(out_path, "PNG")
    print(f"Saved {out_path}")

# -------------------------------------------------------------
# 5. SCREENSHOT 3: IN-GAME PLACEMENT & CHAT (1920x1080)
# -------------------------------------------------------------
def make_screenshot_ingame():
    print("Generating screenshot_3_in_game_placement.png...")
    W, H = 1920, 1080
    im = Image.new("RGBA", (W, H), (15, 20, 30, 255))
    draw = ImageDraw.Draw(im)
    
    for y in range(H):
        t = y / float(H)
        r = int(12 + t * 24)
        g = int(22 + t * 45)
        b = int(40 + t * 25)
        draw.line([(0, y), (W, y)], fill=(r, g, b))
        
    cx, cy = W // 2, H // 2
    draw.line([(cx - 10, cy), (cx + 10, cy)], fill=(255, 255, 255, 200), width=2)
    draw.line([(cx, cy - 10), (cx, cy + 10)], fill=(255, 255, 255, 200), width=2)
    
    box_w, box_h = 640, 340
    bx = cx - box_w // 2
    by = cy - box_h // 2 + 50
    
    holo_col = (52, 211, 153, 160)
    for i in range(0, box_w, 40):
        draw.line([(bx + i, by), (bx + i, by + box_h)], fill=(52, 211, 153, 40), width=1)
    for j in range(0, box_h, 40):
        draw.line([(bx, by + j), (bx + box_w, by + j)], fill=(52, 211, 153, 40), width=1)
        
    draw.rounded_rectangle([bx, by, bx + box_w, by + box_h], radius=4, outline=holo_col, width=3)
    
    tag_t = "[Litematica] Loaded: 4 Bridge.litematic (Origin: 142, 64, -280)"
    draw.rounded_rectangle([bx + 20, by - 32, bx + 540, by - 6], radius=4, fill=(0, 0, 0, 180), outline=C_EMERALD, width=1)
    draw.text((bx + 30, by - 28), tag_t, fill=C_EMERALD_LIGHT, font=FONT_MONO)
    
    chat_y = H - 180
    chat_x = 40
    chat_w = 640
    chat_h = 100
    
    draw.rounded_rectangle([chat_x, chat_y, chat_x + chat_w, chat_y + chat_h], radius=6, fill=(0, 0, 0, 160))
    
    draw.text((chat_x + 16, chat_y + 14), "<Player> Connected to world", fill=(200, 200, 200), font=FONT_CHAT)
    draw.text((chat_x + 16, chat_y + 40), "[Litematica Hub] ", fill=(16, 185, 129), font=FONT_CHAT)
    draw.text((chat_x + 175, chat_y + 40), "Downloaded '4 Bridge.litematic' to schematics folder!", fill=(255, 255, 255), font=FONT_CHAT)
    draw.text((chat_x + 16, chat_y + 66), "[Litematica Hub] ", fill=(16, 185, 129), font=FONT_CHAT)
    draw.text((chat_x + 175, chat_y + 66), "Press 'M' -> Load Schematics to place it.", fill=(148, 163, 184), font=FONT_CHAT)
    
    hot_w = 400
    hot_h = 44
    hx = cx - hot_w // 2
    hy = H - 60
    draw.rounded_rectangle([hx, hy, hx + hot_w, hy + hot_h], radius=4, fill=(30, 30, 30, 200), outline=(80, 80, 80), width=2)
    for s in range(1, 9):
        draw.line([(hx + s * (hot_w // 9), hy), (hx + s * (hot_w // 9), hy + hot_h)], fill=(60, 60, 60))
        
    out_path = os.path.join(OUTPUT_DIR, "screenshot_3_in_game_placement.png")
    im.save(out_path, "PNG")
    print(f"Saved {out_path}")

# -------------------------------------------------------------
# 6. SCREENSHOT 4: LITEMATICA MENU INTEGRATION (1920x1080)
# -------------------------------------------------------------
def make_screenshot_menu():
    print("Generating screenshot_4_litematica_integration.png...")
    W, H = 1920, 1080
    im = Image.new("RGBA", (W, H), (0, 0, 0, 210))
    draw = ImageDraw.Draw(im)
    
    mw, mh = 800, 560
    mx = (W - mw) // 2
    my = (H - mh) // 2
    
    draw.rounded_rectangle([mx, my, mx + mw, my + mh], radius=14, fill=(15, 23, 42, 250), outline=C_CARD_BORDER, width=2)
    
    t = "Litematica Main Menu"
    bbox = draw.textbbox((0, 0), t, font=FONT_TITLE)
    draw.text((mx + (mw - (bbox[2] - bbox[0])) // 2, my + 30), t, fill=C_TEXT, font=FONT_TITLE)
    
    btn_w = 480
    btn_h = 48
    bx = mx + (mw - btn_w) // 2
    
    buttons = [
        ("Schematic Placements", False),
        ("Loaded Schematics", False),
        ("Area Selection", False),
        ("Schematic Manager", False),
        ("⭐ Online Hub Gallery (Litematica Online Hub)", True),
        ("Configuration GUI", False)
    ]
    
    by = my + 110
    for title, is_hub in buttons:
        if is_hub:
            draw.rounded_rectangle([bx, by, bx + btn_w, by + btn_h], radius=8, fill=(16, 185, 129, 60), outline=C_EMERALD, width=2)
            draw.text((bx + 35, by + 12), title, fill=C_EMERALD_LIGHT, font=FONT_HEADING)
        else:
            draw.rounded_rectangle([bx, by, bx + btn_w, by + btn_h], radius=8, fill=(20, 30, 48, 200), outline=(40, 54, 80), width=1)
            draw.text((bx + 35, by + 12), title, fill=C_TEXT_MUTED, font=FONT_HEADING)
        by += btn_h + 16
        
    out_path = os.path.join(OUTPUT_DIR, "screenshot_4_litematica_integration.png")
    im.save(out_path, "PNG")
    print(f"Saved {out_path}")

# -------------------------------------------------------------
# 7. ANIMATED GIF: DOWNLOAD SCHEMATIC (960x540)
# -------------------------------------------------------------
def make_download_gif():
    print("Generating download_schematic.gif...")
    GW, GH = 960, 540
    
    raw = Image.open(os.path.join(OUTPUT_DIR, "raw_ui_capture.png")).convert("RGBA")
    base_clean = raw.copy()
    b_draw = ImageDraw.Draw(base_clean)
    
    b_draw.rectangle([38, 5, 160, 32], fill=(11, 16, 29, 255))
    ava_src = Image.open(r"C:\LitematicaWebHub\public\avatar.png").convert("RGBA")
    ava24 = ava_src.resize((24, 24), Image.Resampling.LANCZOS)
    ava_m = Image.new("L", (24, 24), 0)
    ImageDraw.Draw(ava_m).rounded_rectangle([0, 0, 24, 24], radius=6, fill=255)
    base_clean.paste(ava24, (14, 5), ava_m)
    
    base_frame = base_clean.resize((GW, GH), Image.Resampling.LANCZOS)
    
    sx = GW / 1024.0
    sy = GH / 538.0
    
    bx1 = int(785 * sx)
    by1 = int(92 * sy)
    bx2 = int(895 * sx)
    by2 = int(120 * sy)
    
    def draw_cursor(d, x, y):
        pts = [(x, y), (x, y + 18), (x + 5, y + 14), (x + 10, y + 22), (x + 13, y + 21), (x + 8, y + 13), (x + 14, y + 13)]
        d.polygon(pts, fill=(255, 255, 255), outline=(0, 0, 0))

    frames = []
    
    start_x, start_y = GW // 2, GH // 2
    target_x, target_y = bx1 + (bx2 - bx1) // 2, by1 + (by2 - by1) // 2
    
    for i in range(12):
        t = i / 11.0
        e = 0.5 - 0.5 * math.cos(t * math.pi)
        cx = int(start_x + (target_x - start_x) * e)
        cy = int(start_y + (target_y - start_y) * e)
        
        f = base_frame.copy()
        fd = ImageDraw.Draw(f)
        
        if t > 0.8:
            fd.rounded_rectangle([bx1, by1, bx2, by2], radius=4, fill=(16, 185, 129, 60), outline=C_EMERALD_LIGHT, width=2)
            fd.text((bx1 + 14, by1 + 5), "Download", fill=C_EMERALD_LIGHT, font=FONT_SMALL)
            
        draw_cursor(fd, cx, cy)
        frames.append(f)
        
    for i in range(4):
        f = base_frame.copy()
        fd = ImageDraw.Draw(f)
        fd.rounded_rectangle([bx1, by1, bx2, by2], radius=4, fill=C_EMERALD_DARK, outline=C_EMERALD, width=2)
        fd.text((bx1 + 14, by1 + 6), "Download", fill=(255, 255, 255), font=FONT_SMALL)
        fd.circle((target_x, target_y), radius=8 + i * 4, outline=(52, 211, 153, 200 - i * 40), width=2)
        draw_cursor(fd, target_x, target_y)
        frames.append(f)
        
    bar_w = 400
    bar_h = 32
    bar_x = (GW - bar_w) // 2
    bar_y = GH - 75
    
    for i in range(16):
        pct = (i + 1) / 16.0
        f = base_frame.copy()
        fd = ImageDraw.Draw(f)
        
        fd.rounded_rectangle([bx1, by1, bx2, by2], radius=4, fill=(25, 36, 56), outline=C_CARD_BORDER, width=1)
        fd.text((bx1 + 18, by1 + 5), "Saving...", fill=C_EMERALD_LIGHT, font=FONT_SMALL)
        
        fd.rounded_rectangle([bar_x - 10, bar_y - 20, bar_x + bar_w + 10, bar_y + bar_h + 10], radius=8, fill=(9, 14, 26, 240), outline=C_EMERALD_BORDER, width=1)
        fd.text((bar_x, bar_y - 14), f"Downloading: 4 Bridge.litematic ({int(pct * 100)}%)", fill=C_TEXT, font=FONT_SMALL)
        
        fd.rounded_rectangle([bar_x, bar_y + 8, bar_x + bar_w, bar_y + 16], radius=4, fill=(20, 29, 45))
        fd.rounded_rectangle([bar_x, bar_y + 8, bar_x + int(bar_w * pct), bar_y + 16], radius=4, fill=C_EMERALD)
        
        draw_cursor(fd, target_x, target_y)
        frames.append(f)
        
    for i in range(20):
        f = base_frame.copy()
        fd = ImageDraw.Draw(f)
        
        fd.rounded_rectangle([bx1, by1, bx2, by2], radius=4, fill=(16, 53, 40), outline=C_EMERALD, width=2)
        fd.text((bx1 + 10, by1 + 5), "✓ Downloaded", fill=C_EMERALD_LIGHT, font=FONT_SMALL)
        
        fd.text((GW // 2 - 140, GH - 22), "Saved '4 Bridge.litematic' to schematics folder", fill=C_EMERALD_LIGHT, font=FONT_SMALL)
        
        toast_y = GH - 90
        toast_w = 460
        toast_x = 20
        fd.rounded_rectangle([toast_x, toast_y, toast_x + toast_w, toast_y + 36], radius=6, fill=(0, 0, 0, 200), outline=C_EMERALD, width=1)
        fd.text((toast_x + 12, toast_y + 9), "[Litematica Hub] Downloaded '4 Bridge.litematic'!", fill=(52, 211, 153), font=FONT_SMALL)
        
        draw_cursor(fd, target_x + i * 2, target_y + i)
        frames.append(f)
        
    out_path = os.path.join(OUTPUT_DIR, "download_schematic.gif")
    frames[0].save(
        out_path,
        save_all=True,
        append_images=frames[1:],
        duration=70,
        loop=0,
        optimize=True
    )
    print(f"Saved {out_path} ({len(frames)} frames)")

# -------------------------------------------------------------
# 8. MODRINTH DESCRIPTION & METADATA
# -------------------------------------------------------------
def make_descriptions():
    print("Writing Modrinth description & metadata...")
    
    summary_text = "Browse, search, and download schematics directly inside Minecraft from the Litematica Online Hub gallery without leaving the game."
    with open(os.path.join(OUTPUT_DIR, "summary.txt"), "w", encoding="utf-8") as f:
        f.write(summary_text)
        
    desc_md = """# 🌿 Litematica Online Hub

![Banner](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/banner.png)

<p align="center">
  <a href="https://litematica-online-hub.shop"><img src="https://img.shields.io/badge/Official_Website-litematica--online--hub.shop-10b981?style=for-the-badge&logo=google-chrome&logoColor=white" /></a>
  <a href="https://github.com/PonosDinozavr/litematica-online-hub"><img src="https://img.shields.io/badge/GitHub-Open_Source-24292e?style=for-the-badge&logo=github&logoColor=white" /></a>
  <a href="https://discord.gg/frogstudio"><img src="https://img.shields.io/badge/Frog_Studio-Community-5865F2?style=for-the-badge&logo=discord&logoColor=white" /></a>
</p>

---

**Litematica Online Hub** is an in-game gallery addon for the legendary **Litematica** mod on Fabric. It connects Minecraft directly to the community schematics database [litematica-online-hub.shop](https://litematica-online-hub.shop). 

Browse thousands of schematics, search by keywords, filter by categories and Minecraft versions, view screenshot carousels, and download builds directly into your `.minecraft/schematics/` folder with **one single click** — without ever Alt-Tabbing or leaving the game!

---

## ⚡ Instant 1-Click Download Demo

![Downloading Schematic Demo](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/download_schematic.gif)

---

## ✨ Features

- 🖼️ **In-Game Gallery Browser**: Modern, fluid, dark-emerald interface matching the web platform.
- ⚡ **1-Click Direct Download**: Schematics are automatically saved into your active Litematica folder and ready to paste.
- 🔍 **Live Search & Filter**: Instantly search by title, author, or keywords; filter by tags (Redstone, Farms, Castles, Bridges, Houses) and Minecraft versions.
- 📸 **Widescreen Screenshot Carousel**: Browse multiple angles and preview images per schematic right inside the menu.
- 💾 **Offline Page Cache**: Previously loaded pages remain accessible even if your internet connection drops.
- 🔔 **In-Game Update Alerts**: Notifies you when a new mod build is released with instant links.
- ⌨️ **Configurable Keybind**: Open the gallery at any time with a single keybind (default: `Open Online Gallery`).

---

## 📸 Screenshots

| In-Game Gallery | Detailed Card View |
| :---: | :---: |
| ![Gallery](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/screenshot_1_gallery.png) | ![Details](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/screenshot_2_schematic_details.png) |

| Holographic Placement | Litematica Menu Integration |
| :---: | :---: |
| ![In-Game Placement](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/screenshot_3_in_game_placement.png) | ![Menu](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/screenshot_4_litematica_integration.png) |

---

## 📋 Requirements & Compatibility

| Component | Required Version |
| :--- | :--- |
| **Minecraft** | `26.3`, `26.2`, `26.1.2`, `26.1.1`, `26.1`, `1.21.11` |
| **Mod Loader** | **Fabric Loader** `>=0.19.3` |
| **Dependencies** | **Fabric API**, **Litematica**, **MaLiLib** |
| **Java** | `Java 21+` |

---

## 🛠️ How to Install

1. Make sure you have **Fabric Loader** and **Fabric API** installed for your Minecraft version.
2. Ensure **Litematica** and **MaLiLib** are placed inside your `.minecraft/mods` folder.
3. Download the matching version of **Litematica Online Hub** from the Versions tab and drop it into `.minecraft/mods`.
4. Launch Minecraft, open the controls to check the keybind (or open via Litematica menu), and enjoy exploring schematics!

---

## 🎮 Default Controls

- **Open Gallery**: Go to `Options` -> `Controls` -> `Keybinds` -> category **Litematica Online Hub**.
- Alternatively, open Litematica Main Menu (default `M`) and click **Online Hub**.

---

## 📄 License & Credits

- Developed with ❤️ by **Frog Studio** & **ponos_dinozavr**.
- Source code is open source and licensed under the [MIT License](https://github.com/PonosDinozavr/litematica-online-hub/blob/main/LICENSE).
"""
    with open(os.path.join(OUTPUT_DIR, "description.md"), "w", encoding="utf-8") as f:
        f.write(desc_md)
        
    with open(os.path.join(OUTPUT_DIR, "README.md"), "w", encoding="utf-8") as f:
        f.write(desc_md)
        
    # Also create a Russian version: description_ru.md
    desc_ru = """# 🌿 Litematica Online Hub

![Баннер](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/banner.png)

<p align="center">
  <a href="https://litematica-online-hub.shop"><img src="https://img.shields.io/badge/Официальный_Сайт-litematica--online--hub.shop-10b981?style=for-the-badge&logo=google-chrome&logoColor=white" /></a>
  <a href="https://github.com/PonosDinozavr/litematica-online-hub"><img src="https://img.shields.io/badge/GitHub-Открытый_Исходный_Код-24292e?style=for-the-badge&logo=github&logoColor=white" /></a>
  <a href="https://discord.gg/frogstudio"><img src="https://img.shields.io/badge/Frog_Studio-Сообщество-5865F2?style=for-the-badge&logo=discord&logoColor=white" /></a>
</p>

---

**Litematica Online Hub** — внутриигровой аддон-галерея для мода **Litematica** на Fabric. Он напрямую подключает Minecraft к единой базе схематик [litematica-online-hub.shop](https://litematica-online-hub.shop).

Ищите лучшие постройки, фильтруйте по тегам и версиям игры, листайте скриншоты в карусели и скачивайте схематики напрямую в папку `.minecraft/schematics/` **в один клик** — без сворачивания игры и переходов в браузер!

---

## ⚡ Демонстрация скачивания в 1 клик

![Скачивание схематики](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/download_schematic.gif)

---

## ✨ Возможности

- 🖼️ **Внутриигровая галерея**: Стильный интерфейс в темных изумрудных тонах сайта (`dark-emerald`).
- ⚡ **Скачивание в 1 клик**: Файлы автоматически помещаются в папку со схематиками Litematica и сразу готовы к вставке.
- 🔍 **Поиск и фильтры**: Моментальный поиск по названиям, авторам и тегам (Редстоун, Фермы, Замки, Дома, Механизмы) с выбором версии игры.
- 📸 **Широкоформатная карусель скриншотов**: Просмотр нескольких ракурсов постройки прямо в карточке.
- 💾 **Офлайн-кеш страниц**: Ранее просмотренные схемы остаются доступны даже при перебоях с интернетом.
- 🔔 **Уведомления об обновлениях**: Мод сообщит при выходе новой версии прямо в игре.
- ⌨️ **Настраиваемая горячая клавиша**: Быстрый доступ к каталогу по клавише (по умолчанию: `Open Online Gallery`).

---

## 📸 Скриншоты

| Каталог в игре | Подробный вид карточки |
| :---: | :---: |
| ![Галерея](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/screenshot_1_gallery.png) | ![Карточка](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/screenshot_2_schematic_details.png) |

| Голограмма размещения | Меню Litematica |
| :---: | :---: |
| ![Размещение](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/screenshot_3_in_game_placement.png) | ![Меню](https://github.com/PonosDinozavr/litematica-online-hub/raw/main/modrinth_release/screenshot_4_litematica_integration.png) |

---

## 📋 Системные требования и совместимость

| Компонент | Требуемая версия |
| :--- | :--- |
| **Minecraft** | `26.3`, `26.2`, `26.1.2`, `26.1.1`, `26.1`, `1.21.11` |
| **Загрузчик модов** | **Fabric Loader** `>=0.19.3` |
| **Зависимости** | **Fabric API**, **Litematica**, **MaLiLib** |
| **Java** | `Java 21+` |

---

## 🛠️ Инструкция по установке

1. Убедитесь, что у вас установлены **Fabric Loader** и **Fabric API** для вашей версии Minecraft.
2. Поместите моды **Litematica** и **MaLiLib** в папку `.minecraft/mods`.
3. Скачайте соответствующий JAR-файл **Litematica Online Hub** из вкладки «Versions» и скопируйте в `.minecraft/mods`.
4. Запустите игру, настройте клавишу в управлении (или откройте через меню Litematica по кнопке `M`) и наслаждайтесь удобным каталогом!

---

## 📄 Лицензия

- Разработано командой **Frog Studio** & **ponos_dinozavr**.
- Открытый исходный код под лицензией [MIT License](https://github.com/PonosDinozavr/litematica-online-hub/blob/main/LICENSE).
"""
    with open(os.path.join(OUTPUT_DIR, "description_ru.md"), "w", encoding="utf-8") as f:
        f.write(desc_ru)

    print("Saved description.md, description_ru.md, and summary.txt")

if __name__ == "__main__":
    make_icon()
    make_banner()
    make_screenshot_gallery()
    make_screenshot_details()
    make_screenshot_ingame()
    make_screenshot_menu()
    make_download_gif()
    make_descriptions()
    print("All Modrinth assets generated successfully!")

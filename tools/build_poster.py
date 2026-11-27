# -*- coding: utf-8 -*-
"""生成「建议收集」v0.0.10 介绍图（版式参考用户给的「旅行足迹」竖版海报）。

用法：python tools/build_poster.py
输出：dist/poster-0.0.10.png（1680 x 2200）
"""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1680, 2200
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DIST = os.path.join(ROOT, "dist")

FONT_REG = r"C:\Windows\Fonts\msyh.ttc"
FONT_BOLD = r"C:\Windows\Fonts\msyhbd.ttc"

INK = (16, 24, 40)
INK_SOFT = (71, 84, 103)
INK_FAINT = (125, 135, 155)
BRAND = (59, 91, 246)
BRAND_DEEP = (40, 82, 235)
CHIP_BG = (233, 239, 255)
CARD = (255, 255, 255)
ICON_BG = (238, 243, 255)


def font(path, size):
    return ImageFont.truetype(path, size)


def put_text(d, xy, s, f, fill, anchor="la"):
    d.text(xy, s, font=f, fill=fill, anchor=anchor)


def wrap(d, s, f, max_w):
    lines, cur = [], ""
    for ch in s:
        trial = cur + ch
        if d.textlength(trial, font=f) <= max_w:
            cur = trial
        else:
            lines.append(cur)
            cur = ch
    if cur:
        lines.append(cur)
    return lines


def vgrad(size, top, bottom):
    w, h = size
    col = Image.new("RGB", (1, h))
    d = ImageDraw.Draw(col)
    for y in range(h):
        t = y / max(1, h - 1)
        d.point((0, y), tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)))
    return col.resize((w, h), Image.BILINEAR).convert("RGBA")


def gradient_tile(size, radius, c1, c2, ss=4):
    """圆角方块 + 斜向渐变，用 4 倍超采样换平滑边缘。"""
    w, h = size[0] * ss, size[1] * ss
    x = np.linspace(0, 1, w)[None, :]
    y = np.linspace(0, 1, h)[:, None]
    t = np.clip(x * 0.4 + y * 0.6, 0, 1)
    arr = np.zeros((h, w, 4), np.uint8)
    for i in range(3):
        arr[:, :, i] = (c1[i] + (c2[i] - c1[i]) * t).astype(np.uint8)
    arr[:, :, 3] = 255
    tile = Image.fromarray(arr, "RGBA")
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h - 1), radius=radius * ss, fill=255)
    tile.putalpha(mask)
    return tile.resize(size, Image.LANCZOS)


def shadow_layer(size, radius, blur=26, dy=16, alpha=44, color=(24, 40, 90)):
    w, h = size
    pad = blur * 2 + dy + 8
    layer = Image.new("RGBA", (w + pad * 2, h + pad * 2), (0, 0, 0, 0))
    sh = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle(
        (pad, pad + dy, pad + w, pad + h + dy), radius=radius, fill=color + (alpha,)
    )
    layer = Image.alpha_composite(layer, sh.filter(ImageFilter.GaussianBlur(blur)))
    return layer, pad


def phone(base, path, box, radius=44, frame=13, shadow=True):
    """把 1080x2400 的截图贴进白色手机框（box 是整机外框）。"""
    x0, y0, x1, y1 = box
    fw, fh = x1 - x0, y1 - y0
    if shadow:
        layer, pad = shadow_layer((fw, fh), radius)
        base.alpha_composite(layer, (x0 - pad, y0 - pad))

    d = ImageDraw.Draw(base)
    d.rounded_rectangle((x0, y0, x1, y1), radius=radius, fill=CARD, outline=(231, 236, 248), width=2)

    inner = (x0 + frame, y0 + frame, x1 - frame, y1 - frame)
    iw, ih = inner[2] - inner[0], inner[3] - inner[1]
    shot = Image.open(path).convert("RGB")
    scale = max(iw / shot.width, ih / shot.height)
    shot = shot.resize((round(shot.width * scale), round(shot.height * scale)), Image.LANCZOS)
    left = (shot.width - iw) // 2
    top = (shot.height - ih) // 2
    shot = shot.crop((left, top, left + iw, top + ih))

    mask = Image.new("L", (iw, ih), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, iw - 1, ih - 1), radius=max(6, radius - frame), fill=255)
    base.paste(shot, inner[:2], mask)


def feature_icon(base, kind, box):
    """功能小图标：浅蓝圆角底 + 深蓝线条。"""
    x0, y0, x1, y1 = box
    size = (x1 - x0, y1 - y0)
    ss = 4
    tile = Image.new("RGBA", (size[0] * ss, size[1] * ss), (0, 0, 0, 0))
    td = ImageDraw.Draw(tile)
    td.rounded_rectangle((0, 0, size[0] * ss - 1, size[1] * ss - 1), radius=18 * ss, fill=ICON_BG + (255,))
    cx, cy = size[0] * ss / 2, size[1] * ss / 2
    lw = 5 * ss

    if kind == "list":
        for dy in (-15, 0, 15):
            td.ellipse((cx - 16 * ss, (cy + dy * ss) - 4 * ss, cx - 8 * ss, (cy + dy * ss) + 4 * ss),
                       outline=BRAND_DEEP + (255,), width=3 * ss)
            td.line((cx + 0 * ss, cy + dy * ss, cx + 19 * ss, cy + dy * ss),
                    fill=BRAND_DEEP + (255,), width=lw)
    elif kind == "shield":
        pts = [(cx, cy - 21 * ss), (cx + 18 * ss, cy - 13 * ss), (cx + 18 * ss, cy + 4 * ss),
               (cx, cy + 21 * ss), (cx - 18 * ss, cy + 4 * ss), (cx - 18 * ss, cy - 13 * ss)]
        td.polygon(pts, outline=BRAND_DEEP + (255,), width=lw)
        td.line((cx - 8 * ss, cy + 1 * ss, cx - 1 * ss, cy + 8 * ss, cx + 10 * ss, cy - 7 * ss),
                fill=BRAND_DEEP + (255,), width=lw, joint="curve")
    else:  # photo
        td.rounded_rectangle((cx - 20 * ss, cy - 16 * ss, cx + 20 * ss, cy + 16 * ss),
                             radius=5 * ss, outline=BRAND_DEEP + (255,), width=lw)
        td.ellipse((cx - 13 * ss, cy - 9 * ss, cx - 5 * ss, cy - 1 * ss),
                   outline=BRAND_DEEP + (255,), width=4 * ss)
        td.line((cx - 16 * ss, cy + 11 * ss, cx - 2 * ss, cy - 2 * ss, cx + 7 * ss, cy + 7 * ss,
                 cx + 17 * ss, cy - 4 * ss), fill=BRAND_DEEP + (255,), width=lw, joint="curve")

    tile = tile.resize(size, Image.LANCZOS)
    base.alpha_composite(tile, (x0, y0))


def app_icon(base, box):
    """应用图标：蓝紫渐变圆角方块 + 白色对话气泡 + 三个琥珀点。"""
    x0, y0, x1, y1 = box
    size = (x1 - x0, y1 - y0)
    base.alpha_composite(gradient_tile(size, 40, (86, 106, 245), (124, 92, 255)), (x0, y0))

    ss = 4
    layer = Image.new("RGBA", (size[0] * ss, size[1] * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    w, h = layer.size
    d.rounded_rectangle((w * 0.22, h * 0.26, w * 0.78, h * 0.62), radius=int(h * 0.09),
                        fill=(255, 255, 255, 255))
    d.polygon([(w * 0.31, h * 0.60), (w * 0.31, h * 0.75), (w * 0.43, h * 0.62)],
              fill=(255, 255, 255, 255))
    for cx in (0.36, 0.50, 0.64):
        r = h * 0.032
        d.ellipse((w * cx - r, h * 0.44 - r, w * cx + r, h * 0.44 + r), fill=(255, 196, 74, 255))
    base.alpha_composite(layer.resize(size, Image.LANCZOS), (x0, y0))


def pill(d, box, label, f, fg, bg, radius=None):
    x0, y0, x1, y1 = box
    d.rounded_rectangle(box, radius=radius or (y1 - y0) // 2, fill=bg)
    put_text(d, ((x0 + x1) // 2, (y0 + y1) // 2), label, f, fg, anchor="mm")


def main():
    base = vgrad((W, H), (248, 250, 255), (226, 233, 250))

    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((820, 300, 1900, 1600), fill=(150, 176, 255, 66))
    base.alpha_composite(glow.filter(ImageFilter.GaussianBlur(130)))

    app_icon(base, (110, 116, 262, 268))

    d = ImageDraw.Draw(base)
    f_title = font(FONT_BOLD, 94)
    f_sub = font(FONT_REG, 33)
    f_chip = font(FONT_REG, 27)
    f_h = font(FONT_BOLD, 42)
    f_body = font(FONT_REG, 29)
    f_num = font(FONT_BOLD, 76)
    f_lab = font(FONT_REG, 28)
    f_flow = font(FONT_REG, 26)
    f_cap = font(FONT_REG, 24)
    f_foot = font(FONT_REG, 25)

    put_text(d, (110, 300), "建议收集", f_title, INK)
    put_text(d, (114, 424), "把想法写下来，会被逐条看到", f_sub, INK_SOFT)

    chip = "账号一套 · 多软件共用"
    cw = d.textlength(chip, font=f_chip)
    pill(d, (112, 496, 112 + cw + 56, 552), chip, f_chip, BRAND_DEEP, CHIP_BG)
    put_text(d, (112 + cw + 88, 525), "v0.0.10", f_chip, INK_FAINT, anchor="lm")

    features = [
        ("list", "逐条有交代",
         "接纳、驳回（要写理由）、解决、推送版本号，每一步都留痕，谁提的也一眼看得到。"),
        ("shield", "按项目开权限",
         "在「成员权限」里勾谁，谁才能进这个项目看和提；没勾的人连列表都打不开。"),
        ("photo", "带图、备注、自动更新",
         "每条建议最多 3 张图；管理员可写只有自己看得到的备注；打开软件自动查新版本。"),
    ]
    y = 642
    for kind, head, body in features:
        feature_icon(base, kind, (112, y + 2, 184, y + 74))
        put_text(d, (214, y), head, f_h, INK)
        yy = y + 62
        for ln in wrap(d, body, f_body, 690):
            put_text(d, (214, yy), ln, f_body, INK_SOFT)
            yy += 45
        y = yy + 74

    # 三个数字
    stats = [("400 字", "驳回理由上限"), ("3 张", "每条建议配图"), ("5 种", "建议状态流转")]
    xs = [112, 452, 792]
    for i, (num, lab) in enumerate(stats):
        x = xs[i]
        put_text(d, (x, 1428), num, f_num, BRAND_DEEP)
        put_text(d, (x + 3, 1536), lab, f_lab, INK_SOFT)
        if i:
            d.line((x - 36, 1436, x - 36, 1556), fill=(204, 212, 233), width=2)

    # 状态流转条
    put_text(d, (112, 1626), "一条建议的一生", f_lab, INK)
    flow = ["待处理", "已接纳", "已解决", "已推送"]
    fx = 112
    fy = 1680
    for i, label in enumerate(flow):
        wpx = int(d.textlength(label, font=f_flow)) + 54
        pill(d, (fx, fy, fx + wpx, fy + 58), label, f_flow, BRAND_DEEP, (236, 241, 255))
        fx += wpx
        if i < len(flow) - 1:
            put_text(d, (fx + 18, fy + 28), "→", f_flow, (150, 160, 180), anchor="lm")
            fx += 60
    put_text(d, (112, 1760), "驳回的要写理由（400 字以内），也归到「已解决」，随时能退回待办。",
             font(FONT_REG, 24), INK_FAINT)

    put_text(d, (112, 2078), "安卓 8.0 及以上 · 覆盖安装不丢数据 · http://39.96.113.178:8080/", f_foot, INK_FAINT)

    # 主截图：管理台「已解决」列表（带备注）
    phone(base, os.path.join(DIST, "note-card-0.0.10.png"), (1002, 132, 1566, 1352))

    # 三张小图
    thumbs = [
        (os.path.join(DIST, "user-card-0.0.9.png"), "我的反馈标签"),
        (os.path.join(DIST, "editor-image-0.0.9.png"), "提建议带图"),
        (os.path.join(DIST, "member-note-0.0.10.png"), "权限 + 私密备注"),
    ]
    tw = 172
    gap = 24
    for i, (path, cap) in enumerate(thumbs):
        tx = 1002 + i * (tw + gap)
        th = round(tw * 2400 / 1080) + 20
        phone(base, path, (tx, 1440, tx + tw, 1440 + th), radius=28, frame=9)
        put_text(d, (tx + tw // 2, 1440 + th + 40), cap, f_cap, INK_FAINT, anchor="mm")

    out = os.path.join(DIST, "poster-0.0.10.png")
    base.convert("RGB").save(out, quality=95)
    print("saved", out, base.size)


if __name__ == "__main__":
    main()

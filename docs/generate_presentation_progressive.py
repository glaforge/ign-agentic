#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generator for the ALTERNATIVE (Progressive / Interleaved Demos) IGN Workshop PowerPoint (.pptx):
"Développement Agentique & Systèmes Multi-Agents Géospatiaux"

PROGRESSIVE STRUCTURE (Each technology is immediately followed by its IGN Geospatial Demo):
- Ouverture & Agenda
- Partie 1 : Du LLM à l'Agent IA & Standards (@Tool, SKILL.md, MCP)
- Partie 2 : Socle Géospatial IGN (data.geopf.fr), Démo Prélude (_00) & Cockpit 3D Temps Réel
- Partie 3 : Google Antigravity, Antigravity SDK for Java & Démo Acte 3 (Détective Multimodal _03)
- Partie 4 : LangChain4j (AiServices, RAG ONNX) & Démo Acte 1 (Copilote Urbanisme & ZAN _01)
- Partie 5 : Google ADK for Java (Multi-Agents) & Démo Acte 2 (Cellule de Crise _02)
- Partie 6 : Serveurs MCP, Souveraineté (LiteRT-LM / Gemma 4) & Démo Acte 4 (Double MCP _04)
- Synthèse Comparative & Ressources
"""

import os
import zipfile
from PIL import Image
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.oxml.ns import qn
from pptx.oxml import parse_xml

# ==============================================================================
# 1. FIXED TYPOGRAPHY FAMILIES & SCALE (Strict Design System)
# ==============================================================================
FONT_TITLE = "Google Sans Flex Rounding"
FONT_BODY  = "Google Sans Flex Normal"
FONT_MONO  = "Google Sans Code"

SZ_SECTION_TITLE  = Pt(36)
SZ_SLIDE_TITLE    = Pt(26)
SZ_SLIDE_SUBTITLE = Pt(15)
SZ_CARD_TITLE     = Pt(17)
SZ_BODY           = Pt(15)
SZ_DIAGRAM_TITLE  = Pt(13.5)
SZ_DIAGRAM_SUB    = Pt(11.5)
SZ_CODE           = Pt(11.5)
SZ_PILL           = Pt(9.5)
SZ_CAPTION        = Pt(10.5)

# ==============================================================================
# 2. OFFICIAL GOOGLE 500 COLOR PALETTE (True Blue, Red, Yellow, Green)
# ==============================================================================
G_BLUE_500   = RGBColor(0x42, 0x85, 0xF4)  # #4285F4 - Google Blue 500
G_RED_500    = RGBColor(0xEA, 0x43, 0x35)  # #EA4335 - Google Red 500
G_YELLOW_500 = RGBColor(0xFB, 0xBC, 0x04)  # #FBBC04 - Google Yellow 500
G_GREEN_500  = RGBColor(0x34, 0xA8, 0x53)  # #34A853 - Google Green 500

# Soft pastel tints for diagram node fills (Google 50 shades)
G_BLUE_TINT   = RGBColor(0xE8, 0xF0, 0xFE)  # #E8F0FE
G_RED_TINT    = RGBColor(0xFC, 0xE8, 0xE6)  # #FCE8E6
G_YELLOW_TINT = RGBColor(0xFE, 0xF7, 0xE0)  # #FEF7E0
G_GREEN_TINT  = RGBColor(0xE6, 0xF4, 0xEA)  # #E6F4EA

# High-contrast text shades for inline bold labels on white cards (Google 700/800)
G_BLUE_TEXT   = RGBColor(0x19, 0x67, 0xD2)  # #1967D2
G_RED_TEXT    = RGBColor(0xC5, 0x22, 0x1F)  # #C5221F
G_YELLOW_TEXT = RGBColor(0xB0, 0x60, 0x00)  # #B06000
G_GREEN_TEXT  = RGBColor(0x13, 0x73, 0x33)  # #137333

TEXT_ACCENT_MAP = {
    G_BLUE_500:   G_BLUE_TEXT,
    G_RED_500:    G_RED_TEXT,
    G_YELLOW_500: G_YELLOW_TEXT,
    G_GREEN_500:  G_GREEN_TEXT,
}

PILL_TEXT_MAP = {
    G_BLUE_500:   RGBColor(255, 255, 255),
    G_RED_500:    RGBColor(255, 255, 255),
    G_GREEN_500:  RGBColor(255, 255, 255),
    G_YELLOW_500: RGBColor(32, 33, 36),
}

# Base Google Neutrals
C_NAVY       = RGBColor(0x20, 0x21, 0x24)  # #202124 - Google Grey 900
C_DARK_CARD  = RGBColor(0x28, 0x2A, 0x2E)  # #282A2E - Dark surface for code
C_TEXT_DARK  = RGBColor(0x20, 0x21, 0x24)  # #202124 - Primary body text
C_TEXT_MUTED = RGBColor(0x5F, 0x63, 0x68)  # #5F6368 - Google Grey 700
C_TEXT_LIGHT = RGBColor(0xF8, 0xF9, 0xFA)  # #F8F9FA - Google Grey 50
C_BG_LIGHT   = RGBColor(0xF8, 0xF9, 0xFA)  # #F8F9FA - Slide Background
C_WHITE      = RGBColor(0xFF, 0xFF, 0xFF)  # #FFFFFF - Card Surface
C_BORDER     = RGBColor(0xDA, 0xDC, 0xE0)  # #DADCE0 - Google Grey 300

# Code Syntax Highlighting (Google 300 shades on dark background)
C_CODE_KW    = RGBColor(0x8A, 0xB4, 0xF8)  # Google Blue 300
C_CODE_STR   = RGBColor(0x81, 0xC9, 0x95)  # Google Green 300
C_CODE_ANN   = RGBColor(0xFD, 0xD6, 0x63)  # Google Yellow 300
C_CODE_ALERT = RGBColor(0xF2, 0x8B, 0x82)  # Google Red 300
C_CODE_CMT   = RGBColor(0x9A, 0xA0, 0xA6)  # Google Grey 500
C_CODE_TXT   = RGBColor(0xF1, 0xF3, 0xF4)  # Google Grey 100

# ==============================================================================
# 3. FIXED GEOMETRY & GENEROUS SPACING (16:9 Widescreen: 13.333 x 7.5 in)
# ==============================================================================
SLIDE_W   = Inches(13.333)
SLIDE_H   = Inches(7.5)
MARGIN_X  = Inches(0.6)
CONTENT_W = Inches(12.133)

PILL_TOP     = Inches(0.28)
TITLE_TOP    = Inches(0.68)
SUBTITLE_TOP = Inches(1.24)
BODY_TOP     = Inches(2.15)
BODY_H       = Inches(4.50)


def set_font_family(run, font_name, paragraph=None, is_mono=False):
    """
    Set font family on a:latin, a:ea, and a:cs (and paragraph a:defRPr if provided)
    with explicit pitchFamily="49" (Fixed-pitch / Modern monospace) for code runs
    so Google Slides always preserves the monospace font on PPTX import.
    """
    run.font.name = font_name
    pitch_attr = ' pitchFamily="49" charset="0"' if is_mono else ""
    rPr = run._r.get_or_add_rPr()
    for tag in ("a:latin", "a:ea", "a:cs"):
        el = rPr.find(qn(tag))
        if el is None:
            el = parse_xml(f'<{tag} xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" typeface="{font_name}"{pitch_attr}/>')
            rPr.append(el)
        else:
            el.set("typeface", font_name)
            if is_mono:
                el.set("pitchFamily", "49")
                el.set("charset", "0")

    if paragraph is not None:
        pPr = paragraph._p.get_or_add_pPr()
        defRPr = pPr.find(qn("a:defRPr"))
        if defRPr is None:
            defRPr = parse_xml(
                f'<a:defRPr xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">'
                f'<a:latin typeface="{font_name}"{pitch_attr}/>'
                f'<a:ea typeface="{font_name}"{pitch_attr}/>'
                f'<a:cs typeface="{font_name}"{pitch_attr}/>'
                f'</a:defRPr>'
            )
            pPr.append(defRPr)


class DeckBuilder:
    def __init__(self, img_dir):
        self.prs = Presentation()
        self.prs.slide_width = SLIDE_W
        self.prs.slide_height = SLIDE_H
        self.blank_layout = self.prs.slide_layouts[6]
        self.img_dir = img_dir
        self.slide_num = 0

    def new_slide(self, section_tag, title, subtitle, section_color=G_BLUE_500):
        slide = self.prs.slides.add_slide(self.blank_layout)
        self.slide_num += 1

        bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, SLIDE_W, SLIDE_H)
        bg.fill.solid()
        bg.fill.fore_color.rgb = C_BG_LIGHT
        bg.line.fill.background()

        for idx, col in enumerate([G_BLUE_500, G_RED_500, G_YELLOW_500, G_GREEN_500]):
            seg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(idx * 3.333), 0, Inches(3.334), Inches(0.09))
            seg.fill.solid()
            seg.fill.fore_color.rgb = col
            seg.line.fill.background()

        self.add_pill(slide, MARGIN_X, PILL_TOP, section_tag, bg_color=section_color)

        tb_t = slide.shapes.add_textbox(MARGIN_X, TITLE_TOP, CONTENT_W, Inches(0.52))
        tf_t = tb_t.text_frame
        tf_t.word_wrap = True
        tf_t.margin_left = tf_t.margin_right = tf_t.margin_top = tf_t.margin_bottom = 0
        r_t = tf_t.paragraphs[0].add_run()
        r_t.text = title
        set_font_family(r_t, FONT_TITLE)
        r_t.font.size = SZ_SLIDE_TITLE
        r_t.font.bold = True
        r_t.font.color.rgb = C_NAVY

        tb_s = slide.shapes.add_textbox(MARGIN_X, SUBTITLE_TOP, CONTENT_W, Inches(0.45))
        tf_s = tb_s.text_frame
        tf_s.word_wrap = True
        tf_s.margin_left = tf_s.margin_right = tf_s.margin_top = tf_s.margin_bottom = 0
        r_s = tf_s.paragraphs[0].add_run()
        r_s.text = subtitle
        set_font_family(r_s, FONT_BODY)
        r_s.font.size = SZ_SLIDE_SUBTITLE
        r_s.font.color.rgb = C_TEXT_MUTED

        h_line = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, MARGIN_X, Inches(1.85), Inches(1.6), Inches(0.04))
        h_line.fill.solid()
        h_line.fill.fore_color.rgb = section_color
        h_line.line.fill.background()

        f_line = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, MARGIN_X, Inches(6.95), CONTENT_W, Inches(0.015))
        f_line.fill.solid()
        f_line.fill.fore_color.rgb = C_BORDER
        f_line.line.fill.background()

        self.add_caption(slide, MARGIN_X, Inches(7.02), Inches(9.5), Inches(0.35),
                         "Workshop IGN — Systèmes Multi-Agents Géospatiaux  •  Guillaume Laforge (@glaforge)")
        self.add_caption(slide, Inches(11.2), Inches(7.02), Inches(1.533), Inches(0.35),
                         f"{self.slide_num}", align=PP_ALIGN.RIGHT, bold=True)
        return slide

    def add_section_divider(self, part_tag, title, subtitle, topics, accent_color=G_BLUE_500):
        slide = self.prs.slides.add_slide(self.blank_layout)
        self.slide_num += 1

        bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, SLIDE_W, SLIDE_H)
        bg.fill.solid()
        bg.fill.fore_color.rgb = C_NAVY
        bg.line.fill.background()

        for idx, col in enumerate([G_BLUE_500, G_RED_500, G_YELLOW_500, G_GREEN_500]):
            seg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(idx * 3.333), 0, Inches(3.334), Inches(0.10))
            seg.fill.solid()
            seg.fill.fore_color.rgb = col
            seg.line.fill.background()

        stripe = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, Inches(0.10), Inches(0.22), SLIDE_H - Inches(0.10))
        stripe.fill.solid()
        stripe.fill.fore_color.rgb = accent_color
        stripe.line.fill.background()

        self.add_pill(slide, Inches(0.8), Inches(1.25), part_tag, bg_color=accent_color)

        tb_t = slide.shapes.add_textbox(Inches(0.8), Inches(1.85), Inches(6.8), Inches(2.1))
        tf_t = tb_t.text_frame
        tf_t.word_wrap = True
        r_t = tf_t.paragraphs[0].add_run()
        r_t.text = title
        set_font_family(r_t, FONT_TITLE)
        r_t.font.size = SZ_SECTION_TITLE
        r_t.font.bold = True
        r_t.font.color.rgb = C_WHITE

        tb_s = slide.shapes.add_textbox(Inches(0.8), Inches(4.25), Inches(6.6), Inches(1.6))
        tf_s = tb_s.text_frame
        tf_s.word_wrap = True
        r_s = tf_s.paragraphs[0].add_run()
        r_s.text = subtitle
        set_font_family(r_s, FONT_BODY)
        r_s.font.size = SZ_SLIDE_SUBTITLE
        r_s.font.color.rgb = RGBColor(218, 220, 224)

        panel = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(7.9), Inches(1.25), Inches(4.8), Inches(5.0))
        panel.adjustments[0] = 0.04
        panel.fill.solid()
        panel.fill.fore_color.rgb = C_DARK_CARD
        panel.line.color.rgb = accent_color
        panel.line.width = Pt(1.8)

        tb_p = slide.shapes.add_textbox(Inches(8.25), Inches(1.55), Inches(4.1), Inches(0.5))
        r_p = tb_p.text_frame.paragraphs[0].add_run()
        r_p.text = "Dans cette section :"
        set_font_family(r_p, FONT_TITLE)
        r_p.font.size = SZ_CARD_TITLE
        r_p.font.bold = True
        r_p.font.color.rgb = G_YELLOW_500

        tb_l = slide.shapes.add_textbox(Inches(8.25), Inches(2.35), Inches(4.1), Inches(3.6))
        tf_l = tb_l.text_frame
        tf_l.word_wrap = True
        for i, item in enumerate(topics):
            p = tf_l.paragraphs[0] if i == 0 else tf_l.add_paragraph()
            p.space_after = Pt(14)
            r = p.add_run()
            r.text = f"▸  {item}"
            set_font_family(r, FONT_BODY)
            r.font.size = SZ_BODY
            r.font.color.rgb = C_WHITE
        return slide

    def add_pill(self, slide, left, top, text, bg_color=G_BLUE_500):
        w = Inches(max(1.6, len(text) * 0.11 + 0.5))
        h = Inches(0.30)
        pill = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, w, h)
        pill.adjustments[0] = 0.5
        pill.fill.solid()
        pill.fill.fore_color.rgb = bg_color
        pill.line.fill.background()
        tf = pill.text_frame
        tf.word_wrap = False
        tf.vertical_anchor = MSO_ANCHOR.MIDDLE
        tf.margin_left = tf.margin_right = Inches(0.12)
        tf.margin_top = tf.margin_bottom = Inches(0.02)
        p = tf.paragraphs[0]
        p.alignment = PP_ALIGN.CENTER
        r = p.add_run()
        r.text = text.upper()
        set_font_family(r, FONT_TITLE)
        r.font.size = SZ_PILL
        r.font.bold = True
        r.font.color.rgb = PILL_TEXT_MAP.get(bg_color, C_WHITE)
        return pill

    def add_caption(self, slide, left, top, width, height, text, align=PP_ALIGN.LEFT, bold=False, italic=False, color=C_TEXT_MUTED, font_name=FONT_BODY):
        tb = slide.shapes.add_textbox(left, top, width, height)
        tf = tb.text_frame
        tf.word_wrap = True
        tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
        p = tf.paragraphs[0]
        p.alignment = align
        r = p.add_run()
        r.text = text
        set_font_family(r, font_name, paragraph=p, is_mono=(font_name == FONT_MONO))
        r.font.size = SZ_CAPTION
        r.font.bold = bold
        r.font.italic = italic
        r.font.color.rgb = color
        return tb

    def add_card(self, slide, left, top, width, height, title=None, bullets=None,
                 accent_color=G_BLUE_500, bg_color=C_WHITE, badge=None):
        card = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
        card.adjustments[0] = 0.03
        card.fill.solid()
        card.fill.fore_color.rgb = bg_color
        card.line.color.rgb = C_BORDER if bg_color == C_WHITE else accent_color
        card.line.width = Pt(1.5)

        stripe = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, left, top + Inches(0.12), Inches(0.10), height - Inches(0.24))
        stripe.fill.solid()
        stripe.fill.fore_color.rgb = accent_color
        stripe.line.fill.background()

        c_left = left + Inches(0.32)
        c_width = width - Inches(0.52)
        curr_y = top + Inches(0.24)

        is_dark = (bg_color == C_DARK_CARD or bg_color == C_NAVY)
        title_col = C_WHITE if is_dark else C_NAVY
        body_col = C_TEXT_LIGHT if is_dark else C_TEXT_DARK
        prefix_col = G_YELLOW_500 if is_dark else TEXT_ACCENT_MAP.get(accent_color, G_BLUE_TEXT)

        if badge:
            self.add_pill(slide, c_left, curr_y, badge, bg_color=accent_color)
            curr_y += Inches(0.46)

        avail_h = height - (curr_y - top) - Inches(0.20)
        tb = slide.shapes.add_textbox(c_left, curr_y, c_width, avail_h)
        tf = tb.text_frame
        tf.word_wrap = True
        tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0

        has_first_para = False
        if title:
            p_t = tf.paragraphs[0]
            has_first_para = True
            p_t.space_after = Pt(14)
            r_t = p_t.add_run()
            r_t.text = title
            set_font_family(r_t, FONT_TITLE, paragraph=p_t)
            r_t.font.size = SZ_CARD_TITLE
            r_t.font.bold = True
            r_t.font.color.rgb = title_col

        if bullets:
            for idx, item in enumerate(bullets):
                p = tf.paragraphs[0] if (idx == 0 and not has_first_para) else tf.add_paragraph()
                p.space_after = Pt(10)
                if isinstance(item, tuple):
                    b_head, b_rest = item
                    r1 = p.add_run()
                    r1.text = "• " + b_head
                    set_font_family(r1, FONT_BODY, paragraph=p)
                    r1.font.size = SZ_BODY
                    r1.font.bold = True
                    r1.font.color.rgb = prefix_col

                    r2 = p.add_run()
                    r2.text = b_rest
                    set_font_family(r2, FONT_BODY, paragraph=p)
                    r2.font.size = SZ_BODY
                    r2.font.color.rgb = body_col
                else:
                    r = p.add_run()
                    r.text = "• " + item
                    set_font_family(r, FONT_BODY, paragraph=p)
                    r.font.size = SZ_BODY
                    r.font.color.rgb = body_col
        return card

    def add_code_box(self, slide, left, top, width, height, header_title, code_lines):
        box = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
        box.adjustments[0] = 0.025
        box.fill.solid()
        box.fill.fore_color.rgb = C_DARK_CARD
        box.line.color.rgb = C_BORDER
        box.line.width = Pt(1.2)

        hdr_h = Inches(0.38)
        hdr = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, left, top, width, hdr_h)
        hdr.fill.solid()
        hdr.fill.fore_color.rgb = C_NAVY
        hdr.line.fill.background()

        for i, col in enumerate([G_RED_500, G_YELLOW_500, G_GREEN_500]):
            dot = slide.shapes.add_shape(MSO_SHAPE.OVAL, left + Inches(0.16 + i * 0.2), top + Inches(0.13), Inches(0.12), Inches(0.12))
            dot.fill.solid()
            dot.fill.fore_color.rgb = col
            dot.line.fill.background()

        self.add_caption(slide, left + Inches(0.8), top + Inches(0.09), width - Inches(0.9), Inches(0.24),
                         header_title, bold=True, color=C_CODE_KW, font_name=FONT_MONO)

        tb = slide.shapes.add_textbox(left + Inches(0.22), top + hdr_h + Inches(0.14),
                                     width - Inches(0.44), height - hdr_h - Inches(0.24))
        tf = tb.text_frame
        tf.word_wrap = True
        tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0

        lstStyle = tb._element.txBody.find(qn("a:lstStyle"))
        if lstStyle is not None:
            lvl1pPr = parse_xml(
                f'<a:lvl1pPr xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">'
                f'<a:defRPr sz="1150">'
                f'<a:latin typeface="{FONT_MONO}" pitchFamily="49" charset="0"/>'
                f'<a:ea typeface="{FONT_MONO}" pitchFamily="49" charset="0"/>'
                f'<a:cs typeface="{FONT_MONO}" pitchFamily="49" charset="0"/>'
                f'</a:defRPr>'
                f'</a:lvl1pPr>'
            )
            lstStyle.append(lvl1pPr)

        for idx, line in enumerate(code_lines):
            p = tf.paragraphs[0] if idx == 0 else tf.add_paragraph()
            p.space_after = Pt(2.5)
            r = p.add_run()
            r.text = line
            set_font_family(r, FONT_MONO, paragraph=p, is_mono=True)
            r.font.size = SZ_CODE
            s_line = line.strip()
            if s_line.startswith("//") or s_line.startswith("#"):
                r.font.color.rgb = C_CODE_CMT
                r.font.italic = True
            elif s_line.startswith("@"):
                r.font.color.rgb = C_CODE_ANN
                r.font.bold = True
            elif any(s_line.startswith(k) for k in ("public ", "record ", "interface ", "class ", "try ", "return ")):
                r.font.color.rgb = C_CODE_KW
            elif "✓" in s_line or "✅" in s_line:
                r.font.color.rgb = C_CODE_STR
            elif "❌" in s_line or "⚠️" in s_line or "🚨" in s_line:
                r.font.color.rgb = C_CODE_ALERT
            else:
                r.font.color.rgb = C_CODE_TXT
        return box

    def add_diagram_node(self, slide, left, top, width, height, title, subtitle=None,
                         bg_color=C_WHITE, border_color=G_BLUE_500):
        node = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
        node.adjustments[0] = 0.08
        node.fill.solid()
        node.fill.fore_color.rgb = bg_color
        node.line.color.rgb = border_color
        node.line.width = Pt(2.2)

        tf = node.text_frame
        tf.word_wrap = True
        tf.vertical_anchor = MSO_ANCHOR.MIDDLE
        tf.margin_left = tf.margin_right = Inches(0.12)
        tf.margin_top = tf.margin_bottom = Inches(0.08)

        p = tf.paragraphs[0]
        p.alignment = PP_ALIGN.CENTER
        r = p.add_run()
        r.text = title
        set_font_family(r, FONT_TITLE)
        r.font.size = SZ_DIAGRAM_TITLE
        r.font.bold = True
        r.font.color.rgb = C_NAVY

        if subtitle:
            p2 = tf.add_paragraph()
            p2.alignment = PP_ALIGN.CENTER
            p2.space_before = Pt(5)
            r2 = p2.add_run()
            r2.text = subtitle
            set_font_family(r2, FONT_BODY)
            r2.font.size = SZ_DIAGRAM_SUB
            r2.font.color.rgb = C_TEXT_MUTED
        return node

    def add_arrow(self, slide, left, top, width, height, direction="right", color=G_BLUE_500):
        shape_map = {
            "right": MSO_SHAPE.RIGHT_ARROW,
            "left": MSO_SHAPE.LEFT_ARROW,
            "down": MSO_SHAPE.DOWN_ARROW,
            "up": MSO_SHAPE.UP_ARROW,
            "both": MSO_SHAPE.LEFT_RIGHT_ARROW,
        }
        arr = slide.shapes.add_shape(shape_map[direction], left, top, width, height)
        arr.fill.solid()
        arr.fill.fore_color.rgb = color
        arr.line.fill.background()
        return arr

    def add_fitted_image(self, slide, img_filename, box_left, box_top, box_w, box_h, caption=None):
        img_path = os.path.join(self.img_dir, img_filename)
        with Image.open(img_path) as im:
            w_px, h_px = im.size
        aspect = w_px / float(h_px)

        avail_h = box_h - (Inches(0.36) if caption else 0)
        box_aspect = float(box_w) / float(avail_h)

        if aspect > box_aspect:
            final_w = box_w
            final_h = int(box_w / aspect)
            final_left = box_left
            final_top = box_top + int((avail_h - final_h) / 2)
        else:
            final_h = avail_h
            final_w = int(avail_h * aspect)
            final_top = box_top
            final_left = box_left + int((box_w - final_w) / 2)

        frame = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE,
                                       final_left - Inches(0.04), final_top - Inches(0.04),
                                       final_w + Inches(0.08), final_h + Inches(0.08))
        frame.fill.solid()
        frame.fill.fore_color.rgb = C_WHITE
        frame.line.color.rgb = C_BORDER
        frame.line.width = Pt(1.5)

        slide.shapes.add_picture(img_path, final_left, final_top, width=final_w, height=final_h)

        if caption:
            self.add_caption(slide, box_left, final_top + final_h + Inches(0.08), box_w, Inches(0.3),
                             caption, align=PP_ALIGN.CENTER, italic=True)

    def layout_two_cards(self, slide, left_card, right_card):
        w = Inches(5.9)
        self.add_card(slide, MARGIN_X, BODY_TOP, w, BODY_H, **left_card)
        self.add_card(slide, MARGIN_X + w + Inches(0.333), BODY_TOP, w, BODY_H, **right_card)

    def layout_three_cards(self, slide, cards):
        w = Inches(3.84)
        gap = Inches(0.306)
        for i, c in enumerate(cards):
            self.add_card(slide, MARGIN_X + i * (w + gap), BODY_TOP, w, BODY_H, **c)

    def layout_four_columns(self, slide, cards):
        w = Inches(2.84)
        gap = Inches(0.257)
        for i, c in enumerate(cards):
            self.add_card(slide, MARGIN_X + i * (w + gap), BODY_TOP, w, BODY_H, **c)


# ==============================================================================
# BUILD THE PROGRESSIVE PRESENTATION (Demos Interleaved After Each Technology)
# ==============================================================================
def build_presentation():
    img_dir = "/Users/glaforge/Projects/IGN/docs/images"
    b = DeckBuilder(img_dir)

    # --------------------------------------------------------------------------
    # SLIDE 1 : COVER SLIDE
    # --------------------------------------------------------------------------
    s = b.prs.slides.add_slide(b.blank_layout)
    b.slide_num += 1
    bg = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, SLIDE_W, SLIDE_H)
    bg.fill.solid()
    bg.fill.fore_color.rgb = C_NAVY
    bg.line.fill.background()

    for idx, col in enumerate([G_BLUE_500, G_RED_500, G_YELLOW_500, G_GREEN_500]):
        seg = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(idx * 3.333), 0, Inches(3.334), Inches(0.12))
        seg.fill.solid()
        seg.fill.fore_color.rgb = col
        seg.line.fill.background()

    b.add_pill(s, MARGIN_X, Inches(0.75), "WORKSHOP IGN  •  AGENTS IA GÉOSPATIAUX", bg_color=G_GREEN_500)

    tb_t = s.shapes.add_textbox(MARGIN_X, Inches(1.35), Inches(6.9), Inches(2.0))
    tf_t = tb_t.text_frame
    tf_t.word_wrap = True
    r_t = tf_t.paragraphs[0].add_run()
    r_t.text = "Développement Agentique &\nAgents IA Géospatiaux"
    set_font_family(r_t, FONT_TITLE)
    r_t.font.size = SZ_SECTION_TITLE
    r_t.font.bold = True
    r_t.font.color.rgb = C_WHITE

    tb_s = s.shapes.add_textbox(MARGIN_X, Inches(3.55), Inches(6.9), Inches(1.1))
    tf_s = tb_s.text_frame
    tf_s.word_wrap = True
    r_s = tf_s.paragraphs[0].add_run()
    r_s.text = "Google Antigravity  •  Antigravity SDK  •  LangChain4j  •  Google ADK  •  MCP\nPropulsés par Gemini 3.8 Flash, LiteRT-LM (Gemma 4) & l'IGN"
    set_font_family(r_s, FONT_BODY)
    r_s.font.size = SZ_SLIDE_SUBTITLE
    r_s.font.color.rgb = RGBColor(218, 220, 224)

    b.add_card(s, MARGIN_X, Inches(4.75), Inches(6.9), Inches(2.15),
               title="Guillaume Laforge",
               accent_color=G_YELLOW_500, bg_color=C_DARK_CARD,
               bullets=[
                   ("Developer Advocate ", "chez Google Cloud"),
                   ("Blog : ", "https://glaforge.dev"),
                   ("Decks : ", "https://speakerdeck.com/glaforge")
               ])

    b.add_fitted_image(s, "map-viewer-screenshot.png", Inches(7.8), Inches(1.3), Inches(4.933), Inches(5.55),
                       caption="Cockpit Géospatial 3D Temps Réel (Plan IGN V2 + BD TOPO V3)")

    # --------------------------------------------------------------------------
    # SLIDE 2 : AGENDA PROGRESSIF (Concept -> Framework -> Démo IGN)
    # --------------------------------------------------------------------------
    s = b.new_slide("AGENDA PROGRESSIF", "Fil Conducteur : Concept → Framework → Démo IGN",
                    "Chaque brique technologique est immédiatement illustrée par son cas d'étude sur la Géoplateforme", G_BLUE_500)
    b.layout_four_columns(s, [
        dict(badge="PARTIE 1 & 2", title="Fondamentaux & Socle IGN", accent_color=G_YELLOW_500, bullets=[
            ("Concepts IA : ", "@Tool, SKILL.md & MCP"),
            ("Géoplateforme : ", "6 flux data.geopf.fr"),
            ("Démo Prélude : ", "Test live & Cockpit 3D")
        ]),
        dict(badge="PARTIE 3", title="Antigravity & Démo Acte 3", accent_color=G_RED_500, bullets=[
            ("Antigravity : ", "2.0, IDE, CLI & SDK Java"),
            ("Gouvernance : ", "Policies HITL & Hooks"),
            ("Démo Acte 3 : ", "Détective VLM BD ORTHO")
        ]),
        dict(badge="PARTIE 4 & 5", title="LangChain4j, ADK & Démos 1–2", accent_color=G_BLUE_500, bullets=[
            ("LangChain4j : ", "AiServices & RAG ONNX"),
            ("Démo Acte 1 : ", "Copilote Urbanisme ZAN"),
            ("ADK & Acte 2 : ", "Cellule Crise 5 agents")
        ]),
        dict(badge="PARTIE 6", title="MCP, LiteRT & Démo Acte 4", accent_color=G_GREEN_500, bullets=[
            ("Serveur MCP : ", "20 lignes de Java"),
            ("Démo Acte 4 : ", "Double MCP & WFS"),
            ("Souveraineté : ", "LiteRT-LM & Gemma 4")
        ]),
    ])

    # ==========================================================================
    # PARTIE 1 : DU LLM À L'AGENT IA & STANDARDS (Slides 3 à 9)
    # ==========================================================================
    b.add_section_divider(
        "PARTIE 1",
        "Du Modèle (LLM)\nau Système Agentique",
        "Comprendre pourquoi un LLM seul ne suffit pas en géomatique et comment l'équiper d'outils, de savoir-faire et de protocoles ouverts.",
        [
            "Limites d'un LLM seul sur le terrain",
            "L'équation d'un Agent IA",
            "Du Prompt au Loop Engineering",
            "Tools (@Tool), Skills (SKILL.md) & MCP"
        ],
        accent_color=G_YELLOW_500
    )

    # SLIDE 4 : Pourquoi un LLM seul échoue en Géomatique
    s = b.new_slide("PARTIE 1 • CONSTAT", "Pourquoi un LLM Seul Échoue en Géomatique ?",
                    "Un modèle de langage prédit des mots, mais ignore la réalité physique du territoire", G_YELLOW_500)
    b.layout_two_cards(s,
        dict(badge="SANS OUTILS", title="Un LLM isolé hallucine", accent_color=G_RED_500, bullets=[
            ("Coordonnées floues : ", "Incapable de géocoder une adresse au mètre près."),
            ("Aucun relief : ", "Ignore l'altitude NGF réelle et la pente du terrain."),
            ("Données figées : ", "Ne voit ni la BD ORTHO 20 cm, ni la BD TOPO V3.")
        ]),
        dict(badge="AVEC UN AGENT IA", title="Un Agent connecté vérifie tout", accent_color=G_GREEN_500, bullets=[
            ("Appels à la source : ", "Interroge en direct la Géoplateforme IGN."),
            ("Calculs exacts : ", "Croise RGE ALTI, vecteurs WFS et règlement PLU."),
            ("Preuve visuelle : ", "Inspecte l'orthophoto et génère une carte.")
        ])
    )

    # SLIDE 5 : L'Équation Fondamentale d'un Agent IA
    s = b.new_slide("PARTIE 1 • ARCHITECTURE", "L'Équation Fondamentale d'un Agent IA",
                    "Les 5 briques essentielles qui transforment Gemini 3.8 Flash en agent opérationnel", G_YELLOW_500)
    nodes = [
        ("1. Modèle", "Gemini 3.8 Flash\nRaisonnement & Vision", G_BLUE_500, G_BLUE_TINT),
        ("2. Outils", "@Tool / MCP\nAPIs & Calculs", G_GREEN_500, G_GREEN_TINT),
        ("3. Skills & RAG", "Savoir-faire métier\nSKILL.md & PLU", G_YELLOW_500, G_YELLOW_TINT),
        ("4. Mémoire", "SessionState\nContexte partagé", G_BLUE_500, G_BLUE_TINT),
        ("5. Gouvernance", "Policies & Hooks\nValidation humaine", G_RED_500, G_RED_TINT),
    ]
    for i, (t, sub, col, bg_c) in enumerate(nodes):
        x = MARGIN_X + Inches(i * 2.48)
        b.add_diagram_node(s, x, BODY_TOP, Inches(2.2), Inches(1.45), t, sub, bg_color=bg_c, border_color=col)
        if i < 4:
            b.add_arrow(s, x + Inches(2.23), BODY_TOP + Inches(0.58), Inches(0.22), Inches(0.28), "right", col)

    b.add_card(s, MARGIN_X, Inches(3.95), CONTENT_W, Inches(2.70),
               title="Principe directeur : séparer le raisonnement de l'exécution",
               accent_color=G_YELLOW_500,
               bullets=[
                   ("Le Modèle décide : ", "Gemini 3.8 Flash analyse la mission et choisit les outils à appeler."),
                   ("Le Harnais exécute : ", "Le SDK Java invoque les APIs, applique la sécurité et garde l'état.")
               ])

    # SLIDE 6 : Du Prompt Engineering au Loop Engineering
    s = b.new_slide("PARTIE 1 • LOOP ENGINEERING", "Du Prompt Engineering au « Loop Engineering »",
                    "La vraie puissance d'un agent réside dans sa boucle fermée d'auto-correction", G_YELLOW_500)
    b.layout_three_cards(s, [
        dict(badge="2023", title="1. Prompt Engineering", accent_color=G_BLUE_500, bullets=[
            ("Question unique : ", "Formuler le meilleur prompt possible."),
            ("Zéro rétroaction : ", "Aucune correction automatique en cas d'erreur.")
        ]),
        dict(badge="2024", title="2. Context Engineering", accent_color=G_YELLOW_500, bullets=[
            ("Enrichissement : ", "Injecter les bons extraits RAG et schémas."),
            ("Flux linéaire : ", "Pas d'adaptation dynamique aux erreurs d'API.")
        ]),
        dict(badge="2025–2026", title="3. Loop Engineering", accent_color=G_GREEN_500, bullets=[
            ("Plan → Act → Verify : ", "L'agent agit, observe et se corrige."),
            ("Auto-réparation : ", "Lit le schéma XSD si un filtre WFS échoue !")
        ])
    ])

    # SLIDE 7 : Standard #1 — Les Outils (Function Calling)
    s = b.new_slide("PARTIE 1 • STANDARD #1 : TOOLS", "Les Outils (Function Calling) : Les « Mains » de l'Agent",
                    "Permettre à Gemini 3.8 Flash d'invoquer des méthodes Java déterministes", G_BLUE_500)
    b.add_diagram_node(s, MARGIN_X, BODY_TOP, Inches(3.5), Inches(1.35),
                       "1. Gemini 3.8 Flash", "Émet une intention d'appel :\ngeocode_address(\"Saint-Mandé\")",
                       bg_color=G_BLUE_TINT, border_color=G_BLUE_500)
    b.add_arrow(s, Inches(4.25), BODY_TOP + Inches(0.52), Inches(0.6), Inches(0.3), "right", G_BLUE_500)
    b.add_diagram_node(s, Inches(5.0), BODY_TOP, Inches(3.5), Inches(1.35),
                       "2. SDK Java (Exécution)", "Exécute la méthode Java\nsur https://data.geopf.fr",
                       bg_color=G_GREEN_TINT, border_color=G_GREEN_500)
    b.add_arrow(s, Inches(8.65), BODY_TOP + Inches(0.52), Inches(0.6), Inches(0.3), "right", G_GREEN_500)
    b.add_diagram_node(s, Inches(9.4), BODY_TOP, Inches(3.333), Inches(1.35),
                       "3. Résultat Structuré", "Retourne le JSON exact :\nlon=2.4243, lat=48.8445",
                       bg_color=G_YELLOW_TINT, border_color=G_YELLOW_500)

    b.add_card(s, MARGIN_X, Inches(3.85), CONTENT_W, Inches(2.80),
               title="Point clé : le LLM n'exécute jamais le code directement",
               accent_color=G_BLUE_500,
               bullets=[
                   ("Contrat déclaratif : ", "Vos méthodes Java sont décrites par @Tool, @P et @Schema."),
                   ("Contrôle côté JVM : ", "Votre application maîtrise les appels réseau, l'audit et la sécurité.")
               ])

    # SLIDE 8 : Standard #2 — Les Agent Skills (SKILL.md)
    s = b.new_slide("PARTIE 1 • STANDARD #2 : AGENT SKILLS", "Les Agent Skills (`SKILL.md`) : Le « Savoir-Faire » Métier",
                    "Un standard ouvert avec chargement progressif (Progressive Disclosure)", G_RED_500)
    b.layout_two_cards(s,
        dict(badge="STRUCTURE D'UN SKILL", title="Un dossier modulaire auto-descriptif", accent_color=G_RED_500, bullets=[
            ("Fichier SKILL.md : ", "En-tête YAML (name + description) et guide Markdown."),
            ("Ressources : ", "Peut inclure scripts/, examples/ et references/."),
            ("Standard ouvert : ", "Supporté par Antigravity IDE, CLI et SDK Java.")
        ]),
        dict(badge="PROGRESSIVE DISCLOSURE", title="Économiser 95 % du contexte", accent_color=G_GREEN_500, bullets=[
            ("1. Au démarrage : ", "Seule la description YAML (~100 octets) est chargée."),
            ("2. À l'activation : ", "Le corps du SKILL.md n'est lu qu'en cas de besoin."),
            ("Cas IGN : ", "Enseigne les règles CRS:84 vs EPSG:4326 à la demande.")
        ])
    )

    # SLIDE 9 : Standard #3 — Model Context Protocol (MCP)
    s = b.new_slide("PARTIE 1 • STANDARD #3 : MCP", "Model Context Protocol (MCP) : Le « Port USB-C » de l'IA",
                    "Découpler les fournisseurs d'outils (Serveurs MCP) des applications clientes", G_GREEN_500)
    b.add_diagram_node(s, MARGIN_X, BODY_TOP, Inches(3.6), Inches(1.35),
                       "Clients MCP", "Antigravity IDE, Claude,\nLangChain4j, Google ADK",
                       bg_color=G_BLUE_TINT, border_color=G_BLUE_500)
    b.add_arrow(s, Inches(4.35), BODY_TOP + Inches(0.52), Inches(1.1), Inches(0.32), "both", G_GREEN_500)
    b.add_diagram_node(s, Inches(5.6), BODY_TOP, Inches(2.8), Inches(1.35),
                       "Protocole JSON-RPC", "Transport STDIO (local)\nou Streamable HTTP",
                       bg_color=G_YELLOW_TINT, border_color=G_YELLOW_500)
    b.add_arrow(s, Inches(8.55), BODY_TOP + Inches(0.52), Inches(0.8), Inches(0.32), "both", G_GREEN_500)
    b.add_diagram_node(s, Inches(9.5), BODY_TOP, Inches(3.233), Inches(1.35),
                       "Serveurs MCP", "ign-geoplateforme (Java)\nign-mcp-carto (Node.js)",
                       bg_color=G_GREEN_TINT, border_color=G_GREEN_500)

    b.add_card(s, MARGIN_X, Inches(3.85), CONTENT_W, Inches(2.80),
               title="Pourquoi MCP change la donne pour l'IGN ?",
               accent_color=G_GREEN_500,
               bullets=[
                   ("Réutilisabilité universelle : ", "Un seul serveur MCP Géoplateforme sert tous les IDE et agents."),
                   ("Chaînage inter-serveurs : ", "Combine extraction WFS (Java) et rendu Plan IGN V2 (Node.js).")
               ])

    # ==========================================================================
    # PARTIE 2 : SOCLE GÉOSPATIAL IGN, DÉMO PRÉLUDE (_00) & COCKPIT 3D (Slides 10 à 18)
    # ==========================================================================
    b.add_section_divider(
        "PARTIE 2",
        "Socle Géoplateforme IGN,\nPrélude & Cockpit 3D",
        "Découvrir les 6 géoservices souverains (data.geopf.fr), notre classe Java unifiée à 10 outils, et les tester immédiatement en direct.",
        [
            "Les 6 géoservices de la Géoplateforme IGN",
            "Le piège OGC CRS:84 vs EPSG:4326 & SKILL.md",
            "Classe unifiée GeoplateformeTools.java (10 outils)",
            "🎬 Démo Prélude (_00) & Cockpit 3D Temps Réel"
        ],
        accent_color=G_GREEN_500
    )

    # SLIDE 11 : Les Géoservices IGN — Localisation, Relief & Mobilité
    s = b.new_slide("PARTIE 2 • GÉOPLATEFORME (1/2)", "Géoplateforme IGN : Localisation, Relief & Mobilité",
                    "Flux REST ouverts et gratuits (`https://data.geopf.fr`) — Aucune clé API requise", G_GREEN_500)
    b.layout_three_cards(s, [
        dict(badge="1. GÉOCODAGE BAN", title="Recherche d'Adresses", accent_color=G_BLUE_500, bullets=[
            ("Endpoint : ", "/geocodage/search"),
            ("Entrée : ", "Adresse postale libre."),
            ("Sortie : ", "WGS84 (lon, lat) & code INSEE.")
        ]),
        dict(badge="2. RGE ALTI", title="Altimétrie & Pente", accent_color=G_GREEN_500, bullets=[
            ("Endpoint : ", "/altimetrie/.../elevation.json"),
            ("Méthode : ", "Sonde 5 points (centre ± 50m)."),
            ("Sortie : ", "Altitude NGF (m) & pente (%).")
        ]),
        dict(badge="3. VALHALLA", title="Calcul d'Isochrones", accent_color=G_YELLOW_500, bullets=[
            ("Endpoint : ", "/navigation/isochrone"),
            ("Profils : ", "Piéton ou Véhicule."),
            ("Sortie : ", "Polygone GeoJSON d'accès.")
        ])
    ])

    # SLIDE 12 : Les Géoservices IGN — Vecteurs WFS, Imagerie WMS & Plan IGN
    s = b.new_slide("PARTIE 2 • GÉOPLATEFORME (2/2)", "Géoplateforme IGN : Vecteurs WFS, Orthophoto & Plan IGN",
                    "Interroger les objets 3D du territoire, la photographie aérienne 20 cm et le fond souverain", G_GREEN_500)
    b.layout_three_cards(s, [
        dict(badge="4. WFS 2.0 BD TOPO", title="Vecteurs 3D & Cadastre", accent_color=G_BLUE_500, bullets=[
            ("Couches : ", "batiment, cours_d_eau, vegetation, parcelle."),
            ("Attributs : ", "Hauteurs, étages et matériaux.")
        ]),
        dict(badge="5. WMS 1.3 BD ORTHO", title="Photo Aérienne 20 cm", accent_color=G_RED_500, bullets=[
            ("Couche : ", "HR.ORTHOIMAGERY.ORTHOPHOTOS."),
            ("Usage IA : ", "Tuile PNG 512×512 pour Gemini Vision.")
        ]),
        dict(badge="6. WMTS PLAN IGN V2", title="Fond Cartographique", accent_color=G_GREEN_500, bullets=[
            ("Couche : ", "PLANIGNV2."),
            ("Usage IA : ", "Fond officiel pour MCP Carto & Cockpit 3D.")
        ])
    ])

    # SLIDE 13 : Le Piège OGC des Projections (CRS:84 vs EPSG:4326)
    s = b.new_slide("PARTIE 2 • PIÈGE OGC & SKILL", "Le Piège Classique OGC : `CRS:84` vs `EPSG:4326`",
                    "Pourquoi les LLMs échouent sur les BBOX OGC sans un Agent Skill dédié", G_RED_500)
    b.layout_two_cards(s,
        dict(badge="LE PIÈGE D'INVERSION D'AXES", title="Direction l'Océan Indien !", accent_color=G_RED_500, bullets=[
            ("EPSG:4326 (Strict OGC) : ", "Ordre Latitude, Longitude (Y, X)."),
            ("CRS:84 (Web / GeoJSON) : ", "Ordre Longitude, Latitude (X, Y)."),
            ("Conséquence : ", "Inverser les deux dans une BBOX WFS renvoie 0 objet.")
        ]),
        dict(badge="LA SOLUTION : SKILL.MD", title="Encapsuler l'Expertise IGN", accent_color=G_GREEN_500, bullets=[
            ("Fichier : ", ".agents/skills/ign-geoplateforme/SKILL.md"),
            ("Règle d'or : ", "Impose SRSNAME=CRS:84 (lon, lat) partout."),
            ("Guide WFS : ", "Documente la découverte XSD en 3 étapes.")
        ])
    )

    # SLIDE 14 : Architecture Unifiée — GeoplateformeTools.java
    s = b.new_slide("PARTIE 2 • SOCLE JAVA UNIFIÉ", "Une Seule Classe Java pour les 3 SDKs + MCP",
                    "`GeoplateformeTools.java` : zéro duplication de code grâce à la double annotation", G_GREEN_500)
    consumers = [
        ("LangChain4j", "AiServices.tools()", G_BLUE_500, G_BLUE_TINT),
        ("Google ADK", "FunctionTool.create()", G_GREEN_500, G_GREEN_TINT),
        ("Antigravity SDK", "ToolRegistry", G_RED_500, G_RED_TINT),
        ("Serveur MCP", "DefaultMcpServer", G_YELLOW_500, G_YELLOW_TINT),
    ]
    for i, (t, sub, col, bg_c) in enumerate(consumers):
        x = MARGIN_X + Inches(i * 3.1)
        b.add_diagram_node(s, x, BODY_TOP, Inches(2.8), Inches(1.05), t, sub, bg_color=bg_c, border_color=col)
        b.add_arrow(s, x + Inches(1.25), BODY_TOP + Inches(1.10), Inches(0.3), Inches(0.26), "down", col)

    b.add_diagram_node(s, MARGIN_X, Inches(3.55), CONTENT_W, Inches(0.92),
                       "fr.ign.workshop.tools.GeoplateformeTools (10 Outils Géospatiaux Unifiés)",
                       "Annotés simultanément avec @Tool / @P (LangChain4j) et @Schema (Google ADK)",
                       bg_color=G_YELLOW_TINT, border_color=G_YELLOW_500)

    b.add_code_box(s, MARGIN_X, Inches(4.65), CONTENT_W, Inches(2.00),
                   "Double annotation sur chaque méthode de GeoplateformeTools.java", [
                       "@Tool(\"Extrait les bâtiments BD TOPO V3 (hauteurs, étages) dans un rayon donné\")",
                       "@Schema(name = \"query_bdtopo_buildings\", description = \"Extrait les bâtiments BD TOPO V3\")",
                       "public String queryBdtopoBuildings(",
                       "  @P(\"Longitude WGS84\") @Schema(name = \"lon\") double lon,",
                       "  @P(\"Latitude WGS84\")  @Schema(name = \"lat\") double lat,",
                       "  @P(\"Rayon en mètres\") @Schema(name = \"radiusMeters\") double radiusMeters) { ... }"
                   ])

    # SLIDE 15 : Catalogue des 10 Outils (7 Métier + 3 Introspection WFS)
    s = b.new_slide("PARTIE 2 • CATALOGUE DES 10 OUTILS", "7 Outils Métier + 3 Outils d'Introspection WFS Dynamique",
                    "Aller au-delà des couches codées en dur en permettant à l'agent d'explorer les 64 couches OGC", G_GREEN_500)
    b.layout_two_cards(s,
        dict(badge="7 OUTILS MÉTIER CLÉS EN MAIN", title="Opérations Immédiates (Actes 1 à 3)", accent_color=G_BLUE_500, bullets=[
            ("Localisation & Relief : ", "geocode_address, get_elevation_and_slope, compute_isochrone."),
            ("Vecteurs BD TOPO : ", "query_bdtopo_buildings, query_bdtopo_environment."),
            ("Vision & Action : ", "download_bdortho_aerial_image, submit_bdtopo_collaborative_report.")
        ]),
        dict(badge="3 OUTILS D'INTROSPECTION OGC", title="Découverte WFS Dynamique (Acte 4)", accent_color=G_GREEN_500, bullets=[
            ("1. search_wfs_layers : ", "Cherche par mot-clé dans les 64 couches WFS."),
            ("2. describe_wfs_layer_schema : ", "Lit le XSD pour trouver le nom des colonnes."),
            ("3. query_any_wfs_layer : ", "Exécute un filtre CQL et renvoie wfs_geojson_url.")
        ])
    )

    # SLIDE 16 : 🎬 DÉMO PRÉLUDE (_00_TestGeoplateformeTools.java)
    s = b.new_slide("🎬 DÉMO PRÉLUDE • _00_TestGeoplateformeTools", "Démo Prélude : Test Temps Réel sur le Siège de l'IGN",
                    "Vérifier en une commande la portabilité des 10 outils et la réponse live de `data.geopf.fr`", G_GREEN_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(5.9), BODY_H,
                   "_00_TestGeoplateformeTools.java", [
                       "GeoplateformeTools tools = new GeoplateformeTools();",
                       "",
                       "// 1. Enregistrement Antigravity SDK",
                       "ToolRegistry agy = new ToolRegistry();",
                       "agy.registerToolsFromObject(tools);",
                       "",
                       "// 2. Enregistrement Google ADK",
                       "FunctionTool adk = FunctionTool.create(",
                       "  tools, \"geocodeAddress\");",
                       "",
                       "// 3. Appels live (Place de Jaude, Clermont-Ferrand)",
                       "tools.geocodeAddress(\"Place de Jaude, 63000 Clermont-Ferrand\");",
                       "tools.getElevationAndSlope(3.0821, 45.7766);",
                       "tools.queryBdtopoBuildings(3.0821, 45.7766, 120);"
                   ])
    b.add_code_box(s, Inches(6.833), BODY_TOP, Inches(5.9), BODY_H,
                   "Résultat Console (Clermont-Ferrand — Données Réelles IGN)", [
                       "✓ Antigravity SDK : 10 outils enregistrés",
                       "✓ Google ADK      : geocode_address prêt",
                       "",
                       "📍 Géocodage BAN (Place de Jaude) :",
                       "   lon=3.082062, lat=45.776588 | Lambert-93=(706376, 6519667)",
                       "",
                       "⛰️ Altimétrie RGE ALTI :",
                       "   Altitude : 387.16 m NGF | Pente : 2.12% (Sud-Ouest)",
                       "",
                       "🏢 WFS BD TOPO V3 (rayon 120m) :",
                       "   86 bâtiments | Hauteur max: 29.1m (10 étages)",
                       "",
                       "🚶 Isochrone 5 min piéton :",
                       "   Enveloppe 0.59 km x 0.71 km (13 sommets)"
                   ])

    # SLIDE 17 : 🎬 COCKPIT 3D TEMPS RÉEL (1/2) — Plan IGN V2 & Bâti 3D
    s = b.new_slide("🎬 COCKPIT 3D • MAPLIBRE GL JS (1/2)", "Cockpit Géospatial 3D Temps Réel : Plan IGN V2 & Bâti 3D",
                    "Chaque appel à `GeoplateformeTools` met à jour la carte 3D en direct (`target/map-data/latest-state.json`)", G_BLUE_500)
    b.add_fitted_image(s, "map-viewer-screenshot.png", MARGIN_X, BODY_TOP, Inches(7.6), BODY_H,
                       caption="web/map-viewer.html — Plan IGN V2 + Extrusion 3D BD TOPO V3 + Isochrone Valhalla")
    b.add_card(s, Inches(8.45), BODY_TOP, Inches(4.28), BODY_H,
               badge="SYNCHRONISATION LIVE", title="Caméra Pilotée par l'Agent", accent_color=G_BLUE_500,
               bullets=[
                   ("Polling (1,5 s) : ", "Animation flyTo dès qu'un outil est appelé avec une nouvelle adresse ou coordonnée."),
                   ("Extrusion 3D : ", "Hauteur réelle issue de BDTOPO_V3:batiment."),
                   ("Isochrone : ", "Polygone Valhalla et pente RGE ALTI.")
               ])

    # SLIDE 18 : 🎬 COCKPIT 3D TEMPS RÉEL (2/2) — Orthophoto 20 cm & Cadastre PCI
    s = b.new_slide("🎬 COCKPIT 3D • MAPLIBRE GL JS (2/2)", "Cockpit 3D : Orthophotographie 20 cm & Cadastre PCI",
                    "Basculer instantanément entre le Plan IGN V2, la BD ORTHO aérienne et le Parcellaire Express", G_GREEN_500)
    b.add_fitted_image(s, "map-viewer-ortho-screenshot.png", MARGIN_X, BODY_TOP, Inches(7.6), BODY_H,
                       caption="map-viewer.html — Fond WMS BD ORTHO 20 cm + Surcouche Cadastrale PCI + Bâti 3D")
    b.add_card(s, Inches(8.45), BODY_TOP, Inches(4.28), BODY_H,
               badge="MULTI-COUCHES IGN", title="3 Flux WMTS / WMS Intégrés", accent_color=G_GREEN_500,
               bullets=[
                   ("Plan IGN V2 : ", "Lisibilité topographique optimale."),
                   ("BD ORTHO 20 cm : ", "Inspection visuelle des toitures et jardins."),
                   ("Cadastre PCI : ", "Limites parcellaires superposables en 1 clic.")
               ])

    # ==========================================================================
    # PARTIE 3 : GOOGLE ANTIGRAVITY, SDK JAVA & DÉMO ACTE 3 (Slides 19 à 32)
    # ==========================================================================
    b.add_section_divider(
        "PARTIE 3",
        "Google Antigravity, SDK Java\n& Démo Acte 3 (Multimodal)",
        "L'environnement de développement AI-First de Google, son SDK Java embarqué, et leur mise en œuvre dans notre Détective Territorial Multimodal.",
        [
            "Les 4 surfaces de Google Antigravity",
            "Subagents, Skills, MCP, Rules & Hooks",
            "Antigravity SDK for Java : Architecture & Policies",
            "🎬 Démo Acte 3 : Détective Multimodal BD ORTHO × BD TOPO"
        ],
        accent_color=G_RED_500
    )

    # SLIDE 20 : Google Antigravity — Les 4 Surfaces
    s = b.new_slide("PARTIE 3 • GOOGLE ANTIGRAVITY", "Google Antigravity : 1 Moteur, 4 Surfaces",
                    "Du poste de commandement multi-agents jusqu'au SDK embarqué dans votre code Java", G_RED_500)
    b.layout_four_columns(s, [
        dict(badge="SURFACE 1", title="Antigravity 2.0", accent_color=G_RED_500, bullets=[
            ("Agent Manager : ", "Pilotez plusieurs agents en parallèle."),
            ("Inbox asynchrone : ", "Suivi des tâches longues et validations.")
        ]),
        dict(badge="SURFACE 2", title="Antigravity IDE", accent_color=G_BLUE_500, bullets=[
            ("Éditeur AI-First : ", "Diff interactif et widgets Generative UI."),
            ("Browser Agent : ", "Contrôle Chrome pour tester vos webapps.")
        ]),
        dict(badge="SURFACE 3", title="Antigravity CLI", accent_color=G_GREEN_500, bullets=[
            ("Commande agy : ", "Missions autonomes dans le terminal."),
            ("CI/CD & Serveurs : ", "Partage les mêmes Skills et MCP.")
        ]),
        dict(badge="SURFACE 4", title="Antigravity SDK", accent_color=G_YELLOW_500, bullets=[
            ("Java & Python : ", "Embarquez localharness dans vos apps."),
            ("Focus Acte 3 : ", "Détective territorial multimodal.")
        ]),
    ])

    # SLIDE 21 : La Boucle Fermée Plan → Act → Verify
    s = b.new_slide("PARTIE 3 • BOUCLE FERMÉE", "La Boucle Fermée d'Antigravity : Plan → Act → Verify",
                    "Antigravity ne s'arrête pas à la génération de code : il vérifie systématiquement son travail", G_RED_500)
    b.layout_three_cards(s, [
        dict(badge="ÉTAPE 1", title="PLAN (Concevoir)", accent_color=G_BLUE_500, bullets=[
            ("Exploration : ", "Analyse le dépôt et charge les Skills."),
            ("Plan d'action : ", "Soumet l'Implementation Plan à validation.")
        ]),
        dict(badge="ÉTAPE 2", title="ACT (Construire)", accent_color=G_GREEN_500, bullets=[
            ("Édition précise : ", "Modifie le code sur plusieurs fichiers."),
            ("Outils & MCP : ", "Exécute Maven, scripts et géoservices.")
        ]),
        dict(badge="ÉTAPE 3", title="VERIFY (Prouver)", accent_color=G_RED_500, bullets=[
            ("Tests & Browser : ", "Vérifie la compilation et le rendu web."),
            ("Walkthrough : ", "Produit un rapport final avec captures.")
        ])
    ])

    # SLIDE 22 : Primitives d'Autonomie — Subagents & Isolation
    s = b.new_slide("PARTIE 3 • AUTONOMIE", "Passer à l'Échelle avec les Subagents",
                    "Déléguer les tâches d'exploration ou de test à des sous-agents spécialisés au contexte isolé", G_RED_500)
    b.layout_three_cards(s, [
        dict(badge="RESEARCH SUBAGENT", title="Exploration Sans Bruit", accent_color=G_BLUE_500, bullets=[
            ("Lecture seule : ", "Explore des dizaines de fichiers en tâche de fond."),
            ("Contexte préservé : ", "Ne renvoie que la synthèse à l'agent parent.")
        ]),
        dict(badge="BROWSER SUBAGENT", title="Test Web Visuel", accent_color=G_GREEN_500, bullets=[
            ("Pilote Chrome : ", "Clique, navigue et inspecte le DOM."),
            ("Preuve visuelle : ", "Prend des screenshots et lit la console.")
        ]),
        dict(badge="WORKSPACES ISOLÉS", title="Parallélisme Sûr", accent_color=G_YELLOW_500, bullets=[
            ("3 modes : ", "inherit, branch (clone) ou share (worktree)."),
            ("Réveil réactif : ", "Notifie le parent dès la fin du travail.")
        ])
    ])

    # SLIDE 23 : Custom Agents, Slash Commands & Skills
    s = b.new_slide("PARTIE 3 • PERSONNALISATION", "Custom Agents, Slash Commands & Agent Skills",
                    "Adapter le comportement d'Antigravity aux standards de votre équipe", G_RED_500)
    b.layout_two_cards(s,
        dict(badge="RACCOURCIS & RÔLES", title="Slash Commands & Custom Agents", accent_color=G_BLUE_500, bullets=[
            ("/plan & /grill-me : ", "Aligner l'architecture avant de coder."),
            ("/goal & /boost : ", "Exécution autonome approfondie."),
            ("Custom Agents : ", "Profils dédiés (prompt, modèle, outils).")
        ]),
        dict(badge="DÉCOUVERTE AUTOMATIQUE", title="Hiérarchie des Agent Skills", accent_color=G_GREEN_500, bullets=[
            ("Workspace (.agents/skills/) : ", "Partagé via Git avec toute l'équipe."),
            ("Global (~/.gemini/config/skills/) : ", "Compétences personnelles."),
            ("Dans ce projet : ", "ign-geoplateforme est détecté nativement !")
        ])
    )

    # SLIDE 24 : Gouvernance — Rules, Hooks, Permissions & Plugins
    s = b.new_slide("PARTIE 3 • GOUVERNANCE", "Contrôle & Sécurité : Rules, Hooks & Plugins",
                    "Encadrer l'autonomie de l'agent par des garde-fous déterministes et partageables", G_RED_500)
    b.layout_three_cards(s, [
        dict(badge="RULES (.md)", title="Consignes Permanentes", accent_color=G_BLUE_500, bullets=[
            ("Priorité absolue : ", "Toujours actives dans le contexte."),
            ("Standards d'équipe : ", "Conventions de code et routage.")
        ]),
        dict(badge="LIFECYCLE HOOKS", title="Interception Déterministe", accent_color=G_RED_500, bullets=[
            ("Pre / Post Tool Call : ", "Inspecte chaque appel d'outil."),
            ("Audit & Blocage : ", "Journalise ou bloque une action.")
        ]),
        dict(badge="PLUGINS (/plugin)", title="Distribution Clé en Main", accent_color=G_GREEN_500, bullets=[
            ("Bundle complet : ", "Skills + Rules + MCP + Sidecars UI."),
            ("Partage en 1 clic : ", "Idéal pour un kit « Géomatique IGN ».")
        ])
    ])

    # SLIDE 25 : Antigravity SDK for Java — Architecture
    s = b.new_slide("PARTIE 3 • ANTIGRAVITY SDK JAVA", "Antigravity SDK for Java : Architecture",
                    "Piloter le véritable moteur Go d'Antigravity (`localharness`) depuis la JVM via WebSockets", G_RED_500)
    b.add_diagram_node(s, MARGIN_X, BODY_TOP, Inches(3.5), Inches(1.35),
                       "Application Java 21+", "Agent.builder() / AgentConfig\nBloc try-with-resources",
                       bg_color=G_BLUE_TINT, border_color=G_BLUE_500)
    b.add_arrow(s, Inches(4.25), BODY_TOP + Inches(0.52), Inches(1.1), Inches(0.32), "both", G_RED_500)
    b.add_diagram_node(s, Inches(5.5), BODY_TOP, Inches(3.3), Inches(1.35),
                       "Binaire Go : localharness", "Extrait & lancé automatiquement\nCommunication WebSockets",
                       bg_color=G_RED_TINT, border_color=G_RED_500)
    b.add_arrow(s, Inches(8.95), BODY_TOP + Inches(0.52), Inches(0.8), Inches(0.32), "both", G_GREEN_500)
    b.add_diagram_node(s, Inches(9.9), BODY_TOP, Inches(2.833), Inches(1.35),
                       "Gemini 3.8 Flash", "Appels HTTPS Gemini &\ncallbacks @Tool locaux",
                       bg_color=G_GREEN_TINT, border_color=G_GREEN_500)

    b.add_card(s, MARGIN_X, Inches(3.85), CONTENT_W, Inches(2.80),
               title="Pourquoi cette architecture hybride Java + Go ?",
               accent_color=G_RED_500,
               bullets=[
                   ("Fidélité totale : ", "Même moteur, mêmes Skills et mêmes Policies que l'IDE Antigravity."),
                   ("AutoCloseable : ", "Le bloc try (...) gère proprement le cycle de vie du processus Go.")
               ])

    # SLIDE 26 : Antigravity SDK for Java — Initialisation & Skills
    s = b.new_slide("PARTIE 3 • ANTIGRAVITY SDK JAVA", "Initialiser un Agent & Charger un Skill en Java",
                    "Une API fluide pour configurer Gemini 3.8 Flash, le prompt système et les dossiers de Skills", G_RED_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(7.1), BODY_H,
                   "Configuration AgentConfig (Antigravity SDK for Java)", [
                       "// Configuration de l'agent avec Gemini 3.8 Flash",
                       "AgentConfig config = AgentConfig.builder()",
                       "  .model(\"gemini-3.8-flash\")",
                       "  .apiKey(System.getenv(\"GEMINI_API_KEY\"))",
                       "  .systemPrompt(\"Tu es un inspecteur territorial IGN.\")",
                       "  .addSkillPath(Path.of(\".agents/skills/ign-geoplateforme\"))",
                       "  .build();",
                       "",
                       "// Enregistrement des outils Java par réflexion",
                       "ToolRegistry tools = new ToolRegistry();",
                       "tools.registerToolsFromObject(new GeoplateformeTools());"
                   ])
    b.add_card(s, Inches(7.95), BODY_TOP, Inches(4.78), BODY_H,
               badge="PONT D'OUTILS UNIVERSEL", title="Réflexion à 3 Niveaux", accent_color=G_RED_500,
               bullets=[
                   ("1. Interface Tool : ", "Implémentation bas niveau directe."),
                   ("2. Annotations SDK : ", "@Tool et @Param natifs."),
                   ("3. Pont LangChain4j : ", "Détecte les @Tool et @P existants !")
               ])

    # SLIDE 27 : Antigravity SDK for Java — Streaming Réflexion vs Réponse
    s = b.new_slide("PARTIE 3 • ANTIGRAVITY SDK JAVA", "Streaming Temps Réel : Pensées vs Réponse Finale",
                    "Observer en direct le raisonnement interne de Gemini 3.8 Flash avant l'émission de la réponse", G_RED_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(7.1), BODY_H,
                   "Écouteur d'événements (SessionEventListener)", [
                       "SessionEventListener listener = new SessionEventListener() {",
                       "  @Override",
                       "  public void onThoughtsDelta(String delta) {",
                       "    // Flux de réflexion interne (Thinking)",
                       "    System.out.print(GRAY + delta + RESET);",
                       "  }",
                       "",
                       "  @Override",
                       "  public void onTextDelta(String delta) {",
                       "    // Flux de la réponse finale destinée à l'utilisateur",
                       "    System.out.print(delta);",
                       "  }",
                       "};"
                   ])
    b.add_card(s, Inches(7.95), BODY_TOP, Inches(4.78), BODY_H,
               badge="OBSERVABILITÉ", title="Transparence du Raisonnement", accent_color=G_BLUE_500,
               bullets=[
                   ("thoughtsDelta : ", "Trace la planification et la lecture du Skill."),
                   ("textDelta : ", "Diffuse la réponse finale en continu."),
                   ("Idéal pour l'UI : ", "Alimente une console ou un cockpit live.")
               ])

    # SLIDE 28 : Antigravity SDK for Java — Policies & Hooks (Human-in-the-Loop)
    s = b.new_slide("PARTIE 3 • SÉCURITÉ SDK JAVA", "Gouvernance : `Policies` & `PreToolCallDecideHook`",
                    "Appliquer le principe du moindre privilège et exiger une validation humaine sur les actions critiques", G_RED_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(7.3), BODY_H,
                   "Liste blanche + Validation Humaine (HITL) + Hook d'audit", [
                       "Policies securityPolicy = Policies.denyAll()",
                       "  // Lecture autorisée automatiquement",
                       "  .allowTools(\"query_bdtopo_buildings\", \"get_elevation_and_slope\")",
                       "  // Écriture soumise à confirmation de l'opérateur humain !",
                       "  .askUser(\"submit_bdtopo_collaborative_report\",",
                       "       req -> confirmWithHumanOperator(req));",
                       "",
                       "Agent.builder()",
                       "  .policies(securityPolicy)",
                       "  .onPreToolCallDecide(ctx -> {",
                       "    logAudit(ctx.toolName(), ctx.argumentsJson());",
                       "    return HookDecision.allow();",
                       "  });"
                   ])
    b.add_card(s, Inches(8.15), BODY_TOP, Inches(4.58), BODY_H,
               badge="ZÉRO CONFIANCE AVEUGLE", title="Sécurité Déterministe", accent_color=G_RED_500,
               bullets=[
                   ("denyAll() : ", "Tout outil non autorisé est bloqué."),
                   ("askUser() : ", "Exige l'accord humain avant toute écriture."),
                   ("PreToolCallHook : ", "Journalise et filtre les arguments JSON.")
               ])

    # SLIDE 29 : Antigravity SDK for Java — Multimodalité & Sorties Structurées
    s = b.new_slide("PARTIE 3 • MULTIMODALITÉ & SCHÉMAS", "Entrées Multimodales & Sorties Structurées (`Record`)",
                    "Envoyer une image aérienne à Gemini 3.8 Flash et récupérer un objet Java fortement typé", G_RED_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(7.3), BODY_H,
                   "AgentInput.Image + responseSchema(Record.class)", [
                       "// 1. Définition du contrat de sortie via un Record Java",
                       "public record QualificationReport(",
                       "  String address, int bdTopoBuildingCount,",
                       "  boolean anomalyDetected, String visualObservations,",
                       "  String recommendedAction, String confidenceLevel) {}",
                       "",
                       "// 2. Passage du schéma à l'agent et envoi Texte + Image PNG",
                       "try (Agent agent = Agent.builder()",
                       "    .responseSchema(QualificationReport.class).build()) {",
                       "  agent.run(session, List.of(",
                       "    new AgentInput.Text(\"Compare l'orthophoto et la BD TOPO\"),",
                       "    new AgentInput.Image(orthoPngBytes, \"image/png\")",
                       "  ), listener);",
                       "}"
                   ])
    b.add_card(s, Inches(8.15), BODY_TOP, Inches(4.58), BODY_H,
               badge="INTÉGRATION SI", title="Du Pixel au JSON Typé", accent_color=G_GREEN_500,
               bullets=[
                   ("AgentInput.Image : ", "Envoie directement la tuile PNG BD ORTHO."),
                   ("responseSchema : ", "Génère le JSON Schema depuis le Record Java.")
               ])

    # SLIDE 30 : 🎬 DÉMO ACTE 3 — Architecture (Détective Territorial Multimodal)
    s = b.new_slide("🎬 DÉMO ACTE 3 • ARCHITECTURE (ANTIGRAVITY SDK)", "Démo Acte 3 : Le Détective Territorial Multimodal & Gouverné",
                    "Réconcilier l'orthophotographie aérienne 20 cm et les vecteurs BD TOPO sous contrôle humain", G_RED_500)
    steps_a3 = [
        ("1. Tuile WMS BD ORTHO", "Image PNG 512×512\nAgentInput.Image", G_YELLOW_500, G_YELLOW_TINT),
        ("2. Vecteurs BD TOPO", "Bâti + Altimétrie\nListe blanche Policies", G_BLUE_500, G_BLUE_TINT),
        ("3. Gouvernance HITL", "PreToolCallDecideHook\n+ Policies.askUser()", G_RED_500, G_RED_TINT),
        ("4. Sortie Typée", "QualificationReport\nRecord Java validé", G_GREEN_500, G_GREEN_TINT),
    ]
    for i, (t, sub, col, bg_c) in enumerate(steps_a3):
        x = MARGIN_X + Inches(i * 3.1)
        b.add_diagram_node(s, x, BODY_TOP, Inches(2.8), Inches(1.35), t, sub, bg_color=bg_c, border_color=col)
        if i < 3:
            b.add_arrow(s, x + Inches(2.84), BODY_TOP + Inches(0.52), Inches(0.22), Inches(0.28), "right", col)

    b.add_card(s, MARGIN_X, Inches(3.85), CONTENT_W, Inches(2.80),
               title="Cas d'usage IGN : assister la mise à jour de la BD TOPO par Vision IA",
               accent_color=G_RED_500,
               bullets=[
                   ("Vision × Vecteur : ", "Gemini 3.8 Flash compare l'orthophoto 20 cm avec les emprises bâties du WFS."),
                   ("Validation humaine : ", "Policies.askUser bloque l'envoi du signalement jusqu'à l'accord de l'opérateur.")
               ])

    # SLIDE 31 : 🎬 DÉMO ACTE 3 — Photo Aérienne & Rapport Structuré
    s = b.new_slide("🎬 DÉMO ACTE 3 • VISION & RAPPORT STRUCTURÉ", "Démo Acte 3 : Tuile BD ORTHO Inspectée & Rapport Final",
                    "L'agent analyse l'orthophoto 512×512, obtient l'accord humain et produit le Record JSON", G_RED_500)
    b.add_fitted_image(s, "bd_ortho_tile.png", MARGIN_X, BODY_TOP, Inches(4.5), BODY_H,
                       caption="Tuile WMS BD ORTHO 20 cm (512×512 px) — Saint-Mandé")
    b.add_code_box(s, Inches(5.35), BODY_TOP, Inches(7.38), BODY_H,
                   "Trace d'Audit & Sortie QualificationReport (_03_AntigravityDetectiveAgent)", [
                       "🔍 [AUDIT HOOK] Autorisé : query_bdtopo_buildings(r=80m) -> 19 bâtiments",
                       "🔍 [AUDIT HOOK] Autorisé : get_elevation_and_slope -> 48.45m NGF",
                       "",
                       "⚠️ [POLICIES.ASK_USER] Confirmation requise pour :",
                       "   submit_bdtopo_collaborative_report",
                       "   -> Approbation opérateur accordée ✓ (#IGN-COLLAB-94067)",
                       "",
                       "✅ [SORTIE STRUCTURÉE — QualificationReport] :",
                       "{",
                       "  \"address\": \"73 Avenue de Paris, 94160 Saint-Mandé\",",
                       "  \"bdTopoBuildingCount\": 19,",
                       "  \"anomalyDetected\": true,",
                       "  \"visualObservations\": \"Verrière et structure légère en cour",
                       "             intérieure visibles sur la BD ORTHO\",",
                       "  \"recommendedAction\": \"Mise à jour mineure soumise à l'IGN\",",
                       "  \"confidenceLevel\": \"HIGH\"",
                       "}"
                   ])

    # SLIDE 32 : 🎬 DÉMO ACTE 3 — Modale d'Inspection Multimodale dans le Cockpit 3D
    s = b.new_slide("🎬 DÉMO ACTE 3 • COCKPIT VLM", "Démo Acte 3 dans le Cockpit : Modale d'Inspection VLM",
                    "Comparer côte à côte la tuile aérienne envoyée à Gemini 3.8 Flash et les métriques BD TOPO", G_RED_500)
    b.add_fitted_image(s, "map-viewer-modal-screenshot.png", MARGIN_X, BODY_TOP, Inches(7.6), BODY_H,
                       caption="map-viewer.html — Inspection Multimodale BD ORTHO 512×512 × Métriques BD TOPO V3")
    b.add_card(s, Inches(8.45), BODY_TOP, Inches(4.28), BODY_H,
               badge="EXPLICABILITÉ IA", title="Voir ce que « Voit » l'Agent", accent_color=G_RED_500,
               bullets=[
                   ("Preuve visuelle : ", "Tuile exacte analysée par Gemini à l'Acte 3."),
                   ("Métriques WFS : ", "Nombre de bâtiments, hauteur max et pente."),
                   ("Décision humaine : ", "Facilite la validation des signalements IGN.")
               ])

    # ==========================================================================
    # PARTIE 4 : LANGCHAIN4J & DÉMO ACTE 1 (COPILOTE URBANISME) (Slides 33 à 38)
    # ==========================================================================
    b.add_section_divider(
        "PARTIE 4",
        "LangChain4j &\nDémo Acte 1 (Urbanisme ZAN)",
        "L'approche déclarative par interfaces Java (AiServices), le RAG local en mémoire (ONNX) et leur application à l'instruction automatique d'urbanisme.",
        [
            "LangChain4j : AiServices déclaratif",
            "Module Gemini 3.8 Flash & Mode Thinking",
            "RAG local 100 % Java en mémoire (ONNX)",
            "🎬 Démo Acte 1 : Copilote Urbanisme PLU & ZAN"
        ],
        accent_color=G_BLUE_500
    )

    # SLIDE 34 : LangChain4j — Philosophie Déclarative (AiServices)
    s = b.new_slide("PARTIE 4 • LANGCHAIN4J", "LangChain4j : L'Approche Déclarative avec `AiServices`",
                    "Définissez une simple interface Java : LangChain4j génère toute la boucle d'orchestration", G_BLUE_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(7.1), BODY_H,
                   "Interface Java déclarative -> Agent IA opérationnel", [
                       "interface UrbanismCopilot {",
                       "  @SystemMessage(\"\"\"",
                       "    Tu es un instructeur d'urbanisme expert IGN.",
                       "    Vérifie systématiquement le PLU et les données terrain.",
                       "    \"\"\")",
                       "  String analyzeParcel(String userRequest);",
                       "}",
                       "",
                       "UrbanismCopilot copilot = AiServices.builder(UrbanismCopilot.class)",
                       "  .chatModel(geminiModel)",
                       "  .chatMemory(MessageWindowChatMemory.withMaxMessages(20))",
                       "  .contentRetriever(pluRetriever)",
                       "  .tools(new GeoplateformeTools())",
                       "  .build();"
                   ])
    b.add_card(s, Inches(7.95), BODY_TOP, Inches(4.78), BODY_H,
               badge="ZÉRO BOILERPLATE", title="Ce que gère `AiServices`", accent_color=G_BLUE_500,
               bullets=[
                   ("Proxy dynamique : ", "Transforme l'interface en agent conversationnel."),
                   ("Boucle d'outils : ", "Appelle les méthodes @Tool automatiquement."),
                   ("RAG transparent : ", "Injecte les extraits du PLU à la volée.")
               ])

    # SLIDE 35 : LangChain4j + Gemini 3.8 Flash (Thinking & Résilience)
    s = b.new_slide("PARTIE 4 • LANGCHAIN4J + GEMINI", "Module `langchain4j-google-genai` & Gemini 3.8 Flash",
                    "Tirer parti des signatures de pensée de `gemini-3.8-flash` et gérer proprement les erreurs d'outils", G_BLUE_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(7.1), BODY_H,
                   "Configuration GoogleAiGeminiChatModel (gemini-3.8-flash)", [
                       "ChatModel model = GoogleAiGeminiChatModel.builder()",
                       "  .apiKey(System.getenv(\"GEMINI_API_KEY\"))",
                       "  .modelName(\"gemini-3.8-flash\")",
                       "  // Indispensable pour les thought_signature de Gemini 3.8 :",
                       "  .returnThinking(true)",
                       "  .sendThinking(true)",
                       "  .build();",
                       "",
                       "AiServices.builder(UrbanismCopilot.class)",
                       "  .chatModel(model)",
                       "  .toolArgumentsErrorHandler(",
                       "    ToolArgumentsErrorHandler.sendExceptionMessageToLlm())",
                       "  .build();"
                   ])
    b.add_card(s, Inches(7.95), BODY_TOP, Inches(4.78), BODY_H,
               badge="GEMINI 3.8 FLASH", title="2 Réglages Essentiels", accent_color=G_YELLOW_500,
               bullets=[
                   ("Thought Signatures : ", "returnThinking et sendThinking maintiennent le raisonnement multi-étapes."),
                   ("Error Handlers : ", "Renvoie l'erreur OGC au LLM pour qu'il se corrige.")
               ])

    # SLIDE 36 : LangChain4j — RAG Local 100 % Java (ONNX)
    s = b.new_slide("PARTIE 4 • RAG LOCAL ONNX", "RAG Local en Mémoire : Zéro Base Externe",
                    "Vectoriser un règlement PLU directement dans la JVM grâce au modèle ONNX embarqué", G_BLUE_500)
    b.add_diagram_node(s, MARGIN_X, BODY_TOP, Inches(3.5), Inches(1.25),
                       "1. Document Markdown", "plu-saint-mande-extrait.md\nDécoupé en segments",
                       bg_color=G_YELLOW_TINT, border_color=G_YELLOW_500)
    b.add_arrow(s, Inches(4.25), BODY_TOP + Inches(0.48), Inches(0.6), Inches(0.3), "right", G_BLUE_500)
    b.add_diagram_node(s, Inches(5.0), BODY_TOP, Inches(3.7), Inches(1.25),
                       "2. Modèle ONNX Local", "AllMiniLmL6V2Quantized\nTourne sur CPU dans la JVM",
                       bg_color=G_BLUE_TINT, border_color=G_BLUE_500)
    b.add_arrow(s, Inches(8.85), BODY_TOP + Inches(0.48), Inches(0.6), Inches(0.3), "right", G_GREEN_500)
    b.add_diagram_node(s, Inches(9.6), BODY_TOP, Inches(3.133), Inches(1.25),
                       "3. InMemoryEmbeddingStore", "Recherche par similarité\ncosinus instantanée",
                       bg_color=G_GREEN_TINT, border_color=G_GREEN_500)

    b.add_code_box(s, MARGIN_X, Inches(3.65), CONTENT_W, Inches(3.00),
                   "Indexation RAG locale en 8 lignes de Java", [
                       "EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();",
                       "EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();",
                       "",
                       "EmbeddingStoreIngestor.builder()",
                       "  .documentSplitter(DocumentSplitters.recursive(500, 80))",
                       "  .embeddingModel(embeddingModel).embeddingStore(store)",
                       "  .build().ingest(pluDocument);",
                       "",
                       "ContentRetriever retriever = EmbeddingStoreContentRetriever.from(store);"
                   ])

    # SLIDE 37 : 🎬 DÉMO ACTE 1 — Architecture (Copilote d'Urbanisme & ZAN)
    s = b.new_slide("🎬 DÉMO ACTE 1 • ARCHITECTURE (LANGCHAIN4J)", "Démo Acte 1 : Le Copilote d'Urbanisme « Text-to-GIS » & ZAN",
                    "Croiser automatiquement un règlement PLU local (RAG) avec 5 géoservices IGN temps réel", G_BLUE_500)
    steps_a1 = [
        ("1. Projet Soumis", "Immeuble R+5 (18 m)\n73 Av. de Paris, Saint-Mandé", G_YELLOW_500, G_YELLOW_TINT),
        ("2. UrbanismCopilot", "LangChain4j AiServices\nGemini 3.8 Flash", G_BLUE_500, G_BLUE_TINT),
        ("3. RAG PLU Local", "plu-saint-mande-extrait.md\nPlafonds UA/UB & ZAN", G_RED_500, G_RED_TINT),
        ("4. 5 Outils IGN", "Géocodage, Bâti, Végétation,\nRGE ALTI & Isochrone", G_GREEN_500, G_GREEN_TINT),
    ]
    for i, (t, sub, col, bg_c) in enumerate(steps_a1):
        x = MARGIN_X + Inches(i * 3.1)
        b.add_diagram_node(s, x, BODY_TOP, Inches(2.8), Inches(1.35), t, sub, bg_color=bg_c, border_color=col)
        if i < 3:
            b.add_arrow(s, x + Inches(2.84), BODY_TOP + Inches(0.52), Inches(0.22), Inches(0.28), "right", col)

    b.add_card(s, MARGIN_X, Inches(3.85), CONTENT_W, Inches(2.80),
               title="Le défi métier : détecter une subtilité réglementaire invisible sans SIG",
               accent_color=G_BLUE_500,
               bullets=[
                   ("Règle PLU (RAG) : ", "Hauteur max de 21 m en Zone UA, mais plafonnée à 15 m à moins de 150 m d'une forêt."),
                   ("Vérification WFS : ", "L'agent détecte le Bois de Vincennes en BD TOPO et rejette le projet de 18 m !")
               ])

    # SLIDE 38 : 🎬 DÉMO ACTE 1 — Résultat d'Exécution
    s = b.new_slide("🎬 DÉMO ACTE 1 • RÉSULTAT D'INSTRUCTION", "Démo Acte 1 : Rapport Généré par Gemini 3.8 Flash",
                    "Après 5 appels d'outils autonomes, l'agent identifie la non-conformité de hauteur", G_BLUE_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(7.3), BODY_H,
                   "Sortie Console (_01_LangChain4jUrbanismCopilot.java)", [
                       "📋 RAPPORT D'INSTRUCTION — 73 AVENUE DE PARIS, SAINT-MANDÉ",
                       "",
                       "1. CONTEXTE BÂTI & TOPOGRAPHIE (BD TOPO V3 & RGE ALTI)",
                       "   • 51 bâtiments dans un rayon de 100m (hauteur max 28.6m)",
                       "   • Altitude : 51.99m NGF | Pente : 1.44% (< seuil 3% PLU)",
                       "",
                       "2. CONFORMITÉ PLU & LOI CLIMAT / ZAN",
                       "   ❌ ALERTE HAUTEUR (LISIÈRE DU BOIS DE VINCENNES) :",
                       "    Zone arborée détectée à < 150m via query_bdtopo_environment.",
                       "    Plafond réduit à 15m (R+4) selon l'Art. 2.2 du PLU.",
                       "    -> Le projet proposé à 18m (R+5) DÉPASSE de 3 mètres !",
                       "",
                       "   ✅ GESTION DES EAUX : Pente 1.44% -> Bassin de rétention non requis.",
                       "   ✅ OBJECTIF ZAN : 51 bâtiments (>15) & 35% pleine terre."
                   ])
    b.add_card(s, Inches(8.15), BODY_TOP, Inches(4.58), BODY_H,
               badge="VALEUR AJOUTÉE", title="De 2 Heures à 15 Secondes", accent_color=G_GREEN_500,
               bullets=[
                   ("Zéro hallucination : ", "Chaque mesure provient d'un flux WFS ou RGE ALTI réel."),
                   ("Texte × SIG : ", "Relie l'article du PLU à la géométrie du Bois de Vincennes.")
               ])

    # ==========================================================================
    # PARTIE 5 : GOOGLE ADK FOR JAVA & DÉMO ACTE 2 (CELLULE DE CRISE) (Slides 39 à 45)
    # ==========================================================================
    b.add_section_divider(
        "PARTIE 5",
        "Google ADK for Java &\nDémo Acte 2 (Cellule de Crise)",
        "L'orchestration multi-agents déterministe (Sequential, Parallel, Loop), le partage d'état SessionState et leur application à la gestion de crise.",
        [
            "Architecture Google ADK for Java",
            "Les 4 patterns : Llm, Sequential, Parallel & Loop",
            "SessionState ({cle}) & ADK Dev UI Web",
            "🎬 Démo Acte 2 : Cellule de Crise à 5 Agents"
        ],
        accent_color=G_GREEN_500
    )

    # SLIDE 40 : Google ADK for Java — Vue d'ensemble
    s = b.new_slide("PARTIE 5 • GOOGLE ADK FOR JAVA", "Google ADK (Agent Development Kit) for Java",
                    "Le framework open-source de Google conçu pour l'orchestration multi-agents déterministe", G_GREEN_500)
    b.layout_two_cards(s,
        dict(badge="POURQUOI ADK ?", title="Quand un seul agent ne suffit plus", accent_color=G_GREEN_500, bullets=[
            ("Spécialisation : ", "Découper un problème complexe en agents experts."),
            ("Flux déterministe : ", "Maîtriser l'ordre et le parallélisme d'exécution."),
            ("Dev UI intégrée : ", "Inspecter visuellement les événements et l'état.")
        ]),
        dict(badge="BRIQUES FONDAMENTALES", title="L'Architecture ADK", accent_color=G_BLUE_500, bullets=[
            ("LlmAgent : ", "Agent piloté par Gemini 3.8 Flash + FunctionTool."),
            ("Workflow Agents : ", "SequentialAgent, ParallelAgent et LoopAgent."),
            ("Runner & Session : ", "Gère le flux d'événements et le SessionState.")
        ])
    )

    # SLIDE 41 : Google ADK — Les 4 Patterns d'Orchestration
    s = b.new_slide("PARTIE 5 • ORCHESTRATION ADK", "Les 4 Patterns d'Orchestration Multi-Agents d'ADK",
                    "Assembler des agents comme des briques Lego pour modéliser des processus métier complexes", G_GREEN_500)
    b.layout_four_columns(s, [
        dict(badge="1. LLM AGENT", title="Agent Individuel", accent_color=G_BLUE_500, bullets=[
            ("Raisonnement : ", "Exécute ses outils @Schema."),
            ("Sortie d'état : ", "Écrit dans outputKey.")
        ]),
        dict(badge="2. SEQUENTIAL", title="Pipeline (A → B → C)", accent_color=G_GREEN_500, bullets=[
            ("Ordre strict : ", "Enchaîne les étapes."),
            ("Relais : ", "Passe le SessionState.")
        ]),
        dict(badge="3. PARALLEL", title="Concurrent (A ∥ B)", accent_color=G_YELLOW_500, bullets=[
            ("Simultané : ", "Lance N experts en même temps."),
            ("Gain : ", "Divise la latence globale.")
        ]),
        dict(badge="4. LOOP & SUB", title="Boucle & Routage", accent_color=G_RED_500, bullets=[
            ("LoopAgent : ", "Répète jusqu'à validation."),
            ("Sub-agents : ", "Délégation dynamique.")
        ]),
    ])

    # SLIDE 42 : Google ADK — Communication par SessionState
    s = b.new_slide("PARTIE 5 • ÉTAT DE SESSION ADK", "Communication Inter-Agents via `SessionState` (`{cle}`)",
                    "Un tableau blanc partagé où chaque agent lit les résultats amont et inscrit ses conclusions", G_GREEN_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(7.3), BODY_H,
                   "Passage de relais automatique via outputKey et {variable}", [
                       "// 1. L'agent amont écrit dans la clé 'localisation_sinistre'",
                       "LlmAgent locator = LlmAgent.builder()",
                       "  .name(\"agent-localisation\").model(\"gemini-3.8-flash\")",
                       "  .instruction(\"Géocode l'adresse du sinistre.\")",
                       "  .tools(FunctionTool.create(geoTools, \"geocodeAddress\"))",
                       "  .outputKey(\"localisation_sinistre\")",
                       "  .build();",
                       "",
                       "// 2. L'agent aval injecte {localisation_sinistre} dans son prompt !",
                       "LlmAgent topo = LlmAgent.builder()",
                       "  .name(\"agent-topographe\").model(\"gemini-3.8-flash\")",
                       "  .instruction(\"Analyse le relief pour : {localisation_sinistre}\")",
                       "  .tools(FunctionTool.create(geoTools, \"getElevationAndSlope\"))",
                       "  .outputKey(\"rapport_topographie\")",
                       "  .build();"
                   ])
    b.add_card(s, Inches(8.15), BODY_TOP, Inches(4.58), BODY_H,
               badge="DÉCOUPLAGE PROPRE", title="Flux de Données Typé", accent_color=G_GREEN_500,
               bullets=[
                   ("outputKey : ", "Sauvegarde la sortie de l'agent en session."),
                   ("Template {cle} : ", "Injecte dynamiquement la valeur dans les prompts suivants.")
               ])

    # SLIDE 43 : Google ADK — Dev UI Web & Écosystème
    s = b.new_slide("PARTIE 5 • OUTILLAGE ADK", "ADK Dev UI & Ouverture de l'Écosystème (Kotlin, MCP, A2A)",
                    "Déboguer visuellement vos graphes d'agents et connecter n'importe quel modèle ou serveur MCP", G_GREEN_500)
    b.layout_two_cards(s,
        dict(badge="ADK DEV UI (:8080)", title="Une ligne de code pour tout visualiser", accent_color=G_GREEN_500, bullets=[
            ("AdkWebServer.start(agent) : ", "Lance l'interface web d'inspection."),
            ("Graphe & Timeline : ", "Visualise les branches Sequential et Parallel."),
            ("SessionState live : ", "Inspecte les variables créées à chaque étape.")
        ]),
        dict(badge="INTEROPÉRABILITÉ", title="MCP, LangChain4j, A2A & Kotlin", accent_color=G_BLUE_500, bullets=[
            ("McpToolset : ", "Connexion native aux serveurs MCP."),
            ("Pont LangChain4j : ", "Permet d'utiliser tout modèle LangChain4j."),
            ("ADK for Kotlin : ", "DSL Kotlin idiomatique sur https://adk.dev.")
        ])
    )

    # SLIDE 44 : 🎬 DÉMO ACTE 2 — Architecture (Cellule de Crise Multi-Agents ADK)
    s = b.new_slide("🎬 DÉMO ACTE 2 • ARCHITECTURE (GOOGLE ADK)", "Démo Acte 2 : Cellule de Crise Multi-Agents (Google ADK)",
                    "Coordonner 5 agents spécialisés (`SequentialAgent` + `ParallelAgent`) en lisière de Forêt de Fontainebleau", G_GREEN_500)
    b.add_diagram_node(s, MARGIN_X, Inches(2.35), Inches(2.6), Inches(1.75),
                       "1. agent-localisation", "Géocode le sinistre\nÉcrit dans SessionState :\n{localisation_sinistre}",
                       bg_color=G_BLUE_TINT, border_color=G_BLUE_500)
    b.add_arrow(s, Inches(3.3), Inches(3.05), Inches(0.45), Inches(0.3), "right", G_GREEN_500)

    p_box = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(3.85), BODY_TOP, Inches(5.4), Inches(2.42))
    p_box.adjustments[0] = 0.04
    p_box.fill.solid()
    p_box.fill.fore_color.rgb = G_YELLOW_TINT
    p_box.line.color.rgb = G_YELLOW_500
    p_box.line.width = Pt(2)
    b.add_caption(s, Inches(4.0), BODY_TOP + Inches(0.06), Inches(5.1), Inches(0.28),
                  "2. ParallelAgent (Exécution simultanée des 3 experts)", align=PP_ALIGN.CENTER, bold=True, color=C_NAVY)

    b.add_diagram_node(s, Inches(4.05), BODY_TOP + Inches(0.40), Inches(5.0), Inches(0.56),
                       "agent-topographe  →  {rapport_topographie}", bg_color=C_WHITE, border_color=G_GREEN_500)
    b.add_diagram_node(s, Inches(4.05), BODY_TOP + Inches(1.06), Inches(5.0), Inches(0.56),
                       "agent-enjeux-bati  →  {rapport_enjeux}", bg_color=C_WHITE, border_color=G_BLUE_500)
    b.add_diagram_node(s, Inches(4.05), BODY_TOP + Inches(1.72), Inches(5.0), Inches(0.56),
                       "agent-logistique   →  {rapport_logistique}", bg_color=C_WHITE, border_color=G_RED_500)

    b.add_arrow(s, Inches(9.35), Inches(3.05), Inches(0.45), Inches(0.3), "right", G_GREEN_500)
    b.add_diagram_node(s, Inches(9.9), Inches(2.35), Inches(2.833), Inches(1.75),
                       "3. coordinateur-secours", "Fusionne les 4 clés\nRédige le Point de\nSituation Préfet / DOS",
                       bg_color=G_RED_TINT, border_color=G_RED_500)

    b.add_card(s, MARGIN_X, Inches(4.72), CONTENT_W, Inches(1.93),
               title="Pourquoi ce graphe d'orchestration est optimal en situation d'urgence ?",
               accent_color=G_GREEN_500,
               bullets=[
                   ("Latence divisée par 3 : ", "Topographie, bâti et isochrones sont calculés simultanément en parallèle.")
               ])

    # SLIDE 45 : 🎬 DÉMO ACTE 2 — Code & Résultat Opérationnel
    s = b.new_slide("🎬 DÉMO ACTE 2 • CODE & RÉSULTAT (ADK)", "Démo Acte 2 : Assemblage du Pipeline & Synthèse DOS",
                    "Un code déclaratif concis et une synthèse opérationnelle structurée pour les secours", G_GREEN_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(5.9), BODY_H,
                   "_02_AdkMultiAgentCrisisSimulation.java", [
                       "// 1. Regroupement des 3 experts en parallèle",
                       "ParallelAgent collecteTerritoriale = ParallelAgent.builder()",
                       "  .name(\"collecte-territoriale-parallele\")",
                       "  .subAgents(agentTopographe, agentEnjeuxBati, agentLogistique)",
                       "  .build();",
                       "",
                       "// 2. Pipeline séquentiel complet",
                       "SequentialAgent crisisPipeline = SequentialAgent.builder()",
                       "  .name(\"cellule-crise-ign\")",
                       "  .subAgents(",
                       "    agentLocalisation,",
                       "    collecteTerritoriale,",
                       "    agentCoordinateur)",
                       "  .build();"
                   ])
    b.add_code_box(s, Inches(6.833), BODY_TOP, Inches(5.9), BODY_H,
                   "Synthèse DOS Générée (Lisière Forêt de Fontainebleau)", [
                       "🚨 POINT DE SITUATION — FORÊT DE FONTAINEBLEAU (77300)",
                       "📍 Boulevard Magenta : lon=2.6951, lat=48.4002 (INSEE 77186)",
                       "",
                       "⛰️ TOPOGRAPHIE & HYDRO (RGE ALTI) :",
                       "   • Altitude 75.91m NGF | Pente 1.13% (Nord-Ouest -> Sud-Est)",
                       "   • Propagation thermique vers le Nord-Ouest",
                       "",
                       "🏢 ENJEUX BÂTIS & FORÊT (BD TOPO V3) :",
                       "   • 48 bâtiments exposés (25 résidentiels, max 16.5m / R+5)",
                       "   • Contact direct massif forestier & 6 cours d'eau",
                       "",
                       "🚒 LOGISTIQUE & DÉCISION DOS (Isochrone 5 min) :",
                       "   • Ligne d'arrêt lisière forestière & protection bâti Nord-Ouest."
                   ])

    # ==========================================================================
    # PARTIE 6 : SERVEURS MCP, SOUVERAINETÉ (LITERT-LM) & DÉMO ACTE 4 (Slides 46 à 52)
    # ==========================================================================
    b.add_section_divider(
        "PARTIE 6",
        "Serveurs MCP, Souveraineté\n& Démo Acte 4 (Double MCP)",
        "Exposer nos outils en serveur MCP STDIO en 20 lignes, chaîner deux serveurs MCP et exécuter l'agent 100 % en local avec LiteRT-LM et Gemma 4.",
        [
            "Serveur MCP STDIO Java en 20 lignes",
            "🎬 Démo Acte 4 : Double Serveur MCP & WFS",
            "Cartes générées : Lyon & Toulouse",
            "Souveraineté : LiteRT-LM On-Device avec Gemma 4",
            "Synthèse Comparative & Ressources"
        ],
        accent_color=G_YELLOW_500
    )

    # SLIDE 47 : Serveur MCP STDIO en 20 Lignes
    s = b.new_slide("PARTIE 6 • SERVEUR MCP JAVA", "Serveur MCP STDIO « Zero Boilerplate » en Java",
                    "Exposer n'importe quelle classe annotée `@Tool` comme serveur MCP universel en 20 lignes", G_GREEN_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(7.2), BODY_H,
                   "IgnGeoplateformeMcpServer.java — Le serveur complet !", [
                       "public class IgnGeoplateformeMcpServer {",
                       "  public static void main(String[] args) throws Exception {",
                       "    // 1. Génère automatiquement tools/list et tools/call",
                       "    //    à partir des annotations @Tool / @P !",
                       "    McpServer server = new DefaultMcpServer(",
                       "      List.of(new GeoplateformeTools())",
                       "    );",
                       "",
                       "    // 2. Démarre le transport JSON-RPC sur stdin / stdout",
                       "    new StdioMcpServerTransport(server);",
                       "",
                       "    Thread.currentThread().join();",
                       "  }",
                       "}"
                   ])
    b.add_card(s, Inches(8.05), BODY_TOP, Inches(4.68), BODY_H,
               badge="RÈGLE D'OR STDIO", title="Garder `stdout` Propre !", accent_color=G_GREEN_500,
               bullets=[
                   ("Canal JSON-RPC : ", "stdout est réservé aux messages MCP."),
                   ("Logs sur stderr : ", "Redirigez SLF4J vers System.err et utilisez mvn -q."),
                   ("Zéro schéma manuel : ", "Tout est généré depuis @Tool et @P.")
               ])

    # SLIDE 48 : 🎬 DÉMO ACTE 4 — Architecture Double Serveur MCP
    s = b.new_slide("🎬 DÉMO ACTE 4 • ARCHITECTURE DOUBLE MCP", "Démo Acte 4 : Chaînage de 2 Serveurs MCP & Découverte WFS",
                    "Connecter notre serveur Java (`ign-geoplateforme`) au serveur cartographique (`ign-mcp-carto`)", G_GREEN_500)
    steps_a4 = [
        ("1. search_wfs_layers", "Serveur MCP #1 (Java)\nDécouvre les couches\ndans GetCapabilities", G_BLUE_500, G_BLUE_TINT),
        ("2. describe_wfs_schema", "Serveur MCP #1 (Java)\nLit le XSD : trouve\ngeom, numero, section", G_YELLOW_500, G_YELLOW_TINT),
        ("3. query_any_wfs_layer", "Serveur MCP #1 (Java)\nExécute le filtre CQL et\ncrée wfs_geojson_url", G_RED_500, G_RED_TINT),
        ("4. show_plan_ign_map", "Serveur MCP #2 (Node)\nSuperpose le GeoJSON\nsur Plan IGN V2 (PNG)", G_GREEN_500, G_GREEN_TINT),
    ]
    for i, (t, sub, col, bg_c) in enumerate(steps_a4):
        x = MARGIN_X + Inches(i * 3.1)
        b.add_diagram_node(s, x, BODY_TOP, Inches(2.8), Inches(1.35), t, sub, bg_color=bg_c, border_color=col)
        if i < 3:
            b.add_arrow(s, x + Inches(2.84), BODY_TOP + Inches(0.52), Inches(0.22), Inches(0.28), "right", col)

    b.add_card(s, MARGIN_X, Inches(3.85), CONTENT_W, Inches(2.80),
               title="Le pont inter-serveurs : `wfs_geojson_url`",
               accent_color=G_GREEN_500,
               bullets=[
                   ("Passage par URL : ", "Le serveur Java renvoie l'URL GeoJSON WFS que l'agent transmet au serveur Carto."),
                   ("Auto-découverte : ", "L'agent explore les 64 couches WFS (cadastre, communes) sans code spécifique !")
               ])

    # SLIDE 49 : 🎬 DÉMO ACTE 4 — Carte Générée #1 (Lyon)
    s = b.new_slide("🎬 DÉMO ACTE 4 • RÉSULTAT CARTOGRAPHIQUE #1", "Carte Générée par MCP : Lyon 1er (Plan IGN V2)",
                    "Superposition automatique du bâti BD TOPO V3 sur le fond souverain Plan IGN V2", G_GREEN_500)
    b.add_fitted_image(s, "carte_rue_du_bat_d_argent_lyon_plan_ign.png", MARGIN_X, BODY_TOP, Inches(7.3), BODY_H,
                       caption="docs/images/carte_rue_du_bat_d_argent_lyon_plan_ign.png — 10 rue du Bât-d'Argent, Lyon")
    b.add_card(s, Inches(8.15), BODY_TOP, Inches(4.58), BODY_H,
               badge="CHAÎNAGE AUTOMATIQUE", title="Étapes exécutées par l'Agent", accent_color=G_GREEN_500,
               bullets=[
                   ("1. Géocodage : ", "10 rue du Bât-d'Argent (lon=4.8352, lat=45.7660)."),
                   ("2. Extraction WFS : ", "Emprises bâties dans un rayon de 120 m."),
                   ("3. Rendu Carto MCP : ", "Appel de show_plan_ign_map et export PNG.")
               ])

    # SLIDE 50 : 🎬 DÉMO ACTE 4 — Carte Générée #2 (Parcelles Cadastrales à Toulouse)
    s = b.new_slide("🎬 DÉMO ACTE 4 • RÉSULTAT CARTOGRAPHIQUE #2", "Introspection Cadastrale Dynamique : Toulouse (Haute-Garonne)",
                    "L'agent découvre la couche `PARCELLAIRE-EXPRESS:parcelle`, lit son XSD et affiche les parcelles", G_GREEN_500)
    b.add_fitted_image(s, "carte_rue_du_taur_toulouse_plan_ign.png", MARGIN_X, BODY_TOP, Inches(7.3), BODY_H,
                       caption="docs/images/carte_rue_du_taur_toulouse_plan_ign.png — Section cadastrale AD, Toulouse")
    b.add_card(s, Inches(8.15), BODY_TOP, Inches(4.58), BODY_H,
               badge="DÉCOUVERTE XSD", title="Sans Aucun Code Spécifique", accent_color=G_YELLOW_500,
               bullets=[
                   ("1. search_wfs_layers : ", "Trouve PARCELLAIRE-EXPRESS:parcelle."),
                   ("2. describe_wfs_schema : ", "Identifie la colonne géométrique geom."),
                   ("3. Rendu Cadastral : ", "Superpose les parcelles sur Plan IGN V2.")
               ])

    # SLIDE 51 : 🎬 DÉMO ACTE 4 — Souveraineté & Inférence Locale On-Device avec LiteRT-LM & Gemma 4
    s = b.new_slide("🎬 DÉMO ACTE 4 • SOUVERAINETÉ & MODÈLES OUVERTS", "Souveraineté : Inférence On-Device avec LiteRT-LM & Gemma 4",
                    "Exécuter l'agent et ses outils MCP 100 % en local avec le modèle open weights `gemma4-e2b`", G_GREEN_500)
    b.add_code_box(s, MARGIN_X, BODY_TOP, Inches(6.8), BODY_H,
                   "_04_McpGeoplateformeDemo.java (--litert avec gemma4-e2b)", [
                       "// Mode souverain 100% local On-Device via LiteRT-LM",
                       "if (useLiteRT) {",
                       "  String modelName = \"gemma4-e2b\"; // Modèle open weights",
                       "  LiteRTAgentConfig litertConfig = LiteRTAgentConfig.builder()",
                       "    .modelName(modelName)",
                       "    .port(9379)",
                       "    .backend(LiteRTAgentConfig.Backend.GPU) // GPU Metal",
                       "    .lightweight()",
                       "    .build();",
                       "",
                       "  // Configuration de l'agent pointant sur le serveur local",
                       "  configBuilder",
                       "    .modelName(litertConfig.getAgentConfig().getModelName())",
                       "    .baseUrl(litertConfig.getBaseUrl());",
                       "}"
                   ])
    b.add_card(s, Inches(7.65), BODY_TOP, Inches(5.08), BODY_H,
               badge="SOUVERAINETÉ TOTALE", title="Zéro Dépendance Cloud", accent_color=G_GREEN_500,
               bullets=[
                   ("Modèle Open Weights : ", "Gemma 4 (`gemma4-e2b`) s'exécute directement sur GPU local via LiteRT-LM."),
                   ("Aucun appel d'inférence externe : ", "Les données sensibles du territoire et les requêtes ne quittent jamais votre infrastructure."),
                   ("Mêmes outils MCP : ", "L'agent local pilote exactement les mêmes serveurs MCP STDIO (WFS et Plan IGN V2)."),
                   ("Commande d'exécution : ", "mvn exec:java -Dexec.mainClass=..._04_... -Dexec.args=\"--litert\"")
               ])

    # SLIDE 52 : Comparatif Synthétique des 3 SDKs (après avoir vu les 4 Actes !)
    s = b.new_slide("SYNTHÈSE • COMPARATIF DES 3 SDKS", "Quel Framework / SDK Java Choisir ?",
                    "Retour d'expérience après nos 4 Actes sur les mêmes outils Géoplateforme IGN", G_BLUE_500)
    b.layout_three_cards(s, [
        dict(badge="LANGCHAIN4J", title="Simplicité Déclarative", accent_color=G_BLUE_500, bullets=[
            ("Forces : ", "Interfaces AiServices & RAG ONNX local."),
            ("Usage : ", "Copilotes métier & serveurs MCP STDIO."),
            ("Démos : ", "Acte 1 (Urbanisme) & Serveur MCP.")
        ]),
        dict(badge="GOOGLE ADK", title="Orchestration Multi-Agents", accent_color=G_GREEN_500, bullets=[
            ("Forces : ", "Sequential, Parallel, Loop & Dev UI."),
            ("Usage : ", "Pipelines coordonnant plusieurs experts."),
            ("Démo : ", "Acte 2 (Cellule de Crise à 5 agents).")
        ]),
        dict(badge="ANTIGRAVITY SDK", title="Harnais Autonome Gouverné", accent_color=G_RED_500, bullets=[
            ("Forces : ", "Moteur Go, SKILL.md, Policies HITL & LiteRT."),
            ("Usage : ", "Agents gouvernés, multimodaux ou souverains."),
            ("Démos : ", "Acte 3 (VLM) & Acte 4 (MCP + Gemma 4).")
        ])
    ])

    # SLIDE 53 : CONCLUSION & RESSOURCES
    s = b.new_slide("CONCLUSION & RESSOURCES", "Récapitulatif & Liens pour Aller Plus Loin",
                    "Toutes les présentations, articles et documentations utilisés dans ce workshop", G_BLUE_500)
    b.layout_four_columns(s, [
        dict(badge="SPEAKERDECK", title="Présentations", accent_color=G_RED_500, bullets=[
            ("Profil : ", "speakerdeck.com/glaforge"),
            ("Sujets : ", "Antigravity, SDK Java & ADK")
        ]),
        dict(badge="ARTICLES", title="Blog glaforge.dev", accent_color=G_BLUE_500, bullets=[
            ("LangChain4j : ", "/tags/langchain4j/"),
            ("ADK & AGY : ", "/tags/antigravity/")
        ]),
        dict(badge="DOCS", title="Documentation", accent_color=G_GREEN_500, bullets=[
            ("Google ADK : ", "https://adk.dev"),
            ("Géoplateforme : ", "https://cartes.gouv.fr")
        ]),
        dict(badge="SYNTHÈSE", title="À Retenir", accent_color=G_YELLOW_500, bullets=[
            ("1 Socle Java : ", "GeoplateformeTools.java"),
            ("Cloud & Local : ", "Gemini 3.8 & Gemma 4"),
            ("Souveraineté : ", "LiteRT-LM 100% on-device")
        ]),
    ])

    out_path = "/Users/glaforge/Projects/IGN/docs/presentation-workshop-ign-progressive.pptx"
    b.prs.save(out_path)

    tmp_path = out_path + ".tmp"
    with zipfile.ZipFile(out_path, "r") as zin, zipfile.ZipFile(tmp_path, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            content = zin.read(item.filename)
            if item.filename == "ppt/theme/theme1.xml":
                text = content.decode("utf-8")
                text = text.replace(
                    '<a:majorFont><a:latin typeface="Calibri"/><a:ea typeface=""/><a:cs typeface=""/>',
                    f'<a:majorFont><a:latin typeface="{FONT_TITLE}"/><a:ea typeface="{FONT_TITLE}"/><a:cs typeface="{FONT_TITLE}"/>'
                )
                text = text.replace(
                    '<a:minorFont><a:latin typeface="Calibri"/><a:ea typeface=""/><a:cs typeface=""/>',
                    f'<a:minorFont><a:latin typeface="{FONT_BODY}"/><a:ea typeface="{FONT_BODY}"/><a:cs typeface="{FONT_BODY}"/>'
                )
                content = text.encode("utf-8")
            zout.writestr(item, content)
    os.replace(tmp_path, out_path)
    print(f"Progressive presentation saved successfully to {out_path} ({b.slide_num} slides)")


if __name__ == "__main__":
    build_presentation()

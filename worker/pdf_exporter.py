#!/usr/bin/env python3
"""
pdf_exporter.py — Publication-Quality Astrological Report PDF Generator

Converts Markdown astrological reports into professionally designed multi-page
PDF documents with custom typography, celestial royal color palette, styled tables,
callout boxes, running headers, and two-pass page numbering ("Page X of Y").

Supported inputs:
  - Any local markdown (.md) file path
  - Scan and batch-export all astrological reports located on the user's Desktop
"""

import sys
import os
import re
from pathlib import Path
from datetime import datetime

from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    PageBreak, KeepTogether, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfgen import canvas

# ── Palette Definitions ─────────────────────────────────────────────────────────
COLOR_PRIMARY   = colors.HexColor("#0f2b48")   # Deep Celestial Navy
COLOR_SECONDARY = colors.HexColor("#1b4b72")   # Sapphire Blue
COLOR_ACCENT    = colors.HexColor("#b8860b")   # Dark Goldenrod / Celestial Gold
COLOR_GOLD_BG   = colors.HexColor("#fbf7ed")   # Soft Ivory Gold
COLOR_TEXT      = colors.HexColor("#1e293b")   # Slate 800
COLOR_MUTED     = colors.HexColor("#64748b")   # Slate 500
COLOR_BORDER    = colors.HexColor("#cbd5e1")   # Slate 300
COLOR_TABLE_HDR = colors.HexColor("#173753")   # Deep Ocean Header
COLOR_ROW_ALT   = colors.HexColor("#f8fafc")   # Slate 50
COLOR_QUOTE_BG  = colors.HexColor("#f1f5f9")   # Slate 100
COLOR_DIVIDER   = colors.HexColor("#e2e8f0")   # Light Gray

# ── Numbered Two-Pass Canvas for "Page X of Y" and Running Header ──────────────
class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        page_w, page_h = A4

        # Skip header and footer on first page (cover page) if multi-page
        if self._pageNumber > 1:
            # Running Header
            self.setFont("Helvetica", 8)
            self.setFillColor(COLOR_MUTED)
            self.drawString(40, page_h - 28, "ASTROLOGICAL MASTER REPORT • CONFIDENTIAL LIFE ANALYSIS")
            self.setStrokeColor(COLOR_DIVIDER)
            self.setLineWidth(0.5)
            self.line(40, page_h - 32, page_w - 40, page_h - 32)

            # Running Footer
            self.line(40, 36, page_w - 40, 36)
            self.drawString(40, 24, "Swiss Ephemeris • Meeus Algorithms • Multi-Tradition Astrological Platform")
            page_text = f"Page {self._pageNumber} of {page_count}"
            self.drawRightString(page_w - 40, 24, page_text)

        self.restoreState()


# ── Markdown Parser to ReportLab Flowables ───────────────────────────────────────
def clean_inline_formatting(text):
    """Convert common markdown inline syntax to reportlab XML tags."""
    if not text:
        return ""
    # Escape raw ampersands not part of XML entities
    text = re.sub(r"&(?!amp;|lt;|gt;|quot;|#\d+;)", "&amp;", text)
    # Bold italic ***text*** -> <b><i>text</i></b>
    text = re.sub(r"\*\*\*(.*?)\*\*\*", r"<b><i>\1</i></b>", text)
    # Bold **text** -> <b>text</b>
    text = re.sub(r"\*\*(.*?)\*\*", r"<b>\1</b>", text)
    # Italic *text* or _text_ -> <i>text</i>
    text = re.sub(r"\*(.*?)\*", r"<i>\1</i>", text)
    text = re.sub(r"(?<!\w)_(.*?)_(?!\w)", r"<i>\1</i>", text)
    # Inline code `code` -> <font name="Courier">\1</font>
    text = re.sub(r"`(.*?)`", r'<font name="Courier" size="8" color="#0f2b48">\1</font>', text)
    return text.strip()


def parse_markdown_to_flowables(md_content, styles):
    """Converts a full markdown text document into ReportLab Flowables."""
    flowables = []
    lines = md_content.splitlines()
    i = 0
    total_lines = len(lines)

    in_table = False
    table_raw_rows = []

    while i < total_lines:
        line = lines[i]
        stripped = line.strip()

        # Check for Markdown Table Row
        if stripped.startswith("|") and stripped.endswith("|"):
            cells = [c.strip() for c in stripped.split("|")[1:-1]]
            # Check if this is divider row like |---|---|
            is_divider = all(re.match(r"^:?-+:?$", c) for c in cells)
            if not is_divider:
                table_raw_rows.append(cells)
            in_table = True
            i += 1
            continue
        elif in_table:
            # End of table block: flush table
            if table_raw_rows:
                flowables.append(build_table(table_raw_rows, styles))
                flowables.append(Spacer(1, 10))
                table_raw_rows = []
            in_table = False

        # Blank Line
        if not stripped:
            i += 1
            continue

        # Horizontal Rule (---, ***, ___)
        if re.match(r"^(-{3,}|\*{3,}|_{3,})$", stripped):
            flowables.append(Spacer(1, 4))
            flowables.append(HRFlowable(width="100%", thickness=1, color=COLOR_DIVIDER, spaceAfter=8, spaceBefore=4))
            i += 1
            continue

        # Heading 1 (# Title)
        if stripped.startswith("# "):
            title_text = clean_inline_formatting(stripped[2:])
            flowables.append(Spacer(1, 12))
            flowables.append(Paragraph(title_text, styles["DocTitle"]))
            flowables.append(HRFlowable(width="100%", thickness=2, color=COLOR_ACCENT, spaceAfter=12, spaceBefore=4))
            i += 1
            continue

        # Heading 2 (## Section)
        if stripped.startswith("## "):
            sec_text = clean_inline_formatting(stripped[3:])
            flowables.append(Spacer(1, 14))
            flowables.append(Paragraph(sec_text, styles["SectionHeading"]))
            flowables.append(HRFlowable(width="100%", thickness=0.75, color=COLOR_SECONDARY, spaceAfter=8, spaceBefore=2))
            i += 1
            continue

        # Heading 3 (### Sub-section)
        if stripped.startswith("### "):
            subsec_text = clean_inline_formatting(stripped[4:])
            flowables.append(Spacer(1, 10))
            flowables.append(Paragraph(subsec_text, styles["SubSectionHeading"]))
            i += 1
            continue

        # Blockquote (> Callout)
        if stripped.startswith(">"):
            quote_text = clean_inline_formatting(stripped.lstrip(">").strip())
            flowables.append(Spacer(1, 4))
            # Wrap in callout box
            p = Paragraph(quote_text, styles["CalloutText"])
            t = Table([[p]], colWidths=[515])
            t.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, -1), COLOR_QUOTE_BG),
                ("BOX", (0, 0), (-1, -1), 0.5, COLOR_BORDER),
                ("LINELEFT", (0, 0), (0, -1), 3.5, COLOR_ACCENT),
                ("TOPPADDING", (0, 0), (-1, -1), 6),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
                ("LEFTPADDING", (0, 0), (-1, -1), 12),
                ("RIGHTPADDING", (0, 0), (-1, -1), 10),
            ]))
            flowables.append(t)
            flowables.append(Spacer(1, 6))
            i += 1
            continue

        # Bullet List (- Item or * Item)
        if re.match(r"^[-*•]\s+", stripped):
            bullet_text = clean_inline_formatting(re.sub(r"^[-*•]\s+", "", stripped))
            flowables.append(Paragraph(f"• &nbsp; {bullet_text}", styles["BulletItem"]))
            i += 1
            continue

        # Numbered List (1. Item)
        num_match = re.match(r"^(\d+)\.\s+(.*)$", stripped)
        if num_match:
            num_str, item_text = num_match.groups()
            formatted = clean_inline_formatting(item_text)
            flowables.append(Paragraph(f"<b>{num_str}.</b> &nbsp; {formatted}", styles["BulletItem"]))
            i += 1
            continue

        # Standard Paragraph
        p_text = clean_inline_formatting(stripped)
        flowables.append(Paragraph(p_text, styles["NormalBody"]))
        flowables.append(Spacer(1, 4))
        i += 1

    # Flush remaining table if file ends with table
    if in_table and table_raw_rows:
        flowables.append(build_table(table_raw_rows, styles))
        flowables.append(Spacer(1, 10))

    return flowables


def build_table(rows, styles):
    """Builds a formatted ReportLab Table with proper cell widths and styling."""
    if not rows:
        return Spacer(1, 1)

    col_count = max(len(r) for r in rows)
    # Normalize row lengths
    norm_rows = []
    for r in rows:
        padded = r + [""] * (col_count - len(r))
        norm_rows.append(padded)

    # Available printable width on A4 with 40pt margins = 595.27 - 80 = ~515pt
    total_width = 515
    col_w = total_width / col_count

    # Custom column width balancing
    if col_count == 2:
        col_widths = [140, 375]
    elif col_count == 3:
        col_widths = [120, 150, 245]
    elif col_count == 4:
        col_widths = [110, 120, 130, 155]
    elif col_count == 5:
        col_widths = [90, 100, 100, 110, 115]
    else:
        col_widths = [col_w] * col_count

    # Convert cell text into formatted Paragraphs
    table_data = []
    for row_idx, row in enumerate(norm_rows):
        formatted_row = []
        is_hdr = (row_idx == 0)
        cell_style = styles["TableHeader"] if is_hdr else styles["TableCell"]
        for cell in row:
            clean_cell = clean_inline_formatting(cell)
            formatted_row.append(Paragraph(clean_cell, cell_style))
        table_data.append(formatted_row)

    # Style table
    t = Table(table_data, colWidths=col_widths, repeatRows=1)
    t_style = [
        ("BACKGROUND", (0, 0), (-1, 0), COLOR_TABLE_HDR),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("ALIGN", (0, 0), (-1, -1), "LEFT"),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("BOX", (0, 0), (-1, -1), 0.5, COLOR_BORDER),
        ("INNERGRID", (0, 0), (-1, -1), 0.5, COLOR_DIVIDER),
    ]

    # Alternating row colors
    for r in range(1, len(norm_rows)):
        bg = COLOR_ROW_ALT if (r % 2 == 1) else colors.white
        t_style.append(("BACKGROUND", (0, r), (-1, r), bg))

    t.setStyle(TableStyle(t_style))
    return t


def setup_typography():
    """Initializes high-readability typographic styles."""
    styles = getSampleStyleSheet()

    styles.add(ParagraphStyle(
        "DocTitle",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=20,
        leading=24,
        textColor=COLOR_PRIMARY,
        alignment=0,
        spaceAfter=4,
        spaceBefore=8,
    ))

    styles.add(ParagraphStyle(
        "SectionHeading",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=13,
        leading=16,
        textColor=COLOR_PRIMARY,
        spaceBefore=10,
        spaceAfter=4,
        keepWithNext=True,
    ))

    styles.add(ParagraphStyle(
        "SubSectionHeading",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=10.5,
        leading=13,
        textColor=COLOR_SECONDARY,
        spaceBefore=8,
        spaceAfter=3,
        keepWithNext=True,
    ))

    styles.add(ParagraphStyle(
        "NormalBody",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=8.5,
        leading=11.5,
        textColor=COLOR_TEXT,
        spaceAfter=3,
    ))

    styles.add(ParagraphStyle(
        "CalloutText",
        parent=styles["Normal"],
        fontName="Helvetica-Oblique",
        fontSize=8.5,
        leading=11.5,
        textColor=COLOR_PRIMARY,
    ))

    styles.add(ParagraphStyle(
        "BulletItem",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=8.5,
        leading=11.5,
        textColor=COLOR_TEXT,
        leftIndent=10,
        spaceAfter=2,
    ))

    styles.add(ParagraphStyle(
        "TableHeader",
        parent=styles["Normal"],
        fontName="Helvetica-Bold",
        fontSize=8,
        leading=10,
        textColor=colors.white,
    ))

    styles.add(ParagraphStyle(
        "TableCell",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=7.5,
        leading=9.5,
        textColor=COLOR_TEXT,
    ))

    return styles


def export_markdown_to_pdf(input_path, output_path=None):
    """
    Converts a single Markdown file to a beautifully styled PDF document.
    Returns the absolute path of the generated PDF.
    """
    in_file = Path(input_path).expanduser().resolve()
    if not in_file.exists():
        raise FileNotFoundError(f"Input file not found: {in_file}")

    if output_path is None:
        out_file = in_file.with_suffix(".pdf")
    else:
        out_file = Path(output_path).expanduser().resolve()

    with open(in_file, "r", encoding="utf-8") as f:
        md_text = f.read()

    styles = setup_typography()
    doc = SimpleDocTemplate(
        str(out_file),
        pagesize=A4,
        leftMargin=40,
        rightMargin=40,
        topMargin=42,
        bottomMargin=42
    )

    flowables = parse_markdown_to_flowables(md_text, styles)
    doc.build(flowables, canvasmaker=NumberedCanvas)
    return str(out_file)


def scan_and_export_desktop():
    """
    Finds all astrological report files (*astrology*.md, *report*.md) on Desktop
    and converts them to high-resolution PDFs.
    Returns list of generated PDF paths.
    """
    desktop = Path.home() / "Desktop"
    if not desktop.exists():
        return []

    # Search for markdown reports
    patterns = ["*astrology*.md", "*report*.md", "*vardaan*.md", "*shubham*.md", "*suyash*.md"]
    found_files = set()
    for p in patterns:
        for f in desktop.glob(p):
            if f.is_file() and not f.name.startswith("."):
                found_files.add(f)

    generated = []
    for md_file in sorted(found_files):
        pdf_file = md_file.with_suffix(".pdf")
        try:
            print(f"Exporting: {md_file.name} -> {pdf_file.name}...")
            out = export_markdown_to_pdf(md_file, pdf_file)
            size_kb = round(os.path.getsize(out) / 1024, 1)
            print(f"  ✓ Created: {out} ({size_kb} KB)")
            generated.append(out)
        except Exception as e:
            print(f"  ✗ Failed to export {md_file.name}: {e}", file=sys.stderr)

    return generated


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] != "--desktop":
        target = sys.argv[1]
        out_target = sys.argv[2] if len(sys.argv) > 2 else None
        res = export_markdown_to_pdf(target, out_target)
        print(f"Successfully generated PDF: {res}")
    else:
        results = scan_and_export_desktop()
        print(f"\nDone! Exported {len(results)} report(s) to PDF on Desktop.")

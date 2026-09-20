"""
server.py – Astrological Media, Document Publishing & Computer Vision Microservice.
Provides ReportLab PDF document export, dynamic SVG chart rendering,
and OpenCV image inference for palmistry and facial physiognomy.
"""

import base64
import json
import os
import sys
import tempfile
from pathlib import Path
from typing import Any, Dict, Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response
from pydantic import BaseModel
import uvicorn

# Setup paths for worker modules
BASE_DIR = Path(__file__).resolve().parent
WORKER_DIR = BASE_DIR / "worker"
if not WORKER_DIR.exists():
    WORKER_DIR = BASE_DIR.parent.parent / "worker"

if str(WORKER_DIR) not in sys.path:
    sys.path.insert(0, str(WORKER_DIR))

app = FastAPI(
    title="Astro Media, PDF & Computer Vision Service",
    description="ReportLab PDF publishing, dynamic SVG charts, and OpenCV palmistry/face reading",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class PdfExportRequest(BaseModel):
    markdown_content: str
    filename: Optional[str] = "astrology_report.pdf"

class ChartSvgRequest(BaseModel):
    vedic: Dict[str, Any]
    style: Optional[str] = "north_indian"

class PalmistryRequest(BaseModel):
    image_base64: str
    hand_type: Optional[str] = "right"
    gender: Optional[str] = "male"
    current_age: Optional[int] = 30
    birth_data: Optional[Dict[str, Any]] = None

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "astro-media-export-service",
        "worker_dir": str(WORKER_DIR)
    }

@app.post("/media/chart-svg")
async def generate_chart_svg(req: ChartSvgRequest):
    try:
        from chart_svg import generate_all_charts
        svgs = generate_all_charts(req.vedic)
        return {"status": "ok", "charts": svgs}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"SVG generation failed: {str(e)}")

@app.post("/media/vision/palmistry")
async def analyze_palmistry(req: PalmistryRequest):
    try:
        from palmistry_vision_engine import analyze_palm_image
        result = analyze_palm_image(
            req.image_base64,
            req.hand_type or "right",
            req.gender or "male",
            req.current_age or 30,
            req.birth_data
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Palmistry vision failed: {str(e)}")

@app.post("/media/export-pdf")
async def export_pdf(req: PdfExportRequest):
    try:
        from pdf_exporter import export_markdown_to_pdf
        with tempfile.NamedTemporaryFile(suffix=".md", delete=False, mode="w", encoding="utf-8") as f_in:
            f_in.write(req.markdown_content)
            in_path = f_in.name

        out_path = in_path.replace(".md", ".pdf")
        export_markdown_to_pdf(in_path, out_path)

        with open(out_path, "rb") as f_pdf:
            pdf_bytes = f_pdf.read()

        try:
            os.remove(in_path)
            os.remove(out_path)
        except Exception:
            pass

        return Response(
            content=pdf_bytes,
            media_type="application/pdf",
            headers={"Content-Disposition": f"attachment; filename={req.filename or 'report.pdf'}"}
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"PDF export failed: {str(e)}")

if __name__ == "__main__":
    port = int(os.getenv("PORT", 8084))
    uvicorn.run("server:app", host="0.0.0.0", port=port, log_level="info")

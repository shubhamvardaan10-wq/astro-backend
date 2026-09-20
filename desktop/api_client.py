"""
Resilient Thread-Safe API Client for Astro-Backend Desktop Suite.
"""
import time
import json
import logging
import requests
import urllib3
from typing import Dict, Any, Optional, Tuple, Callable
from .config import (
    DEFAULT_INGRESS_URL,
    DEFAULT_GATEWAY_URL,
    DEFAULT_CALC_URL,
    DEFAULT_AI_URL,
    DEFAULT_MEDIA_URL,
    DEFAULT_API_KEY
)

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
logger = logging.getLogger("AstroApiClient")

class ApiResponse:
    def __init__(
        self,
        endpoint_id: str,
        name: str,
        category: str,
        method: str,
        url: str,
        request_body: Any = None,
        status_code: int = 0,
        expected_code: int = 200,
        duration_ms: int = 0,
        data: Any = None,
        error: Optional[str] = None
    ):
        self.endpoint_id = endpoint_id
        self.name = name
        self.category = category
        self.method = method
        self.url = url
        self.request_body = request_body
        self.status_code = status_code
        self.expected_code = expected_code
        self.duration_ms = duration_ms
        self.data = data
        self.error = error
        self.passed = (status_code == expected_code)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "id": self.endpoint_id,
            "name": self.name,
            "category": self.category,
            "method": self.method,
            "url": self.url,
            "status_code": self.status_code,
            "expected_code": self.expected_code,
            "duration_ms": self.duration_ms,
            "passed": self.passed,
            "error": self.error,
            "data_preview": str(self.data)[:200] if self.data else None
        }


class AstroApiClient:
    def __init__(
        self,
        ingress_url: str = DEFAULT_INGRESS_URL,
        gateway_url: str = DEFAULT_GATEWAY_URL,
        calc_url: str = DEFAULT_CALC_URL,
        ai_url: str = DEFAULT_AI_URL,
        media_url: str = DEFAULT_MEDIA_URL,
        api_key: str = DEFAULT_API_KEY
    ):
        self.ingress_url = ingress_url.rstrip("/")
        self.gateway_url = gateway_url.rstrip("/")
        self.calc_url = calc_url.rstrip("/")
        self.ai_url = ai_url.rstrip("/")
        self.media_url = media_url.rstrip("/")
        self.api_key = api_key

        self.session = requests.Session()
        self.active_chat_session_id: Optional[str] = None
        self.last_provisioned_key: Optional[str] = None

    def update_config(self, ingress_url: str = None, api_key: str = None):
        if ingress_url:
            self.ingress_url = ingress_url.rstrip("/")
        if api_key:
            self.api_key = api_key

    def _resolve_url(self, raw_url: str) -> str:
        resolved = raw_url.replace("{ingress}", self.ingress_url)
        resolved = resolved.replace("{gateway}", self.gateway_url)
        resolved = resolved.replace("{calc}", self.calc_url)
        resolved = resolved.replace("{ai}", self.ai_url)
        resolved = resolved.replace("{media}", self.media_url)
        if "{sessionId}" in resolved:
            sess = self.active_chat_session_id or "sess_desktop_demo"
            resolved = resolved.replace("{sessionId}", sess)
        return resolved

    def execute_catalog_endpoint(self, endpoint_def: Dict[str, Any]) -> ApiResponse:
        raw_url = endpoint_def.get("url", "")
        url = self._resolve_url(raw_url)
        method = endpoint_def.get("method", "GET").upper()
        expected_code = endpoint_def.get("expected_code", 200)
        skip_auth = endpoint_def.get("skip_auth", False)
        custom_key = endpoint_def.get("api_key")
        body = endpoint_def.get("body")

        # Dynamically inject active chat session if required
        if endpoint_def.get("requires_session") and isinstance(body, dict):
            body = dict(body)
            if self.active_chat_session_id:
                body["sessionId"] = self.active_chat_session_id

        headers = {
            "Accept": "application/json",
            "User-Agent": "AstroEnterpriseDesktop/1.0"
        }

        if not skip_auth:
            headers["X-API-Key"] = custom_key if custom_key is not None else self.api_key

        start_t = time.perf_counter()
        status_code = 0
        resp_data = None
        err_msg = None

        try:
            if method == "POST":
                headers["Content-Type"] = "application/json; charset=utf-8"
                resp = self.session.post(
                    url,
                    json=body if body is not None else {},
                    headers=headers,
                    timeout=20,
                    verify=False
                )
            else:
                resp = self.session.get(
                    url,
                    headers=headers,
                    timeout=20,
                    verify=False
                )

            duration_ms = int((time.perf_counter() - start_t) * 1000)
            status_code = resp.status_code

            try:
                resp_data = resp.json()
            except Exception:
                resp_data = resp.text

            # Extract any state mutations for chaining
            if isinstance(resp_data, dict):
                if "sessionId" in resp_data and resp_data["sessionId"]:
                    self.active_chat_session_id = resp_data["sessionId"]
                if "apiKey" in resp_data and resp_data["apiKey"]:
                    self.last_provisioned_key = resp_data["apiKey"]

        except requests.exceptions.RequestException as e:
            duration_ms = int((time.perf_counter() - start_t) * 1000)
            err_msg = str(e)

        return ApiResponse(
            endpoint_id=endpoint_def.get("id", ""),
            name=endpoint_def.get("name", "Unknown"),
            category=endpoint_def.get("category", "General"),
            method=method,
            url=url,
            request_body=body,
            status_code=status_code,
            expected_code=expected_code,
            duration_ms=duration_ms,
            data=resp_data,
            error=err_msg
        )

    # ── High Level Domain Methods ─────────────────────────────────────────────

    def send_chat_message(
        self,
        message: str,
        birth: Optional[Dict[str, Any]] = None,
        session_id: Optional[str] = None,
        language: str = "en"
    ) -> Dict[str, Any]:
        url = f"{self.ingress_url}/api/astro/chat"
        payload = {
            "message": message,
            "language": language
        }
        if session_id or self.active_chat_session_id:
            payload["sessionId"] = session_id or self.active_chat_session_id
        if birth:
            payload["birth"] = birth

        headers = {
            "X-API-Key": self.api_key,
            "Content-Type": "application/json"
        }
        r = self.session.post(url, json=payload, headers=headers, timeout=25, verify=False)
        data = r.json()
        if "sessionId" in data and data["sessionId"]:
            self.active_chat_session_id = data["sessionId"]
        return data

    def get_chat_history(self, session_id: Optional[str] = None) -> Dict[str, Any]:
        sid = session_id or self.active_chat_session_id
        if not sid:
            return {"sessionId": None, "turns": []}
        url = f"{self.ingress_url}/api/astro/chat/history/{sid}"
        headers = {"X-API-Key": self.api_key}
        r = self.session.get(url, headers=headers, timeout=15, verify=False)
        return r.json()

    def get_timeline_forecast(
        self,
        dob: str,
        tob: str,
        city: str,
        horizon_months: int = 12
    ) -> Dict[str, Any]:
        url = f"{self.ingress_url}/api/astro/timeline-forecast"
        headers = {"X-API-Key": self.api_key, "Content-Type": "application/json"}
        payload = {
            "dob": dob,
            "time": tob,
            "city": city,
            "horizonMonths": horizon_months
        }
        r = self.session.post(url, json=payload, headers=headers, timeout=25, verify=False)
        return r.json()

    def get_vedic_chart(self, dob: str, tob: str, city: str) -> Dict[str, Any]:
        url = f"{self.ingress_url}/api/astro/vedic-chart"
        headers = {"X-API-Key": self.api_key, "Content-Type": "application/json"}
        payload = {"dob": dob, "time": tob, "city": city}
        r = self.session.post(url, json=payload, headers=headers, timeout=20, verify=False)
        return r.json()

    def get_panchangam(self, lat: float, lon: float, date_str: str) -> Dict[str, Any]:
        url = f"{self.ingress_url}/api/astro/panchangam"
        headers = {"X-API-Key": self.api_key, "Content-Type": "application/json"}
        payload = {"latitude": lat, "longitude": lon, "date": date_str}
        r = self.session.post(url, json=payload, headers=headers, timeout=20, verify=False)
        return r.json()

    def get_transit_alerts(self, dob: str, tob: str, city: str, target_date: str) -> Dict[str, Any]:
        url = f"{self.ingress_url}/api/astro/transit-alerts"
        headers = {"X-API-Key": self.api_key, "Content-Type": "application/json"}
        payload = {"dob": dob, "time": tob, "city": city, "targetDate": target_date}
        r = self.session.post(url, json=payload, headers=headers, timeout=20, verify=False)
        return r.json()

    def rectify_birth_time(
        self,
        dob: str,
        tob: str,
        city: str,
        uncertainty_minutes: int,
        step_minutes: int,
        gender: str,
        life_events: list
    ) -> Dict[str, Any]:
        url = f"{self.ingress_url}/api/astro/birth-time-rectification"
        headers = {"X-API-Key": self.api_key, "Content-Type": "application/json"}
        payload = {
            "dob": dob,
            "time": tob,
            "city": city,
            "uncertaintyMinutes": uncertainty_minutes,
            "stepMinutes": step_minutes,
            "gender": gender,
            "lifeEvents": life_events
        }
        r = self.session.post(url, json=payload, headers=headers, timeout=30, verify=False)
        return r.json()

    def check_mesh_nodes(self) -> list:
        nodes = [
            ("Ingress HTTPS (:443)", f"{self.ingress_url}/health", "GET", None),
            ("Ingress HTTP (:80)", "http://localhost/health", "GET", None),
            ("Spring Gateway (:18080)", f"{self.gateway_url}/actuator/health", "GET", None),
            ("Calc Engine (:8081)", f"{self.calc_url}/health", "GET", None),
            ("AI RAG Service (:8083)", f"{self.ai_url}/health", "GET", None),
            ("Media Export Service (:8084)", f"{self.media_url}/health", "GET", None),
        ]
        results = []
        for name, url, method, body in nodes:
            t0 = time.perf_counter()
            status = "OFFLINE"
            latency = 0
            detail = ""
            try:
                r = self.session.get(url, timeout=3, verify=False)
                latency = int((time.perf_counter() - t0) * 1000)
                if r.status_code == 200:
                    status = "ONLINE"
                    detail = r.text[:80].replace("\n", " ")
                else:
                    status = f"HTTP {r.status_code}"
                    detail = r.text[:80]
            except Exception as e:
                latency = int((time.perf_counter() - t0) * 1000)
                detail = str(e)[:80]
            results.append({
                "service": name,
                "url": url,
                "status": status,
                "latency_ms": latency,
                "detail": detail
            })
        return results

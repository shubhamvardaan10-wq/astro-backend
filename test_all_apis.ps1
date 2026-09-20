# test_all_apis.ps1 - Master Commercial Release & Microservices Verification Suite
$ErrorActionPreference = "Continue"
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = {$true}
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12

$results = [System.Collections.Generic.List[PSCustomObject]]::new()
$ingressUrl = "https://localhost"
$gatewayUrl = "http://localhost:18080"
$calcUrl    = "http://localhost:8081"
$aiUrl      = "http://localhost:8083"
$mediaUrl   = "http://localhost:8084"

function Test-Endpoint {
    param (
        [string]$Category,
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [string]$Body = $null,
        [string]$ContentType = "application/json",
        [int]$ExpectedCode = 200,
        [string]$ApiKey = "ak_live_master_astro_2026",
        [switch]$SkipAuth
    )

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $status = "FAIL"
    $code = 0
    $detail = ""

    try {
        $headers = @{}
        if (-not $SkipAuth -and $ApiKey) {
            $headers["X-API-Key"] = $ApiKey
        }

        $params = @{
            Uri = $Url
            Method = $Method
            TimeoutSec = 20
            Headers = $headers
        }
        if ($Body) {
            $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($Body)
            $params["ContentType"] = "application/json; charset=utf-8"
        }

        $resp = $null
        try {
            $resp = Invoke-RestMethod @params
            $code = 200
        } catch {
            if ($_.Exception.Response) {
                $code = [int]$_.Exception.Response.StatusCode
            }
            # Fallback to curl.exe if PowerShell 5.1 TLS connection dropped on self-signed cert
            if ($code -eq 0 -or $_.Exception.Message -match "underlying connection was closed|unexpected error occurred") {
                $tmpFile = [System.IO.Path]::GetTempFileName()
                $curlArgs = @("-k", "-s", "-w", "`n%{http_code}", "-X", $Method, $Url)
                if (-not $SkipAuth -and $ApiKey) {
                    $curlArgs += @("-H", "X-API-Key: $ApiKey")
                }
                if ($Body) {
                    [System.IO.File]::WriteAllText($tmpFile, $Body, [System.Text.Encoding]::UTF8)
                    $curlArgs += @("-H", "Content-Type: $ContentType", "--data-binary", "@$tmpFile")
                }
                $curlOut = & curl.exe @curlArgs
                if (Test-Path $tmpFile) { Remove-Item $tmpFile -Force }
                if ($curlOut) {
                    $lines = $curlOut -split "`n"
                    $code = [int]($lines[-1].Trim())
                    $rawJson = ($lines[0..($lines.Length - 2)]) -join "`n"
                    if ($rawJson) {
                        try { $resp = $rawJson | ConvertFrom-Json } catch { $resp = $rawJson }
                    }
                }
            } else {
                throw $_
            }
        }

        $sw.Stop()

        if ($code -eq $ExpectedCode) {
            $status = "PASS"
        }

        if ($resp -is [PSCustomObject] -or $resp -is [hashtable]) {
            $keys = ($resp.PSObject.Properties.Name | Select-Object -First 4) -join ", "
            $detail = "Keys: [$keys]"
            if ($resp.apiKey) {
                $global:ProvisionedKey = $resp.apiKey
                $detail += " [Provisioned: $($resp.apiKey)]"
            }
            if ($resp.sessionId) {
                $global:ActiveChatSession = $resp.sessionId
            }
        } elseif ($resp -is [byte[]]) {
            $detail = "Binary [Bytes: $($resp.Length)]"
        } else {
            $str = if ($resp) { $resp.ToString().Trim() } else { "HTTP $code" }
            $detail = $str.Substring(0, [Math]::Min(50, $str.Length))
        }
    } catch {
        $sw.Stop()
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
        }
        if ($code -eq $ExpectedCode) {
            $status = "PASS"
            $detail = "Verified HTTP $code - " + ($_.Exception.Message -replace "`r`n", " ")
        } else {
            $detail = $_.Exception.Message
        }
    }

    $entry = [PSCustomObject]@{
        Category = $Category
        Endpoint = $Name
        Method   = $Method
        Status   = $status
        HTTP     = $code
        Duration = "$($sw.ElapsedMilliseconds)ms"
        Detail   = $detail
    }
    $results.Add($entry)
    
    $color = if ($status -eq "PASS") { "Green" } else { "Red" }
    Write-Host ("[{0}] {1,-6} {2,-36} -> {3,4} ({4,6}) | {5}" -f $status, $Method, $Name, $code, "$($sw.ElapsedMilliseconds)ms", $detail) -ForegroundColor $color
}

Write-Host "==========================================================================================" -ForegroundColor Cyan
Write-Host " EXECUTING MASTER COMMERCIAL RELEASE & MICROSERVICES TEST SUITE                           " -ForegroundColor Cyan
Write-Host "==========================================================================================" -ForegroundColor Cyan

# ── 1. Production Ingress & TLS Termination ──────────────────────────────────
Write-Host "`n[1. PRODUCTION INGRESS & REVERSE PROXY (PORTS 80/443)]" -ForegroundColor Yellow
Test-Endpoint -Category "Production Ingress" -Name "Ingress Port 80 Health" -Url "http://localhost/health" -SkipAuth
Test-Endpoint -Category "Production Ingress" -Name "Ingress Port 443 HTTPS Health" -Url "$ingressUrl/health" -SkipAuth
Test-Endpoint -Category "Production Ingress" -Name "Ingress Direct /calc/ Route" -Url "$ingressUrl/calc/capabilities" -SkipAuth
Test-Endpoint -Category "Production Ingress" -Name "Ingress Direct /ai/ Route" -Url "$ingressUrl/ai/rag/query" -Method "POST" -Body '{"query":"jupiter transit ascendant"}' -SkipAuth
Test-Endpoint -Category "Production Ingress" -Name "Ingress Direct /media/ Route" -Url "$ingressUrl/media/health" -SkipAuth

# ── 2. Direct Microservices Cluster Infrastructure ───────────────────────────
Write-Host "`n[2. DIRECT MICROSERVICES CLUSTER INFRASTRUCTURE]" -ForegroundColor Yellow
Test-Endpoint -Category "Microservice" -Name "Calc Engine Health" -Url "$calcUrl/health" -SkipAuth
Test-Endpoint -Category "Microservice" -Name "Calc Capabilities" -Url "$calcUrl/calc/capabilities" -SkipAuth
Test-Endpoint -Category "Microservice" -Name "Calc Execute (Ping)" -Url "$calcUrl/calc/execute" -Method "POST" -Body '{"action":"ping"}' -SkipAuth
Test-Endpoint -Category "Microservice" -Name "AI RAG Health" -Url "$aiUrl/health" -SkipAuth
Test-Endpoint -Category "Microservice" -Name "AI RAG Query" -Url "$aiUrl/ai/rag/query" -Method "POST" -Body '{"query":"jupiter transit ascendant"}' -SkipAuth
Test-Endpoint -Category "Microservice" -Name "Media Service Health" -Url "$mediaUrl/health" -SkipAuth

# ── 3. Security, Authentication & Rate Limiter Verification ──────────────────
Write-Host "`n[3. SECURITY, AUTHENTICATION & RATE LIMITING]" -ForegroundColor Yellow
$standardBirth = '{"dob":"1990-01-01","time":"12:00","city":"Delhi"}'
Test-Endpoint -Category "Security & Auth" -Name "Missing API Key (Reject 401)" -Url "$ingressUrl/api/astro/vedic-chart" -Method "POST" -Body $standardBirth -SkipAuth -ExpectedCode 401
Test-Endpoint -Category "Security & Auth" -Name "Invalid API Key (Reject 401)" -Url "$ingressUrl/api/astro/vedic-chart" -Method "POST" -Body $standardBirth -ApiKey "ak_live_invalid_fake_key" -ExpectedCode 401
Test-Endpoint -Category "Security & Auth" -Name "Master Key Auth (Accept 200)" -Url "$ingressUrl/api/astro/vedic-chart" -Method "POST" -Body $standardBirth -ApiKey "ak_live_master_astro_2026" -ExpectedCode 200

# ── 4. Commercial Monetization, Plans & Webhook Key Provisioning ─────────────
Write-Host "`n[4. COMMERCIAL MONETIZATION & WEBHOOK KEY PROVISIONING]" -ForegroundColor Yellow
Test-Endpoint -Category "Billing & Plans" -Name "Public Pricing Plans" -Url "$ingressUrl/api/billing/plans" -SkipAuth
Test-Endpoint -Category "Billing & Plans" -Name "API Key Usage Metering" -Url "$ingressUrl/api/billing/usage" -ApiKey "ak_live_master_astro_2026"

$stripePayload = '{"type":"checkout.session.completed","data":{"object":{"customer_email":"enterprise_client@example.com","metadata":{"tier":"PRO"}}}}'
Test-Endpoint -Category "Webhooks" -Name "Stripe Automated Provisioning" -Url "$ingressUrl/api/billing/webhook/stripe" -Method "POST" -Body $stripePayload -SkipAuth

$razorpayPayload = '{"event":"payment.captured","payload":{"payment":{"entity":{"email":"indian_startup@example.in","notes":{"tier":"STARTER"}}}}}'
Test-Endpoint -Category "Webhooks" -Name "Razorpay Automated Provisioning" -Url "$ingressUrl/api/billing/webhook/razorpay" -Method "POST" -Body $razorpayPayload -SkipAuth

if ($global:ProvisionedKey) {
    Test-Endpoint -Category "Provisioned Auth" -Name "Newly Provisioned Webhook Key" -Url "$ingressUrl/api/billing/usage" -ApiKey $global:ProvisionedKey
}

# ── 5. Gateway Core & Search ──────────────────────────────────────────────────
Write-Host "`n[5. API GATEWAY PLATFORM & SEARCH]" -ForegroundColor Yellow
Test-Endpoint -Category "Gateway" -Name "Actuator Health" -Url "$ingressUrl/actuator/health" -SkipAuth
Test-Endpoint -Category "Gateway" -Name "Actuator Info" -Url "$ingressUrl/actuator/info" -SkipAuth
Test-Endpoint -Category "Gateway" -Name "Classical Rules Search" -Url "$ingressUrl/api/astro/v2/search/rules?q=Sun+in+Aries&limit=5"

# ── 6. Core Ephemeris Calculation Endpoints (Via Secure Ingress) ──────────────
Write-Host "`n[6. CORE PURE-JAVA EPHEMERIS (VIA SECURE INGRESS)]" -ForegroundColor Yellow
Test-Endpoint -Category "Core Ephemeris" -Name "Vedic Chart" -Url "$ingressUrl/api/astro/vedic-chart" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Western Chart" -Url "$ingressUrl/api/astro/western-chart" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Vimshottari Dasha" -Url "$ingressUrl/api/astro/dasha" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Nakshatra Computation" -Url "$ingressUrl/api/astro/nakshatra" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Planetary Positions" -Url "$ingressUrl/api/astro/planets" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Yogas Detection" -Url "$ingressUrl/api/astro/yogas" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Life Prediction Synthesis" -Url "$ingressUrl/api/astro/prediction" -Method "POST" -Body $standardBirth

# ── 7. Decoupled Specialized Calculation Worker Expansions ───────────────────
Write-Host "`n[7. DECOUPLED SPECIALIZED CALCULATION WORKER EXPANSIONS]" -ForegroundColor Yellow
Test-Endpoint -Category "Worker Expansion" -Name "Full Panchangam" -Url "$ingressUrl/api/astro/panchangam" -Method "POST" -Body '{"latitude":28.6139,"longitude":77.2090,"date":"2026-09-21"}'
Test-Endpoint -Category "Worker Expansion" -Name "Dynamic SVG Chart" -Url "$ingressUrl/api/astro/chart-svg" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Ayurvedic Remedies" -Url "$ingressUrl/api/astro/remedies" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Medical Astrology" -Url "$ingressUrl/api/astro/medical" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Vastu Matrix" -Url "$ingressUrl/api/astro/vastu" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Destiny Curve" -Url "$ingressUrl/api/astro/destiny-curve" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Career Ikigai" -Url "$ingressUrl/api/astro/career-ikigai" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Numerology Tuning" -Url "$ingressUrl/api/astro/numerology-tuning" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Sade Sati Timeline" -Url "$ingressUrl/api/astro/sade-sati-timeline" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Kalasarpa Optimizer" -Url "$ingressUrl/api/astro/kalasarpa-optimizer" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Ashtakavarga Kaksha" -Url "$ingressUrl/api/astro/ashtakavarga-kaksha" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Varshaphala Annual" -Url "$ingressUrl/api/astro/varshaphala" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Jaimini Karakamsha" -Url "$ingressUrl/api/astro/jaimini-karakamsha" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Jaimini Chara Dasha" -Url "$ingressUrl/api/astro/jaimini-chara-dasha" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "KP Stellar Significators" -Url "$ingressUrl/api/astro/kp-significators" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "KP Horary 1-249" -Url "$ingressUrl/api/astro/kp-horary" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Bhrigu Nandi Nadi" -Url "$ingressUrl/api/astro/bhrigu-nandi-nadi" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Lal Kitab Debts" -Url "$ingressUrl/api/astro/lal-kitab" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Draconic Soul Chart" -Url "$ingressUrl/api/astro/draconic-chart" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Kota Chakra Defense" -Url "$ingressUrl/api/astro/kota-chakra" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Lo Shu Numerology Grid" -Url "$ingressUrl/api/astro/lo-shu-grid" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Sarvatobhadra Chakra" -Url "$ingressUrl/api/astro/sarvatobhadra-chakra" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Panch Pakshi Rhythm" -Url "$ingressUrl/api/astro/panch-pakshi" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Aura Chakra Alignment" -Url "$ingressUrl/api/astro/aura-chakra" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Sound Therapy Frequency" -Url "$ingressUrl/api/astro/sound-therapy" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Gemstone & Rudraksha" -Url "$ingressUrl/api/astro/gemstone-rudraksha" -Method "POST" -Body $standardBirth

# ── 8. Multi-Chart Matchmaking ────────────────────────────────────────────────
Write-Host "`n[8. MULTI-CHART MATCHMAKING]" -ForegroundColor Yellow
$matchmakingBody = '{"partner1":{"dob":"1990-01-01","time":"12:00","city":"Delhi"},"partner2":{"dob":"1992-05-15","time":"14:30","city":"Mumbai"}}'
Test-Endpoint -Category "Multi-Chart" -Name "36-Guna Kundli Milan" -Url "$ingressUrl/api/astro/matchmaking" -Method "POST" -Body $matchmakingBody
Test-Endpoint -Category "Multi-Chart" -Name "Dasa Koota South System" -Url "$ingressUrl/api/astro/dasa-koota-matching" -Method "POST" -Body $matchmakingBody

# ── 9. Melooha Parity & Superiority Features ─────────────────────────────────
Write-Host "`n[9. MELOOHA PARITY & COMPETITIVE SUPERIORITY FEATURES]" -ForegroundColor Yellow

# A. Stateful Conversational Chat (Turn 1: Marriage query with birth chart)
$chatTurn1 = '{"message":"When will I get married?","birth":{"dob":"1990-01-01","time":"12:00","city":"Delhi"},"language":"en"}'
Test-Endpoint -Category "Conversational AI" -Name "Chat Turn 1 (Marriage Grounding)" -Url "$ingressUrl/api/astro/chat" -Method "POST" -Body $chatTurn1

# B. Multi-Turn Followup (Turn 2: Followup question relying on session memory)
$chatSess = if ($global:ActiveChatSession) { $global:ActiveChatSession } else { "sess_test_123" }
$chatTurn2 = "{`"sessionId`":`"$chatSess`",`"message`":`"What about my wealth and promotion?`",`"language`":`"en`"}"
Test-Endpoint -Category "Conversational AI" -Name "Chat Turn 2 (Session Memory Recall)" -Url "$ingressUrl/api/astro/chat" -Method "POST" -Body $chatTurn2

# C. Multi-Lingual Hindi Chat Turn
$chatTurnHi = "{`"sessionId`":`"$chatSess`",`"message`":`"कैरियर में सफलता कब मिलेगी?`",`"language`":`"hi`"}"
Test-Endpoint -Category "Conversational AI" -Name "Chat Turn 3 (Native Hindi Consultation)" -Url "$ingressUrl/api/astro/chat" -Method "POST" -Body $chatTurnHi

# D. Retrieve Chat Thread History
Test-Endpoint -Category "Conversational AI" -Name "Retrieve Full Chat Thread" -Url "$ingressUrl/api/astro/chat/history/$chatSess"

# E. High-Granularity Month-by-Month Life Timing Forecast (When Will It Happen?)
$forecastBody = '{"dob":"1990-01-01","time":"12:00","city":"Delhi","horizonMonths":12}'
Test-Endpoint -Category "Life Event Timing" -Name "Month-by-Month Trajectory (12M)" -Url "$ingressUrl/api/astro/timeline-forecast" -Method "POST" -Body $forecastBody

# F. Vedic Birth Time Rectification (BTR) Assistant Engine
$btrBody = '{"dob":"1990-01-01","time":"12:00","city":"Delhi","uncertaintyMinutes":20,"stepMinutes":5,"gender":"MALE","lifeEvents":[{"eventType":"CAREER_BREAKTHROUGH","eventDate":"2015-06-01"}]}'
Test-Endpoint -Category "Birth Rectification" -Name "Automated BTR Scoring Engine" -Url "$ingressUrl/api/astro/birth-time-rectification" -Method "POST" -Body $btrBody

# G. Real-Time Proactive Transit Alarms & Webhooks
Test-Endpoint -Category "Daily Transit Alarms" -Name "Chandrashtama & Gochara Alarms" -Url "$ingressUrl/api/astro/transit-alerts" -Method "POST" -Body '{"dob":"1990-01-01","time":"12:00","city":"Delhi","targetDate":"2026-09-21"}'

# H. Native Multilingual Synthesis in Hindi
Test-Endpoint -Category "Localization i18n" -Name "Hindi Vedic Synthesis Report" -Url "$ingressUrl/api/astro/multilingual-report" -Method "POST" -Body '{"dob":"1990-01-01","time":"12:00","city":"Delhi","targetLanguage":"hi"}'

# ── Final Summary ─────────────────────────────────────────────────────────────
Write-Host "`n==========================================================================================" -ForegroundColor Cyan
Write-Host " ALL API TEST SUITE SUMMARY                                                               " -ForegroundColor Cyan
Write-Host "==========================================================================================" -ForegroundColor Cyan

$passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
$total  = $results.Count

Write-Host "TOTAL APIS TESTED   : $total" -ForegroundColor White
Write-Host "SUCCESSFUL (PASS)   : $passed" -ForegroundColor Green
Write-Host "FAILED              : $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })

$results | Format-Table Category, Endpoint, Method, Status, HTTP, Duration, Detail -AutoSize

if ($failed -gt 0) {
    exit 1
} else {
    exit 0
}

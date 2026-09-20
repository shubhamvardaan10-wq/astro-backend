# test_all_apis.ps1 - Comprehensive Automated Microservices Test Suite
$ErrorActionPreference = "Continue"

$results = [System.Collections.Generic.List[PSCustomObject]]::new()

function Test-Endpoint {
    param (
        [string]$Category,
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [string]$Body = $null,
        [string]$ContentType = "application/json"
    )

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $status = "FAIL"
    $code = 0
    $detail = ""

    try {
        $params = @{
            Uri = $Url
            Method = $Method
            TimeoutSec = 15
        }
        if ($Body) {
            $params["Body"] = $Body
            $params["ContentType"] = $ContentType
        }

        $resp = Invoke-RestMethod @params
        $sw.Stop()
        $status = "PASS"
        $code = 200

        if ($resp -is [PSCustomObject] -or $resp -is [hashtable]) {
            $keys = ($resp.PSObject.Properties.Name | Select-Object -First 4) -join ", "
            $detail = "Keys: [$keys]"
        } elseif ($resp -is [byte[]]) {
            $detail = "Binary [Bytes: $($resp.Length)]"
        } else {
            $str = $resp.ToString().Trim()
            $detail = $str.Substring(0, [Math]::Min(50, $str.Length))
        }
    } catch {
        $sw.Stop()
        $detail = $_.Exception.Message
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
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
    Write-Host ("[{0}] {1,-6} {2,-34} -> {3,4} ({4,6}) | {5}" -f $status, $Method, $Name, $code, "$($sw.ElapsedMilliseconds)ms", $detail) -ForegroundColor $color
}

Write-Host "==========================================================================================" -ForegroundColor Cyan
Write-Host " EXECUTING FULL-SUITE VERIFICATION ACROSS ALL MICROSERVICES & ENDPOINTS                   " -ForegroundColor Cyan
Write-Host "==========================================================================================" -ForegroundColor Cyan

# ── 1. Direct Microservices Endpoints ─────────────────────────────────────────
Write-Host "`n[1. DIRECT MICROSERVICES CLUSTER INFRASTRUCTURE]" -ForegroundColor Yellow
Test-Endpoint -Category "Microservice" -Name "Calc Engine Health" -Url "http://localhost:8081/health"
Test-Endpoint -Category "Microservice" -Name "Calc Capabilities" -Url "http://localhost:8081/calc/capabilities"
Test-Endpoint -Category "Microservice" -Name "Calc Execute (Ping)" -Url "http://localhost:8081/calc/execute" -Method "POST" -Body '{"action":"ping"}'
Test-Endpoint -Category "Microservice" -Name "AI RAG Health" -Url "http://localhost:8083/health"
Test-Endpoint -Category "Microservice" -Name "AI RAG Query" -Url "http://localhost:8083/ai/rag/query" -Method "POST" -Body '{"query":"jupiter transit ascendant"}'
Test-Endpoint -Category "Microservice" -Name "Media Service Health" -Url "http://localhost:8084/health"

# ── 2. Gateway Core & Search ──────────────────────────────────────────────────
Write-Host "`n[2. API GATEWAY PLATFORM & SEARCH]" -ForegroundColor Yellow
Test-Endpoint -Category "Gateway" -Name "Actuator Health" -Url "http://localhost:18080/actuator/health"
Test-Endpoint -Category "Gateway" -Name "Actuator Info" -Url "http://localhost:18080/actuator/info"
Test-Endpoint -Category "Gateway" -Name "Classical Rules Search" -Url "http://localhost:18080/api/astro/v2/search/rules?q=Sun+in+Aries&limit=5"

# ── 3. Core Meeus Ephemeris Calculation Endpoints ─────────────────────────────
Write-Host "`n[3. CORE PURE-JAVA EPHEMERIS (0ms PROCESS SPAWN)]" -ForegroundColor Yellow
$standardBirth = '{"dob":"1990-01-01","time":"12:00","city":"Delhi"}'
Test-Endpoint -Category "Core Ephemeris" -Name "Vedic Chart" -Url "http://localhost:18080/api/astro/vedic-chart" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Western Chart" -Url "http://localhost:18080/api/astro/western-chart" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Vimshottari Dasha" -Url "http://localhost:18080/api/astro/dasha" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Nakshatra Computation" -Url "http://localhost:18080/api/astro/nakshatra" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Planetary Positions" -Url "http://localhost:18080/api/astro/planets" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Yogas Detection" -Url "http://localhost:18080/api/astro/yogas" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Core Ephemeris" -Name "Life Prediction Synthesis" -Url "http://localhost:18080/api/astro/prediction" -Method "POST" -Body $standardBirth

# ── 4. Decoupled Specialized Calculation Worker Expansions ───────────────────
Write-Host "`n[4. DECOUPLED SPECIALIZED CALCULATION WORKER EXPANSIONS]" -ForegroundColor Yellow
Test-Endpoint -Category "Worker Expansion" -Name "Full Panchangam" -Url "http://localhost:18080/api/astro/panchangam" -Method "POST" -Body '{"latitude":28.6139,"longitude":77.2090,"date":"2026-09-21"}'
Test-Endpoint -Category "Worker Expansion" -Name "Dynamic SVG Chart" -Url "http://localhost:18080/api/astro/chart-svg" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Ayurvedic Remedies" -Url "http://localhost:18080/api/astro/remedies" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Medical Astrology" -Url "http://localhost:18080/api/astro/medical" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Vastu Matrix" -Url "http://localhost:18080/api/astro/vastu" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Destiny Curve" -Url "http://localhost:18080/api/astro/destiny-curve" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Career Ikigai" -Url "http://localhost:18080/api/astro/career-ikigai" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Numerology Tuning" -Url "http://localhost:18080/api/astro/numerology-tuning" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Sade Sati Timeline" -Url "http://localhost:18080/api/astro/sade-sati-timeline" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Kalasarpa Optimizer" -Url "http://localhost:18080/api/astro/kalasarpa-optimizer" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Ashtakavarga Kaksha" -Url "http://localhost:18080/api/astro/ashtakavarga-kaksha" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Varshaphala Annual" -Url "http://localhost:18080/api/astro/varshaphala" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Jaimini Karakamsha" -Url "http://localhost:18080/api/astro/jaimini-karakamsha" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Jaimini Chara Dasha" -Url "http://localhost:18080/api/astro/jaimini-chara-dasha" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "KP Stellar Significators" -Url "http://localhost:18080/api/astro/kp-significators" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "KP Horary 1-249" -Url "http://localhost:18080/api/astro/kp-horary" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Bhrigu Nandi Nadi" -Url "http://localhost:18080/api/astro/bhrigu-nandi-nadi" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Lal Kitab Debts" -Url "http://localhost:18080/api/astro/lal-kitab" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Draconic Soul Chart" -Url "http://localhost:18080/api/astro/draconic-chart" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Kota Chakra Defense" -Url "http://localhost:18080/api/astro/kota-chakra" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Lo Shu Numerology Grid" -Url "http://localhost:18080/api/astro/lo-shu-grid" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Sarvatobhadra Chakra" -Url "http://localhost:18080/api/astro/sarvatobhadra-chakra" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Panch Pakshi Rhythm" -Url "http://localhost:18080/api/astro/panch-pakshi" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Aura Chakra Alignment" -Url "http://localhost:18080/api/astro/aura-chakra" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Sound Therapy Frequency" -Url "http://localhost:18080/api/astro/sound-therapy" -Method "POST" -Body $standardBirth
Test-Endpoint -Category "Worker Expansion" -Name "Gemstone & Rudraksha" -Url "http://localhost:18080/api/astro/gemstone-rudraksha" -Method "POST" -Body $standardBirth

# ── 5. Multi-Chart Matchmaking ────────────────────────────────────────────────
Write-Host "`n[5. MULTI-CHART MATCHMAKING]" -ForegroundColor Yellow
$matchmakingBody = '{"partner1":{"dob":"1990-01-01","time":"12:00","city":"Delhi"},"partner2":{"dob":"1992-05-15","time":"14:30","city":"Mumbai"}}'
Test-Endpoint -Category "Multi-Chart" -Name "36-Guna Kundli Milan" -Url "http://localhost:18080/api/astro/matchmaking" -Method "POST" -Body $matchmakingBody
Test-Endpoint -Category "Multi-Chart" -Name "Dasa Koota South System" -Url "http://localhost:18080/api/astro/dasa-koota-matching" -Method "POST" -Body $matchmakingBody

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

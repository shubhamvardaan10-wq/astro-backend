# test_14_apis.ps1 - Master Verification Suite for the 14 Consolidated Enterprise APIs
$ErrorActionPreference = "Continue"
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12

$results = [System.Collections.Generic.List[PSCustomObject]]::new()
$gatewayUrl = "http://localhost:18080"
$standardBirth = '{"dob":"1990-01-15","time":"14:30","city":"Mumbai"}'
$matchmakingBody = '{"partner1":{"dob":"1990-01-01","time":"12:00","city":"Delhi"},"partner2":{"dob":"1992-05-15","time":"14:30","city":"Mumbai"}}'

function Test-Master-API {
    param (
        [int]$Number,
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [string]$Body = $null,
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
            TimeoutSec = 30
            Headers = $headers
        }
        if ($Body) {
            $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($Body)
            $params["ContentType"] = "application/json; charset=utf-8"
        }

        $resp = Invoke-RestMethod @params
        $code = 200
        $sw.Stop()

        if ($code -eq $ExpectedCode) {
            $status = "PASS"
        }

        if ($resp -is [PSCustomObject] -or $resp -is [hashtable]) {
            $keys = ($resp.PSObject.Properties.Name | Select-Object -First 4) -join ", "
            $detail = "Keys: [$keys]"
            if ($resp.estimatedWordCount) {
                $detail += " [WordCount: $($resp.estimatedWordCount)]"
            }
            if ($resp.sessionId) {
                $global:ActiveChatSession = $resp.sessionId
            }
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
        Number   = $Number
        Endpoint = $Name
        Method   = $Method
        Status   = $status
        HTTP     = $code
        Duration = "$($sw.ElapsedMilliseconds)ms"
        Detail   = $detail
    }
    $results.Add($entry)
    
    $color = if ($status -eq "PASS") { "Green" } else { "Red" }
    Write-Host ("[{0}] #{1,2} {2,-6} {3,-40} -> {4,4} ({5,6}) | {6}" -f $status, $Number, $Method, $Name, $code, "$($sw.ElapsedMilliseconds)ms", $detail) -ForegroundColor $color
}

Write-Host "==========================================================================================" -ForegroundColor Cyan
Write-Host " VERIFYING THE 14 CONSOLIDATED ENTERPRISE APIS (LEAN 2-SERVICE CLUSTER)                   " -ForegroundColor Cyan
Write-Host "==========================================================================================" -ForegroundColor Cyan

# ── 1. Cluster Telemetry & Health ────────────────────────────────────────────
Test-Master-API -Number 1 -Name "Cluster Telemetry & Health" -Url "$gatewayUrl/health" -SkipAuth

# ── 2. Classical Sanskrit Knowledge Base ─────────────────────────────────────
Test-Master-API -Number 2 -Name "Classical Knowledge & Rules Search" -Url "$gatewayUrl/api/astro/knowledge?q=Jupiter+in+Cancer&limit=5"

# ── 3. Commercial Quota & Webhook Management ─────────────────────────────────
Test-Master-API -Number 3 -Name "Commercial Plans & Key Management" -Url "$gatewayUrl/api/billing/manage" -SkipAuth

# ── 4. Master Natal Horoscope ────────────────────────────────────────────────
Test-Master-API -Number 4 -Name "Master Natal Horoscope (D1/Western/Yogas)" -Url "$gatewayUrl/api/astro/chart" -Method "POST" -Body $standardBirth

# ── 5. 5000-Word Comprehensive Prediction Engine ─────────────────────────────
Test-Master-API -Number 5 -Name "5000-Word Vedic Prediction Dossier" -Url "$gatewayUrl/api/astro/prediction" -Method "POST" -Body $standardBirth

# ── 6. Automated Birth Time Rectification (BTR) ──────────────────────────────
$btrBody = '{"dob":"1990-01-15","time":"14:30","city":"Mumbai","uncertaintyMinutes":20,"stepMinutes":5,"gender":"MALE","lifeEvents":[{"eventType":"CAREER_PROMOTION","eventDate":"2018-05-01"}]}'
Test-Master-API -Number 6 -Name "Automated Birth Time Rectification (BTR)" -Url "$gatewayUrl/api/astro/btr" -Method "POST" -Body $btrBody

# ── 7. Cosmic Clock & Panchangam ─────────────────────────────────────────────
$panchangBody = '{"date":"2026-09-21","latitude":19.076,"longitude":72.8777}'
Test-Master-API -Number 7 -Name "Cosmic Clock & 5-Limb Panchangam" -Url "$gatewayUrl/api/astro/panchangam" -Method "POST" -Body $panchangBody

# ── 8. Dynamic Transits, Gochara & Forecast ──────────────────────────────────
Test-Master-API -Number 8 -Name "Dynamic Transits, Gochara & Trajectory" -Url "$gatewayUrl/api/astro/transits" -Method "POST" -Body $standardBirth

# ── 9. Comprehensive Matchmaking Synastry ────────────────────────────────────
Test-Master-API -Number 9 -Name "36-Guna & Dasa Koota Matchmaking" -Url "$gatewayUrl/api/astro/matchmaking" -Method "POST" -Body $matchmakingBody

# ── 10. Conversational Astrologer AI ─────────────────────────────────────────
$chatBody = '{"message":"When will I achieve peak career and wealth?","birth":{"dob":"1990-01-15","time":"14:30","city":"Mumbai"},"language":"en"}'
Test-Master-API -Number 10 -Name "Conversational Astrologer AI (Multi-Turn)" -Url "$gatewayUrl/api/astro/chat" -Method "POST" -Body $chatBody

# ── 11. Specialized Astrological Systems ─────────────────────────────────────
Test-Master-API -Number 11 -Name "Specialized Systems (KP/Jaimini/LalKitab/Nadi)" -Url "$gatewayUrl/api/astro/systems" -Method "POST" -Body $standardBirth

# ── 12. Defensive & Soul Chakras ─────────────────────────────────────────────
Test-Master-API -Number 12 -Name "Defensive Chakras (Kota/Sarvato/Kalasarpa)" -Url "$gatewayUrl/api/astro/chakras" -Method "POST" -Body $standardBirth

# ── 13. Holistic Cosmic Life Matrix & Wellness ───────────────────────────────
Test-Master-API -Number 13 -Name "Life Matrix (Vastu/Ikigai/Numerology/Wellness)" -Url "$gatewayUrl/api/astro/life-matrix" -Method "POST" -Body $standardBirth

# ── 14. High-Definition Visualization & Publishing ───────────────────────────
Test-Master-API -Number 14 -Name "Visualization & Reports (SVG/PDF/Hindi)" -Url "$gatewayUrl/api/astro/visualize" -Method "POST" -Body $standardBirth

Write-Host "`n==========================================================================================" -ForegroundColor Cyan
Write-Host " 14-API MASTER SUITE SUMMARY                                                               " -ForegroundColor Cyan
Write-Host "==========================================================================================" -ForegroundColor Cyan

$passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
$total  = $results.Count

Write-Host "TOTAL APIS TESTED   : $total" -ForegroundColor White
Write-Host "SUCCESSFUL (PASS)   : $passed" -ForegroundColor Green
Write-Host "FAILED              : $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })

$results | Format-Table Number, Endpoint, Method, Status, HTTP, Duration, Detail -AutoSize

if ($failed -gt 0) {
    exit 1
} else {
    exit 0
}

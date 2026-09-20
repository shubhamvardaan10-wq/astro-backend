# deploy.ps1 - Automated Microservices Orchestrator for Windows PowerShell
param (
    [Parameter(Position=0)]
    [ValidateSet("up", "down", "restart", "status", "logs", "test")]
    [string]$Action = "up"
)

$ErrorActionPreference = "Stop"

function Check-Docker {
    try {
        docker info > $null 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Docker daemon is not running. Please start Docker Desktop."
        }
    } catch {
        Write-Error "Docker command not found in PATH."
    }
}

Check-Docker

switch ($Action) {
    "up" {
        Write-Host "=========================================================" -ForegroundColor Cyan
        Write-Host " Starting Astro-Backend 8-Microservice Mesh with Docker   " -ForegroundColor Cyan
        Write-Host "=========================================================" -ForegroundColor Cyan
        docker compose up -d --build
        Write-Host ""
        Write-Host "Microservices cluster launched in detached mode." -ForegroundColor Green
        Write-Host "Run '.\deploy\deploy.ps1 status' to check health status." -ForegroundColor Yellow
    }
    "down" {
        Write-Host "Stopping and removing all microservices containers..." -ForegroundColor Yellow
        docker compose down
        Write-Host "Mesh stopped successfully." -ForegroundColor Green
    }
    "restart" {
        Write-Host "Restarting microservices cluster..." -ForegroundColor Yellow
        docker compose down
        docker compose up -d
        Write-Host "Cluster restarted." -ForegroundColor Green
    }
    "status" {
        Write-Host "Microservices Topology & Health Status:" -ForegroundColor Cyan
        docker compose ps
    }
    "logs" {
        docker compose logs -f --tail=100
    }
    "test" {
        Write-Host "Probing microservice endpoints..." -ForegroundColor Cyan
        $endpoints = @(
            @{ Name = "API Gateway"; Url = "http://localhost:18080/actuator/health" },
            @{ Name = "Calc Engine"; Url = "http://localhost:8081/health" },
            @{ Name = "AI RAG Service"; Url = "http://localhost:8083/health" },
            @{ Name = "Media & Vision"; Url = "http://localhost:8084/health" },
            @{ Name = "Elasticsearch"; Url = "http://localhost:9200" }
        )
        foreach ($ep in $endpoints) {
            try {
                $resp = Invoke-RestMethod -Uri $ep.Url -TimeoutSec 5
                Write-Host "  [OK] $($ep.Name) ($($ep.Url)) is UP" -ForegroundColor Green
            } catch {
                Write-Host "  [WAIT/OFFLINE] $($ep.Name) ($($ep.Url)): $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    }
}

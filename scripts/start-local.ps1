param(
  [int]$TimeoutSeconds = 240
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $repoRoot

try {
  if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker CLI is not installed or not available in PATH."
  }

  docker info *> $null
  if ($LASTEXITCODE -ne 0) {
    throw "Docker Desktop/Engine is not running."
  }

  if (-not (Test-Path ".env")) {
    throw "Missing .env file. Create it and set CATFI_DB_PASSWORD before starting CatFi."
  }

  $dbPassword = Select-String -Path ".env" -Pattern '^\s*CATFI_DB_PASSWORD\s*=\s*.+$'
  if (-not $dbPassword) {
    throw ".env does not contain CATFI_DB_PASSWORD."
  }

  Write-Host "Starting CatFi local environment..."
  docker compose --profile enrichment --profile tools up -d --build
  if ($LASTEXITCODE -ne 0) {
    throw "docker compose up failed."
  }

  $services = @(
    "compensation-db", "mongo", "kafka", "redis", "kafka-ui",
    "user-service", "property-policy-service", "reporting-service",
    "disaster-simulator", "sensor-simulator", "damage-service",
    "compensation-service", "payment-service", "investment-service",
    "portfolio-service", "catfi-bff", "web-ui",
    "market-data-service", "market-shock-service", "weather-service"
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  do {
    $notReady = @()

    foreach ($service in $services) {
      $containerId = (docker compose --profile enrichment --profile tools ps -q $service).Trim()
      if (-not $containerId) {
        $notReady += "$service (not created)"
        continue
      }

      $status = (docker inspect --format '{{.State.Status}}' $containerId).Trim()
      if ($status -ne "running") {
        $notReady += "$service ($status)"
        continue
      }

      $health = (docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' $containerId).Trim()
      if ($health -and $health -ne "healthy") {
        $notReady += "$service ($health)"
      }
    }

    if ($notReady.Count -eq 0) { break }

    Write-Host ("Waiting for: " + ($notReady -join ", "))
    Start-Sleep -Seconds 5
  } while ((Get-Date) -lt $deadline)

  if ($notReady.Count -gt 0) {
    docker compose --profile enrichment --profile tools ps
    throw "CatFi did not become ready within $TimeoutSeconds seconds: $($notReady -join ', ')"
  }

  $checks = @(
    @{ Name = "BFF"; Url = "http://localhost:8087/api/ui/dashboard" },
    @{ Name = "Web UI"; Url = "http://localhost:5173" },
    @{ Name = "Kafka UI"; Url = "http://localhost:8088" }
  )

  foreach ($check in $checks) {
    try {
      $response = Invoke-WebRequest -Uri $check.Url -UseBasicParsing -TimeoutSec 10
      if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 400) {
        throw "HTTP $($response.StatusCode)"
      }
      Write-Host "[OK] $($check.Name): $($check.Url)"
    } catch {
      throw "$($check.Name) health check failed at $($check.Url): $($_.Exception.Message)"
    }
  }

  Write-Host ""
  Write-Host "CatFi local environment is ready."
  Write-Host "Web UI:   http://localhost:5173"
  Write-Host "Kafka UI: http://localhost:8088"
  Write-Host "BFF:      http://localhost:8087"
  Write-Host "Redis:    localhost:6379"
}
finally {
  Pop-Location
}

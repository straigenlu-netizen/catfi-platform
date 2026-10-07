$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $repoRoot

try {
  if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker CLI is not installed or not available in PATH."
  }

  Write-Host "Stopping CatFi local environment..."
  docker compose --profile enrichment --profile tools down
  if ($LASTEXITCODE -ne 0) {
    throw "docker compose down failed."
  }

  Write-Host "CatFi containers stopped. Persistent Docker volumes were kept."
}
finally {
  Pop-Location
}

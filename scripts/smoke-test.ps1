param(
    [string]$BaseUrl = 'http://localhost:8081',
    [string]$From = '2025-01-01',
    [string]$To = '2025-01-31',
    [string]$Quote = 'USD'
)

$ErrorActionPreference = 'Stop'
$base = $BaseUrl.TrimEnd('/')

function Assert-StatusUp {
    $health = Invoke-RestMethod -Uri "$base/actuator/health"

    if ($health.status -ne 'UP') {
        throw "Health was not UP: $($health.status)"
    }
}

Assert-StatusUp

$encodedQuote = [uri]::EscapeDataString($Quote)
$query = "from=$From&to=$To&quote=$encodedQuote"
$ingestUrl = "$base/api/v1/ingestion/backfill?$query"

Write-Host 'First ingestion...'

$first = Invoke-RestMethod -Method Post -Uri $ingestUrl
$first | ConvertTo-Json -Depth 10

Write-Host 'Second ingestion (must insert zero)...'

$second = Invoke-RestMethod -Method Post -Uri $ingestUrl
$second | ConvertTo-Json -Depth 10

$inserted = if ($null -ne $second.inserted) {
    $second.inserted
}
elseif ($null -ne $second.total) {
    $second.total.inserted
}
else {
    throw 'Unknown response shape: inspect second backfill response'
}

if ([int]$inserted -ne 0) {
    throw "Rerun inserted $inserted rows (expected 0)"
}

$rates = Invoke-RestMethod -Uri "$base/api/v1/rates?$query&page=0&size=20"
$stats = Invoke-RestMethod -Uri "$base/api/v1/stats/basic?$query"

if ([int]$rates.totalElements -le 0) {
    throw 'No rates returned'
}

if ([int]$stats.observations -le 0) {
    throw 'No statistics observations returned'
}

if ([int]$stats.observations -ne [int]$rates.totalElements) {
    throw 'Rates totalElements and stats observations differ'
}

$openapi = Invoke-RestMethod -Uri "$base/v3/api-docs"

if (-not $openapi.paths.'/api/v1/rates' -or
    -not $openapi.paths.'/api/v1/stats/basic') {
    throw 'OpenAPI does not include expected GET endpoints'
}

Write-Host "PASS: $($rates.totalElements) observations; rerun inserted 0; OpenAPI available"

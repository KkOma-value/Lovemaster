param(
    [string]$BaseUrl = "http://localhost:8088/api",
    [ValidateSet("sse", "sync")]
    [string]$Mode = "sse",
    [string]$Message = "他回我先忙一会儿晚点聊，这是什么意思？我该怎么回？",
    [string]$ChatId = ("love-test-" + (Get-Date -Format "yyyyMMdd-HHmmss")),
    [string]$Token = "",
    [switch]$SkipHealthCheck,
    [int]$TimeoutSeconds = 60
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function New-RequestUri {
    param(
        [string]$Endpoint,
        [hashtable]$Query
    )

    $builder = [System.UriBuilder]::new($BaseUrl.TrimEnd("/") + $Endpoint)
    $pairs = @()
    foreach ($key in $Query.Keys) {
        $value = [string]$Query[$key]
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            $pairs += ("{0}={1}" -f
                [System.Uri]::EscapeDataString($key),
                [System.Uri]::EscapeDataString($value))
        }
    }
    $builder.Query = [string]::Join("&", $pairs)
    return $builder.Uri.AbsoluteUri
}

function Invoke-HealthCheck {
    $healthUrl = New-RequestUri -Endpoint "/health" -Query @{}
    Write-Host "Checking health: $healthUrl" -ForegroundColor Cyan
    $response = Invoke-WebRequest -UseBasicParsing -Uri $healthUrl -TimeoutSec 10
    Write-Host "Health response: $($response.Content)" -ForegroundColor Green
}

function Invoke-LoveSync {
    $uri = New-RequestUri -Endpoint "/ai/love_app/chat/sync" -Query @{
        message = $Message
        chatId = $ChatId
    }

    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    Write-Host "Calling sync endpoint:" -ForegroundColor Cyan
    Write-Host $uri
    $response = Invoke-WebRequest -UseBasicParsing -Uri $uri -Headers $headers -TimeoutSec $TimeoutSeconds
    Write-Host "`n=== Sync Response ===" -ForegroundColor Yellow
    Write-Host $response.Content
}

function Invoke-LoveSse {
    $uri = New-RequestUri -Endpoint "/ai/love_app/chat/sse" -Query @{
        message = $Message
        chatId = $ChatId
        token = $Token
    }

    Write-Host "Calling SSE endpoint:" -ForegroundColor Cyan
    Write-Host $uri

    $handler = [System.Net.Http.HttpClientHandler]::new()
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds($TimeoutSeconds)
    if ($Token) {
        $client.DefaultRequestHeaders.Authorization =
            [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
    }

    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get, $uri)
    $response = $client.SendAsync(
        $request,
        [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead
    ).GetAwaiter().GetResult()

    if (-not $response.IsSuccessStatusCode) {
        throw "SSE request failed: $([int]$response.StatusCode) $($response.ReasonPhrase)"
    }

    $stream = $response.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
    $reader = [System.IO.StreamReader]::new($stream)
    $finalContent = ""

    Write-Host "`n=== SSE Events ===" -ForegroundColor Yellow
    while (-not $reader.EndOfStream) {
        $line = $reader.ReadLine()
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }
        if (-not $line.StartsWith("data:")) {
            continue
        }

        $payload = $line.Substring(5).Trim()
        if ($payload -eq "[DONE]") {
            Write-Host "[DONE]" -ForegroundColor Green
            break
        }

        try {
            $event = $payload | ConvertFrom-Json
            $type = [string]$event.type
            $content = [string]$event.content
            if ($type -eq "content" -and $content) {
                $finalContent += $content
            }
            "{0,-14} {1}" -f $type, $content
            if ($type -eq "done") {
                break
            }
        } catch {
            Write-Host $payload
        }
    }

    if ($finalContent) {
        Write-Host "`n=== Final Content ===" -ForegroundColor Green
        Write-Host $finalContent
    }

    $reader.Dispose()
    $stream.Dispose()
    $response.Dispose()
    $client.Dispose()
}

Write-Host "BaseUrl : $BaseUrl"
Write-Host "Mode    : $Mode"
Write-Host "ChatId  : $ChatId"

if (-not $SkipHealthCheck) {
    Invoke-HealthCheck
}

if ($Mode -eq "sync") {
    Invoke-LoveSync
} else {
    Invoke-LoveSse
}

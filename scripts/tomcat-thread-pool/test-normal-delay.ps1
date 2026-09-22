param(
    [string]$BaseUrl = "http://localhost:8080",
    [ValidateRange(1, 3600)]
    [int]$SlowSeconds = 10,
    [ValidateRange(1, 1000)]
    [int]$ConcurrentRequests = 10
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

$client = [System.Net.Http.HttpClient]::new()
$responses = [System.Collections.Generic.List[System.IDisposable]]::new()
$normalResponse = $null

try {
    Write-Host "Starting $ConcurrentRequests concurrent slow requests ($SlowSeconds seconds each)..."

    $slowUrl = "$BaseUrl/slow?seconds=$SlowSeconds"
    $tasks = @(
        1..$ConcurrentRequests | ForEach-Object {
            $client.GetAsync($slowUrl)
        }
    )

    Start-Sleep -Seconds 1

    Write-Host "Calling /normal while the Tomcat worker threads are occupied..."
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    $normalResponse = $client.GetAsync("$BaseUrl/normal").GetAwaiter().GetResult()
    $watch.Stop()

    Write-Host "`nResult:"
    Write-Host "  HTTP status : $([int]$normalResponse.StatusCode)"
    Write-Host "  Elapsed     : $([math]::Round($watch.Elapsed.TotalSeconds, 2)) seconds"

    foreach ($task in $tasks) {
        $response = $task.GetAwaiter().GetResult()
        $responses.Add($response)
    }

    Write-Host "`nAll slow requests completed."
}
finally {
    if ($null -ne $normalResponse) {
        $normalResponse.Dispose()
    }

    foreach ($response in $responses) {
        $response.Dispose()
    }

    $client.Dispose()
}

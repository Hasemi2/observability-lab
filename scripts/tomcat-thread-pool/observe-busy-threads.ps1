param(
    [string]$BaseUrl = "http://localhost:8080",
    [ValidateRange(1, 3600)]
    [int]$SlowSeconds = 10,
    [ValidateRange(1, 1000)]
    [int]$ConcurrentRequests = 9
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Net.Http

$client = [System.Net.Http.HttpClient]::new()
$responses = [System.Collections.Generic.List[System.IDisposable]]::new()

try {
    Write-Host "Starting $ConcurrentRequests concurrent slow requests ($SlowSeconds seconds each)..."

    $slowUrl = "$BaseUrl/slow?seconds=$SlowSeconds"
    $tasks = @(
        1..$ConcurrentRequests | ForEach-Object {
            $client.GetAsync($slowUrl)
        }
    )

    Start-Sleep -Seconds 1

    Write-Host "`nTomcat busy-thread metric while the requests are running:"
    $metricsUrl = "$BaseUrl/actuator/metrics/tomcat.threads.busy"
    $metric = $client.GetStringAsync($metricsUrl).GetAwaiter().GetResult()
    $metric | ConvertFrom-Json | ConvertTo-Json -Depth 10

    foreach ($task in $tasks) {
        $response = $task.GetAwaiter().GetResult()
        $responses.Add($response)
    }

    Write-Host "`nAll slow requests completed."
}
finally {
    foreach ($response in $responses) {
        $response.Dispose()
    }

    $client.Dispose()
}

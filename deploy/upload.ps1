# 上传安装包到服务器（Windows 上执行）
#
#   powershell -ExecutionPolicy Bypass -File deploy\upload.ps1
#   powershell -ExecutionPolicy Bypass -File deploy\upload.ps1 -OnlyVersion 0.0.7
#
# 做的事：按 deploy/site/versions.json 里列的版本，把 dist 里对应的 APK 传到
# /var/www/suggestion/apk/，再把下载页（index.html + versions.json）一起传上去，
# 最后 curl 自检一遍。

param(
    [string]$ServerHost = '39.96.113.178',
    [string]$User = 'root',
    [string]$Key = "$env:USERPROFILE\.ssh\suggestion_ecs",
    [int]$Port = 22,
    [string]$RemoteRoot = '/var/www/suggestion',
    [string]$OnlyVersion = ''
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$dist = Join-Path $repo 'dist'
$site = Join-Path $repo 'deploy\site'

$common = @('-i', $Key, '-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=15')
$sshBase = $common + @('-p', "$Port")
$scpBase = $common + @('-P', "$Port")
$target = "$User@$ServerHost"

function Invoke-Remote([string]$cmd) {
    & ssh @sshBase $target $cmd
    if ($LASTEXITCODE -ne 0) { throw "远端命令失败（exit $LASTEXITCODE）：$cmd" }
}

# 1. 以 versions.json 为准，挑出要传的包
$manifestPath = Join-Path $site 'versions.json'
$manifest = Get-Content $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
$items = @($manifest.items)
if ($OnlyVersion) {
    $items = @($items | Where-Object { $_.version -eq $OnlyVersion })
}
if ($items.Count -eq 0) { throw "versions.json 里没有要传的版本（-OnlyVersion=$OnlyVersion）" }

$files = @()
foreach ($it in $items) {
    $name = Split-Path $it.file -Leaf
    $local = Join-Path $dist $name
    if (-not (Test-Path $local)) { throw "dist 里缺少 $name（或者先把 APK 复制到 dist）" }
    $files += Get-Item $local
}

Write-Host "准备上传 $($files.Count) 个安装包到 $target : $RemoteRoot" -ForegroundColor Cyan
Invoke-Remote "mkdir -p $RemoteRoot/apk"

# 2. 传 APK
foreach ($apk in $files) {
    & scp @scpBase $apk.FullName "${target}:$RemoteRoot/apk/$($apk.Name)"
    if ($LASTEXITCODE -ne 0) { throw "上传失败：$($apk.Name)" }
    Write-Host "  ok  $($apk.Name)  $([math]::Round($apk.Length / 1MB, 1)) MB" -ForegroundColor Green
}

# 3. 传下载页
foreach ($name in @('index.html', 'versions.json')) {
    $local = Join-Path $site $name
    & scp @scpBase $local "${target}:$RemoteRoot/$name"
    if ($LASTEXITCODE -ne 0) { throw "上传失败：$name" }
    Write-Host "  ok  $name" -ForegroundColor Green
}

# 4. 权限收一下，nginx 只要读
Invoke-Remote "chmod -R a+r $RemoteRoot && chmod a+rx $RemoteRoot/apk"

# 5. 自检
Write-Host "自检：" -ForegroundColor Cyan
$base = "http://${ServerHost}:8080"
$page = & curl.exe -s -o NUL -w "%{http_code}" --max-time 15 "$base/"
$json = & curl.exe -s -o NUL -w "%{http_code}" --max-time 15 "$base/versions.json"
Write-Host "  下载页 $base/ -> HTTP $page"
Write-Host "  版本清单 -> HTTP $json"
foreach ($apk in $files) {
    $head = & curl.exe -s -I --max-time 15 "$base/apk/$($apk.Name)" 2>$null |
        Select-String -Pattern 'HTTP/|Content-Length' | ForEach-Object { $_.Line.Trim() }
    Write-Host "  $($apk.Name): $($head -join ' | ')"
}
Write-Host "完成。手机打开 $base/ 就能下载。" -ForegroundColor Green

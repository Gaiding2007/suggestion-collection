# 上传安装包到服务器（Windows 上执行）
#
#   powershell -ExecutionPolicy Bypass -File deploy\upload.ps1
#   powershell -ExecutionPolicy Bypass -File deploy\upload.ps1 -OnlyVersion 0.0.6
#
# 做的事：把 dist 里最新的 APK 传到 /var/www/suggestion/apk/，
# 顺手把下载页（index.html + versions.json）也传上去，最后 curl 自检一遍。

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

$sshBase = @('-i', $Key, '-p', "$Port", '-o', 'StrictHostKeyChecking=accept-new', '-o', 'ConnectTimeout=15')
$target = "$User@$ServerHost"

function Invoke-Remote([string]$cmd) {
    & ssh @sshBase $target $cmd
    if ($LASTEXITCODE -ne 0) { throw "远端命令失败（exit $LASTEXITCODE）：$cmd" }
}

# 1. 挑要传的安装包
if ($OnlyVersion) {
    $apks = Get-ChildItem (Join-Path $dist "*$OnlyVersion*.apk")
} else {
    $apks = Get-ChildItem (Join-Path $dist '*.apk')
}
if (-not $apks) { throw "dist 里没有找到安装包" }

Write-Host "准备上传 $($apks.Count) 个安装包到 $target : $RemoteRoot" -ForegroundColor Cyan
Invoke-Remote "mkdir -p $RemoteRoot/apk && ls -d $RemoteRoot"

# 2. 传 APK
foreach ($apk in $apks) {
    $remote = "${target}:$RemoteRoot/apk/$($apk.Name)"
    & scp @sshBase $apk.FullName $remote
    if ($LASTEXITCODE -ne 0) { throw "上传失败：$($apk.Name)" }
    Write-Host "  ok  $($apk.Name)  $([math]::Round($apk.Length / 1MB, 1)) MB" -ForegroundColor Green
}

# 3. 传下载页
foreach ($name in @('index.html', 'versions.json')) {
    $local = Join-Path $site $name
    if (-not (Test-Path $local)) { throw "缺少 $local" }
    & scp @sshBase $local "${target}:$RemoteRoot/$name"
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
foreach ($apk in $apks) {
    $head = & curl.exe -s -I --max-time 15 "$base/apk/$($apk.Name)" 2>$null | Select-String -Pattern 'HTTP/|Content-Length'
    Write-Host "  $($apk.Name): $($head -join ' | ')"
}
Write-Host "完成。手机打开 $base/ 就能下载。" -ForegroundColor Green

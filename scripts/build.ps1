$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
New-Item -ItemType Directory -Force target/classes | Out-Null
$sources = @(Get-ChildItem -LiteralPath src/main/java -Filter *.java -Recurse | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 --release 21 -d target/classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Java 编译失败' }
Write-Host '编译成功。'

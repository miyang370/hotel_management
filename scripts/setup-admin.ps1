$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
& "$PSScriptRoot/build.ps1"
& java -cp 'target/classes;lib/*' com.hotel.SetupAdmin
if ($LASTEXITCODE -ne 0) { throw '管理员初始化失败' }

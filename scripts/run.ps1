$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
& "$PSScriptRoot/build.ps1"
& java -cp 'target/classes;lib/*' com.hotel.Main
if ($LASTEXITCODE -ne 0) { throw '数据库验证未通过，请检查上方提示' }

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
& "$PSScriptRoot/build.ps1"
& javac -encoding UTF-8 -cp target/classes -d target/test-classes src/test/java/com/hotel/BookingIntegrationCheck.java
if ($LASTEXITCODE -ne 0) { throw '测试编译失败' }
$driverPath = Join-Path $env:USERPROFILE '.m2/repository/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar'
& java -cp "target/classes;target/test-classes;lib/*;$driverPath" com.hotel.BookingIntegrationCheck
if ($LASTEXITCODE -ne 0) { throw '预订集成检查未通过' }

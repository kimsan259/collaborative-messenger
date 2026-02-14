$ErrorActionPreference='Continue'
$base='http://localhost:8888'
$u1='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$u2='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$p='pass1234'
$cookie='cookie-test.txt'
if(Test-Path $cookie){Remove-Item $cookie -Force}

$reg1 = '{"username":"' + $u1 + '","password":"' + $p + '","displayName":"User One","email":"' + $u1 + '@test.com"}'
$reg2 = '{"username":"' + $u2 + '","password":"' + $p + '","displayName":"User Two","email":"' + $u2 + '@test.com"}'

$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" -d $reg1
$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" -d $reg2

$login = '{"username":"' + $u1 + '","password":"' + $p + '"}'
$loginResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/auth/login" -H "Content-Type: application/json" -d $login
Write-Output "LOGIN=$loginResp"

$roomJson = '{"name":"attach-test-room","roomType":"GROUP","memberUsernames":["' + $u2 + '"]}'
$roomResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/chat/rooms" -H "Content-Type: application/json" -d $roomJson
Write-Output "ROOM=$roomResp"
$roomObj = $roomResp | ConvertFrom-Json
$roomId = $roomObj.data.id
Write-Output "ROOM_ID=$roomId"

$testFile='tmp-attach-test.txt'
Set-Content -Path $testFile -Value 'hello attachment test' -Encoding UTF8

$sendResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/chat/rooms/$roomId/messages/file" -F "file=@$testFile" -F "content=file upload test"
Write-Output "SEND=$sendResp"
$histResp = curl.exe -s -c $cookie -b $cookie "$base/api/chat/rooms/$roomId/messages?page=0&size=20"
Write-Output "HISTORY=$histResp"

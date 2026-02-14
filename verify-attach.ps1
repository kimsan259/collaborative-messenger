$ErrorActionPreference='Stop'
$base='http://localhost:8888'
$u1='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$u2='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$p='pass1234'
$cookie='cookie-test.txt'
if(Test-Path $cookie){Remove-Item $cookie -Force}

$reg1 = '{"username":"' + $u1 + '","password":"' + $p + '","displayName":"User One","email":"' + $u1 + '@test.com"}'
$reg2 = '{"username":"' + $u2 + '","password":"' + $p + '","displayName":"User Two","email":"' + $u2 + '@test.com"}'

curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" -d $reg1 | Out-Null
curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" -d $reg2 | Out-Null

$login = '{"username":"' + $u1 + '","password":"' + $p + '"}'
curl.exe -s -c $cookie -b $cookie -X POST "$base/api/auth/login" -H "Content-Type: application/json" -d $login | Out-Null

$roomJson = '{"name":"attach-test-room","roomType":"GROUP","memberUsernames":["' + $u2 + '"]}'
$roomResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/chat/rooms" -H "Content-Type: application/json" -d $roomJson
$roomObj = $roomResp | ConvertFrom-Json
$roomId = $roomObj.data.id

$testFile='tmp-attach-test.txt'
Set-Content -Path $testFile -Value 'hello attachment test' -Encoding UTF8

$sendResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/chat/rooms/$roomId/messages/file" -F "file=@$testFile" -F "content=file upload test"
$histResp = curl.exe -s -c $cookie -b $cookie "$base/api/chat/rooms/$roomId/messages?page=0&size=20"
$histObj = $histResp | ConvertFrom-Json
$last = $histObj.data[-1]

Write-Output "USERS=$u1,$u2 ROOM=$roomId"
Write-Output "SEND_RESP=$sendResp"
Write-Output "LAST_TYPE=$($last.messageType) ATTACH_URL=$($last.attachmentUrl) ATTACH_NAME=$($last.attachmentName)"

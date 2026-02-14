$ErrorActionPreference='Stop'
$base='http://localhost:8888'
$u1='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$u2='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$p='pass1234'
$cookie='cookie-test.txt'
if(Test-Path $cookie){Remove-Item $cookie -Force}

$reg1Path='reg1.json'
$reg2Path='reg2.json'
$loginPath='login.json'
$roomPath='room.json'

@{username=$u1;password=$p;displayName='User One';email="$u1@test.com"} | ConvertTo-Json -Compress | Set-Content $reg1Path -Encoding UTF8
@{username=$u2;password=$p;displayName='User Two';email="$u2@test.com"} | ConvertTo-Json -Compress | Set-Content $reg2Path -Encoding UTF8
@{username=$u1;password=$p} | ConvertTo-Json -Compress | Set-Content $loginPath -Encoding UTF8
@{name='attach-test-room';roomType='GROUP';memberUsernames=@($u2)} | ConvertTo-Json -Compress | Set-Content $roomPath -Encoding UTF8

$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" --data-binary "@$reg1Path"
$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" --data-binary "@$reg2Path"

$loginResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/auth/login" -H "Content-Type: application/json" --data-binary "@$loginPath"
$loginObj = $loginResp | ConvertFrom-Json
if(-not $loginObj.success){ throw "login failed: $loginResp" }

$roomResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/chat/rooms" -H "Content-Type: application/json" --data-binary "@$roomPath"
$roomObj = $roomResp | ConvertFrom-Json
if(-not $roomObj.success){ throw "room create failed: $roomResp" }
$roomId = $roomObj.data.id

$testFile='tmp-attach-test.txt'
Set-Content -Path $testFile -Value 'hello attachment test' -Encoding UTF8

$sendResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/chat/rooms/$roomId/messages/file" -F "file=@$testFile" -F "content=file upload test"
$sendObj = $sendResp | ConvertFrom-Json
if(-not $sendObj.success){ throw "send file failed: $sendResp" }

Start-Sleep -Milliseconds 400
$histResp = curl.exe -s -c $cookie -b $cookie "$base/api/chat/rooms/$roomId/messages?page=0&size=20"
$histObj = $histResp | ConvertFrom-Json
if(-not $histObj.success){ throw "history failed: $histResp" }
$last = $histObj.data[-1]

Write-Output "USERS=$u1,$u2 ROOM=$roomId"
Write-Output "SEND_OK=$($sendObj.success) MSG=$($sendObj.message)"
Write-Output "LAST_TYPE=$($last.messageType) ATTACH_URL=$($last.attachmentUrl) ATTACH_NAME=$($last.attachmentName)"

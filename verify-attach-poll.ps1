$ErrorActionPreference='Stop'
$base='http://localhost:8888'
$u1='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$u2='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$p='pass1234'
$cookie='cookie-test.txt'
if(Test-Path $cookie){Remove-Item $cookie -Force}

@{username=$u1;password=$p;displayName='User One';email="$u1@test.com"} | ConvertTo-Json -Compress | Set-Content reg1.json -Encoding UTF8
@{username=$u2;password=$p;displayName='User Two';email="$u2@test.com"} | ConvertTo-Json -Compress | Set-Content reg2.json -Encoding UTF8
@{username=$u1;password=$p} | ConvertTo-Json -Compress | Set-Content login.json -Encoding UTF8
@{name='attach-test-room';roomType='GROUP';memberUsernames=@($u2)} | ConvertTo-Json -Compress | Set-Content room.json -Encoding UTF8

$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" --data-binary "@reg1.json"
$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" --data-binary "@reg2.json"
$loginResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/auth/login" -H "Content-Type: application/json" --data-binary "@login.json"
if(-not (($loginResp|ConvertFrom-Json).success)){ throw "login fail: $loginResp" }
$roomResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/chat/rooms" -H "Content-Type: application/json" --data-binary "@room.json"
$roomObj = $roomResp | ConvertFrom-Json
$roomId = $roomObj.data.id

Set-Content -Path tmp-attach-test.txt -Value 'hello attachment test' -Encoding UTF8
$sendResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/chat/rooms/$roomId/messages/file" -F "file=@tmp-attach-test.txt" -F "content=file upload test"
if(-not (($sendResp|ConvertFrom-Json).success)){ throw "send fail: $sendResp" }

$found = $null
for($i=0;$i -lt 20;$i++){
  Start-Sleep -Milliseconds 300
  $histResp = curl.exe -s -c $cookie -b $cookie "$base/api/chat/rooms/$roomId/messages?page=0&size=50"
  $histObj = $histResp | ConvertFrom-Json
  if($histObj.success -and $histObj.data){
    $match = $histObj.data | Where-Object { $_.attachmentUrl -and $_.attachmentName -eq 'tmp-attach-test.txt' } | Select-Object -Last 1
    if($match){ $found = $match; break }
  }
}

if(-not $found){ throw 'attachment message not found in history within timeout' }
Write-Output "USERS=$u1,$u2 ROOM=$roomId"
Write-Output "ATTACH_OK TYPE=$($found.messageType) URL=$($found.attachmentUrl) NAME=$($found.attachmentName)"

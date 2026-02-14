$ErrorActionPreference='Stop'
$base='http://localhost:8888'
$u1='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$u2='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$p='pass1234'
$cookie='cookie-img.txt'
if(Test-Path $cookie){Remove-Item $cookie -Force}

@{username=$u1;password=$p;displayName='Img One';email="$u1@test.com"} | ConvertTo-Json -Compress | Set-Content reg1.json -Encoding UTF8
@{username=$u2;password=$p;displayName='Img Two';email="$u2@test.com"} | ConvertTo-Json -Compress | Set-Content reg2.json -Encoding UTF8
@{username=$u1;password=$p} | ConvertTo-Json -Compress | Set-Content login.json -Encoding UTF8
@{name='image-test-room';roomType='GROUP';memberUsernames=@($u2)} | ConvertTo-Json -Compress | Set-Content room.json -Encoding UTF8

$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" --data-binary "@reg1.json"
$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" --data-binary "@reg2.json"
$null = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/auth/login" -H "Content-Type: application/json" --data-binary "@login.json"
$roomResp = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/chat/rooms" -H "Content-Type: application/json" --data-binary "@room.json"
$roomId = (($roomResp|ConvertFrom-Json).data.id)

$pngBase64='iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO7ZxgAAAABJRU5ErkJggg=='
[IO.File]::WriteAllBytes('tiny.png',[Convert]::FromBase64String($pngBase64))
$null = curl.exe -s -c $cookie -b $cookie -X POST "$base/api/chat/rooms/$roomId/messages/file" -F "file=@tiny.png;type=image/png" -F "content=tiny image"

$found=$null
for($i=0;$i -lt 20;$i++){
  Start-Sleep -Milliseconds 300
  $hist = curl.exe -s -c $cookie -b $cookie "$base/api/chat/rooms/$roomId/messages?page=0&size=50" | ConvertFrom-Json
  $m = $hist.data | Where-Object { $_.attachmentName -eq 'tiny.png' } | Select-Object -Last 1
  if($m){$found=$m; break}
}
if(-not $found){ throw 'image message not found' }
"ROOM=$roomId IMAGE_TYPE=$($found.messageType) URL=$($found.attachmentUrl)"

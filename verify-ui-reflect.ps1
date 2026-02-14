$ErrorActionPreference='Stop'
$base='http://localhost:8888'
$u1='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$u2='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$p='pass1234'
$c='check-ui-cookie.txt'
if(Test-Path $c){Remove-Item $c -Force}

@{username=$u1;password=$p;displayName='UI One';email="$u1@test.com"} | ConvertTo-Json -Compress | Set-Content reg1.json -Encoding UTF8
@{username=$u2;password=$p;displayName='UI Two';email="$u2@test.com"} | ConvertTo-Json -Compress | Set-Content reg2.json -Encoding UTF8
@{username=$u1;password=$p} | ConvertTo-Json -Compress | Set-Content login.json -Encoding UTF8
@{name='ui-check-room';roomType='GROUP';memberUsernames=@($u2)} | ConvertTo-Json -Compress | Set-Content room.json -Encoding UTF8

$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" --data-binary "@reg1.json"
$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" --data-binary "@reg2.json"
$null = curl.exe -s -c $c -b $c -X POST "$base/api/auth/login" -H "Content-Type: application/json" --data-binary "@login.json"
$roomResp = curl.exe -s -c $c -b $c -X POST "$base/api/chat/rooms" -H "Content-Type: application/json" --data-binary "@room.json"
$roomId = (($roomResp|ConvertFrom-Json).data.id)

$html = curl.exe -s -c $c -b $c "$base/chat/room/$roomId"
$js = curl.exe -s "$base/js/chat.js"

"ROOM_ID=$roomId"
"HTML_HAS_MODAL=" + ($html -match 'id="fileConfirmModal"')
"HTML_HAS_BOOTSTRAP_BUNDLE=" + ($html -match 'bootstrap.bundle.min.js')
"JS_HAS_OPEN_CONFIRM=" + ($js -match 'openFileConfirmModal')
"JS_HAS_CONFIRM_BTN=" + ($js -match 'confirmFileSendBtn')

$ErrorActionPreference='Stop'
$base='http://localhost:8888'
$u1='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$u2='u' + (Get-Random -Minimum 100000 -Maximum 999999)
$p='pass1234'
$c1='c1.txt'; $c2='c2.txt'
if(Test-Path $c1){Remove-Item $c1 -Force}; if(Test-Path $c2){Remove-Item $c2 -Force}

@{username=$u1;password=$p;displayName='F One';email="$u1@test.com"} | ConvertTo-Json -Compress | Set-Content reg1.json -Encoding UTF8
@{username=$u2;password=$p;displayName='F Two';email="$u2@test.com"} | ConvertTo-Json -Compress | Set-Content reg2.json -Encoding UTF8
@{username=$u1;password=$p} | ConvertTo-Json -Compress | Set-Content login1.json -Encoding UTF8
@{username=$u2;password=$p} | ConvertTo-Json -Compress | Set-Content login2.json -Encoding UTF8

$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" --data-binary "@reg1.json"
$null = curl.exe -s -X POST "$base/api/auth/register" -H "Content-Type: application/json" --data-binary "@reg2.json"

$null = curl.exe -s -c $c1 -b $c1 -X POST "$base/api/auth/login" -H "Content-Type: application/json" --data-binary "@login1.json"
$null = curl.exe -s -c $c2 -b $c2 -X POST "$base/api/auth/login" -H "Content-Type: application/json" --data-binary "@login2.json"

$user2 = (curl.exe -s -c $c1 -b $c1 "$base/api/friends/search?keyword=$u2" | ConvertFrom-Json).data[0]
$null = curl.exe -s -c $c1 -b $c1 -X POST "$base/api/friends/request/$($user2.friendId)"
$recv = curl.exe -s -c $c2 -b $c2 "$base/api/friends/requests/received" | ConvertFrom-Json
$fid = $recv.data[0].friendshipId
$null = curl.exe -s -c $c2 -b $c2 -X POST "$base/api/friends/accept/$fid"

$friendsOnline = curl.exe -s -c $c1 -b $c1 "$base/api/friends" | ConvertFrom-Json
$onlineValue = $friendsOnline.data[0].online

$null = curl.exe -s -c $c2 -b $c2 -X POST "$base/api/auth/logout"
Start-Sleep -Milliseconds 300
$friendsAfter = curl.exe -s -c $c1 -b $c1 "$base/api/friends" | ConvertFrom-Json
$offlineValue = $friendsAfter.data[0].online

"ONLINE_BEFORE_LOGOUT=$onlineValue OFFLINE_AFTER_LOGOUT=$offlineValue"

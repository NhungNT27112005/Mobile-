param([string]$BaseUrl = 'http://127.0.0.1:8000')
$ErrorActionPreference = 'Stop'
$newsHubBase = $BaseUrl.TrimEnd('/')
$newsHubName = Read-Host 'Display name'
$newsHubEmail = Read-Host 'Email'
$newsHubSecurePassword = Read-Host 'Password (at least 8 characters)' -AsSecureString
$newsHubPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($newsHubSecurePassword)
try {
    $newsHubPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($newsHubPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($newsHubPointer)
}
try {
    $newsHubRegistration = @{
        displayName = $newsHubName
        email = $newsHubEmail
        password = $newsHubPassword
    } | ConvertTo-Json -Compress
    $newsHubResult = Invoke-RestMethod -Method Post -Uri ($newsHubBase + '/auth/register') -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($newsHubRegistration))
    Write-Output 'Account created. Use this email/password when connecting Android to the real backend.'
} finally {
    $newsHubPassword = $null
    $newsHubRegistration = $null
}

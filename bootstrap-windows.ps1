# Download the main script
$scriptSrc = "http://corefx-ci.cloudapp.net/jenkins/userContent/jenkins-windows-startup.ps1"
$scriptDest = "$env:USERPROFILE\jenkins-windows-startup.ps1"
Write-Output "Downloading primary script from $scriptSrc to $scriptDest"
$wc.DownloadFile($scriptSrc, $scriptDest)
Write-Output "Executing script"
& $scriptDest
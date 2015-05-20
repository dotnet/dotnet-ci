# This script starts up a Windows Jenkins Slave

Set-ExecutionPolicy Unrestricted -force
# If your jenkins server is configured for security, make sure to edit command for how slave executes
# You may need to pass credentails or secret in the command , Refer to help by running "java -jar slave.jar --help"

$jenkinsserverurl = "http://dotnet-ci.cloudapp.net/"
$vmname = $env:COMPUTERNAME

$vmname = $vmname.ToLower();

# Download slave jar

Write-Output "Downloading jenkins slave jar "
$jarSource = $jenkinsserverurl + "jnlpJars/slave.jar"
$jarDest = "$env:USERPROFILE\slave.jar"
$wc = New-Object System.Net.WebClient
$wc.DownloadFile($jarSource, $jarDest)

# execute slave
$serverURL=$jenkinsserverurl + "computer/" + $vmname + "/slave-agent.jnlp"
# syntax for credentials username:apitoken or username:password
# you can get api token by clicking on your username --> configure --> show api token
$token="dotnet-bot:<insert token>"
$commandLine="java -jar $jarDest -jnlpUrl $serverURL -jnlpCredentials $token"
Write-Output "Executing slave process: $commandLine"
& java -jar $jarDest -jnlpUrl $serverURL -jnlpCredentials $token

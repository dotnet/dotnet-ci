# This script starts up a Windows Jenkins Slave

# If your jenkins server is configured for security, make sure to edit command for how slave executes
# You may need to pass credentails or secret in the command , Refer to help by running "java -jar slave.jar --help" 

$jenkinsserverurl = "http://corefx-ci.cloudapp.net/jenkins/"
$vmname = $env:COMPUTERNAME

# WORKAROUND - VM names are always 'Azure' and the computer name comes out as "AZURE".  Alter to avoid a 404 on the jnlp call.
$vmname = $vmname.Substring(0,1) + $vmName.Substring(1).ToLower();

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
$token="dotnet-bot:ca028e8f4a93ac3406bb47fd88ff80d3"
$commandLine="java -jar $jarDest -jnlpUrl $serverURL -jnlpCredentials $token"
Write-Output "Executing slave process: $commandLine"
& java -jar $jarDest -jnlpUrl $serverURL -jnlpCredentials $token
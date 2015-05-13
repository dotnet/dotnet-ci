# This script starts up a Windows Jenkins Slave

Set-ExecutionPolicy Unrestricted -force
# If your jenkins server is configured for security, make sure to edit command for how slave executes
# You may need to pass credentails or secret in the command , Refer to help by running "java -jar slave.jar --help"

$jenkinsserverurl = "http://dotnet-ci.cloudapp.net/"
$vmname = $env:COMPUTERNAME

# WORKAROUND - VM names are always 'Azure' and the computer name comes out as "AZURE".  Alter to avoid a 404 on the jnlp call.
$vmname = $vmname.ToLower();

# Loop this infinitely.  The reason we do this is that in jenkins, if the master breaks the connection,
# the slave process will wait to reconnect but will be unable to load winp.x64.dll because it will still be
# in use by another VM instance.  Thus, if jenkins disconnects, we won't be able to kill hung processes any longer.
# Exiting the java process (via noReconnect) should fix this, and we'll still attempt to reconnect again because
# of the loop.
while($true) {
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
    $token="dotnet-bot:<secret here>"
    $commandLine="java -jar $jarDest -jnlpUrl $serverURL -jnlpCredentials $token -noReconnect"
    Write-Output "Executing slave process: $commandLine"
    & java -jar $jarDest -jnlpUrl $serverURL -jnlpCredentials $token -noReconnect
}
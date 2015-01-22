# This script starts up a Windows Jenkins Slave

Set-ExecutionPolicy Unrestricted -force
# If your jenkins server is configured for security, make sure to edit command for how slave executes
# You may need to pass credentails or secret in the command , Refer to help by running "java -jar slave.jar --help" 

$jenkinsserverurl = "http://corefx-ci.cloudapp.net/jenkins"
$vmname = $env:COMPUTERNAME

# execute slave
Write-Output "Executing slave process "
$java="java.exe"
$jar="-jar"
$jnlpUrl="-jnlpUrl" 
$serverURL=$jenkinsserverurl+"computer/" + $vmname + "/slave-agent.jnlp"
$secret="-jnlpCredentials"
# syntax for credentials username:apitoken or username:password
# you can get api token by clicking on your username --> configure --> show api token
$token="dotnet-bot:ca028e8f4a93ac3406bb47fd88ff80d3"
& $java $jar $destSource $jnlpUrl $serverURL $secret $token
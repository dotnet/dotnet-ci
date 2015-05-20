$JNLPPort=57816
$ServerName="dotnet-ci"

echo "Adding jenkins Master $ServerName port Jenkins JNLP port ($JNLPPort) to have a 30 minute timeout."

Get-AzureVM -ServiceName "dotnet-ci" -Name $ServerName | Add-AzureEndpoint -Name "Jenkins" -Protocol "tcp" -PublicPort $JNLPPort -LocalPort $JNLPPort -IdleTimeoutInMinutes 30 | Update-AzureVM
<#
.SYNOPSIS
    Clean-Deployments
.DESCRIPTION
    Cleans deployments from resource groups so we don't go over azure limits
.PARAMETER ImageBaseName
    Base name for the final image name.
.PARAMETER Run forever
.PARAMETER ResourceGroupName
    Resource group to clean
#>

param (
    [Parameter(Mandatory=$true)]
    [string]$ResourceGroupName,
    [switch]$RunForever = $false,
    [switch]$DryRun = $false
)
   
do {
    $deployments = Get-AzureRmResourceGroupDeployment -ResourceGroupName $ResourceGroupName
    # Delete from oldest first
    $deployments = $deployments | Sort-Object Timestamp
    foreach ($deployment in $deployments) {
        if ($DryRun) {
            Write-Output "Would delete $($deployment.DeploymentName) from $ResourceGroupName"
        } else {
            Write-Output "Deleting $($deployment.DeploymentName) from $ResourceGroupName"
            Remove-AzureRmResourceGroupDeployment -Name $deployment.DeploymentName -ResourceGroupName $ResourceGroupName
        }
    }
}
while($RunForever)
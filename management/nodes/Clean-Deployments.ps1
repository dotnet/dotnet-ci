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
    [string]$ResourceGroupName = $null,
    [switch]$RunForever = $false
)

workflow CleanDeployments {
    param(
        $ResourceGroups,
        $RunForever
    )
   
    foreach -parallel ($resourceGroup in $ResourceGroups) {
        # Need to login to each process.
        Login-AzureRmAccount
        do {
            $deployments = Get-AzureRmResourceGroupDeployment -ResourceGroupName $resourceGroup
            # Delete from oldest first
            $deployments = $deployments | Sort-Object Timestamp
            foreach ($deployment in $deployments) {
                Write-Output "Deleting $($deployment.DeploymentName) from $resourceGroup"
                Remove-AzureRmResourceGroupDeployment -Name $deployment.DeploymentName -ResourceGroupName $resourceGroup
            }
        }
        while($RunForever)
    }
}

# If the RG was not specified, read from the list
$resourceGroups = @($ResourceGroupName)
if (!$ResourceGroupName) {
    $resourceGroups = .\Get-Available-Resource-Groups.ps1
}

CleanDeployments -ResourceGroups $ResourceGroups -RunForever $RunForever
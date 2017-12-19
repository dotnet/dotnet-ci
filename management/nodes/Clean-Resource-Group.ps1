<#
.SYNOPSIS
    Remove-Unused-Nodes
.DESCRIPTION
    Removes VMs from Azure that aren't used by Jenkins (but were provisioned by Jenkins)
.PARAMETER ResourceGroupName
    Name of resource group containings VMs
#>

param (
    [Parameter(Mandatory=$true)]
    [string]$ResourceGroupName,
    [switch]$RunForever = $false,
    $Servers = @("https://ci.dot.net","https://ci2.dot.net","https://ci3.dot.net"),
    [string]$Regex = '.*[a-f0-9]{6}$',
    [switch]$DryRun = $false
)

do {
    $removedList = @()
    Write-Host "Looking for VMs in $ResourceGroupName with names ending matching $Regex"

    $provisionedVMs = Get-AzureRmVM $ResourceGroupName
    $provisionedVMs = $provisionedVMs | Get-Random -Count $provisionedVMs.Count
    foreach ($vm in $provisionedVMs) {
        $vmState = $vm.ProvisioningState
        Write-Verbose "Checking $vmName, provisioning state $vmState"
        if ($vmState -ne "Succeeded" -and $vmState -ne "Failed") {
            continue
        }
        $vmName = $vm.Name
        # A bit of a hack, but determine whether this was a machine
        # allocated by Jenkins by looking for a date suffix
        
        if (-not ($vmName -match $Regex)) {
            Write-Output "Skipping $vmName, doesn't match"
            continue
        }
        $delete = $true
        Write-Verbose "  Looking for $vmName in $server"
        # Look up in each Jenkins instance
        foreach ($server in $Servers) {
            try {
                Write-Verbose "    Looking for $vmName in $server"
                Invoke-WebRequest "$server/computer/$vmName" | Out-NUll
                Write-Verbose "      Found $vmName in $server, not removing"
                # Worked, don't delete
                $delete = $false
                break
            }
            catch {
                $responseCode = $_.Exception.Response.StatusCode
                if ($responseCode -ne "NotFound") {
                    Write-Verbose "      Got unexpected exception code ($responseCode) from $server for $vmName not removing"
                    $delete = $false
                    break
                }
                else {
                    Write-Verbose "      Got 400 exception code from $server for $vmName"
                }
            }
        }

        if ($delete) {
            if ($DryRun) {
                Write-Host "  Would delete $vmName from $ResourceGroupName (provisioning state $vmState)"
                $removedList += $vmName
            }
            else {
                Write-Host "  Deleting $vmName from $ResourceGroupName (provisioning state $vmState)"
                try {
                    .\Delete-VM.ps1 -VMName $vmName -ResourceGroupName $ResourceGroupName -ErrorAction Continue
                }
                catch {
                    Write-Host "  Failed to delete $vmName from $ResourceGroupName"
                }
            }
        }
    }

    Write-Host "Removed: $removedList"
    
    # Remove public IPs and NICs
    
    if ($DryRun) {
        Write-Host "Would delete the following NICs"
        Get-AzureRmNetworkInterface -ResourceGroupName $ResourceGroupName | Where-Object { $_.VirtualMachine -eq $null } | Select-Object Name
        Write-Host "Would delete the following PIPs"
        Get-AzureRmPublicIpAddress -ResourceGroupName $ResourceGroupName | Where-Object { $_.IpConfiguration -eq $null } | Select-Object Name
    } else {
        Get-AzureRmNetworkInterface -ResourceGroupName $ResourceGroupName | Where-Object { $_.VirtualMachine -eq $null } | Remove-AzureRmNetworkInterface -Force
        Get-AzureRmPublicIpAddress -ResourceGroupName $ResourceGroupName | Where-Object { $_.IpConfiguration -eq $null } | Remove-AzureRmPublicIpAddress -Force
    }
}
while ($RunForever)
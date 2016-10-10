<#
.SYNOPSIS
    Get-ConnectionInfo.ps1
.DESCRIPTION
    Retrieves connection info for a VM
.PARAMETER VMName
    VM to get connection info for
.PARAMETER ResourceGroupName
    Resource group containing the VM
#>

param (
    [string]$VMName = $null,
    [string]$ResourceGroupName = $null
)

# Ensure logged in
$ctx = Get-AzureRmContext
if (!$ctx) {
    Exit
}

# If the RG was not specified, read from the list
$resourceGroups = @($ResourceGroupName)
if (!$ResourceGroupName) {
    $resourceGroups = Get-Content 'ResourceGroups.txt' | Select-String '^[^#]'
}

# Walk each RG and look up the VM

foreach ($resourceGroup in $resourceGroups) {
    $existingVM = Get-AzureRmVM -ResourceGroupName $resourceGroup -Name $VMName -ErrorAction SilentlyContinue
    
    if ($existingVM) {
        Write-Verbose "Found $VMName in $resourceGroup"
        # Grab the NIC info
        $nicId = $existingVM.NetworkProfile.NetworkInterfaces[0].Id
        # Parse out the name of the NIC
        $nicName = $nicId.Substring($nicId.LastIndexOf('/') + 1)
        # Grab the NIC info
        Write-Verbose "Looking up NIC $nicName"
        $nicInfo = Get-AzureRmNetworkInterface -Name $nicName -ResourceGroupName $resourceGroup -ErrorAction SilentlyContinue
        
        if (!$nicInfo) {
            Write-Error "Could not find information on NIC $nicName in $resourceGroup"
        }
     
        # Get the IP configurations
        $pipId = $nicInfo.IpConfigurations[0].PublicIpAddress.Id
        
        if (!$pipId) {
            Write-Error "NIC $nicName doesn't have a public IP address"
        }
        
        # Parse out the name of the PIP
        $pipName = $pipId.Substring($pipId.LastIndexOf('/') + 1)
        
        # Look up the PIP
        $pipInfo = Get-AzureRmPublicIpAddress -Name $pipName -ResourceGroupName $resourceGroup -ErrorAction SilentlyContinue
        
        if (!$pipInfo) {
            Write-Error "Could not find information on PIP $pipName in $resourceGroup"
        }
        
        # Check and see whether there are DNS settings. Use those if possible
        $fqdn = $pipInfo.DnsSettings.Fqdn
        $targetAddressOrIP = $null
        if ($fqdn) {
            $targetAddressOrIP = $fqdn
        }
        else {
            # Grab the raw public IP address
            $targetAddressOrIP = $pipInfo.IpAddress
        }
        
        Write-Output "Connection Strings"
        Write-Output "------------------"
        if ($existingVM.OSProfile.linuxConfiguration) {
            # Write out various strings
            Write-Output "PuTTY SSH: putty $targetAddressOrIP"
            Write-Output "SSH: ssh $targetAddressOrIP"
        }
        else {
            Write-Output "RDP: mstsc /v:$targetAddressOrIP"
        }
        Exit
    }
}

Write-Error "Could not find any VM named $VMName"
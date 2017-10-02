<#
.SYNOPSIS
    Delete-VM.ps1
.DESCRIPTION
    Deletes a VM and associated resources
.PARAMETER VMName
    VM to get connection info for
.PARAMETER ResourceGroupName
    Resource group containing the VM
#>

param (
    [string]$VMName,
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

Write-Host "Walking resource groups to find $VMName"
# Find the VM
# Walk each RG and look up the VM

foreach ($resourceGroup in $resourceGroups) {
    Write-Host "Looking in $resourceGroup"
    
    $existingVM = Get-AzureRmVM -ResourceGroupName $resourceGroup -Name $VMName -ErrorAction SilentlyContinue
    
    if ($existingVM) {
        Write-Host "Found $VMName in $resourceGroup"
        # Note the NIC name
        $nicId = $existingVM.NetworkProfile.NetworkInterfaces[0].Id
        # Parse out the name of the NIC
        $nicName = $nicId.Substring($nicId.LastIndexOf('/') + 1)
        
        Write-Host "Removing VM"
        
        # Remove the VM.  We continue running on error so that we ensure we clean up the rest
        Remove-AzureRmVM -Name $VMName -ResourceGroupName $resourceGroup -ErrorAction Continue -Force

        # Grab the NIC info
        Write-Host "Looking up associated NIC $nicName"
        $nicInfo = Get-AzureRmNetworkInterface -Name $nicName -ResourceGroupName $resourceGroup -ErrorAction SilentlyContinue
        
        if (!$nicInfo) {
            Write-Error "Could not find information on NIC $nicName in $resourceGroup"
        }
        else {
            Write-Host "Removing NIC $nicName"
            
            # Remove the NIC
            Remove-AzureRmNetworkInterface -Name $nicName -ResourceGroupName $resourceGroup -Force
            
            # Get the IP configurations
            $pipId = $nicInfo.IpConfigurations[0].PublicIpAddress.Id
        
            if (!$pipId) {
                Write-Error "NIC $nicName doesn't have a public IP address"
            }
            else {
                # Parse out the name of the PIP
                $pipName = $pipId.Substring($pipId.LastIndexOf('/') + 1)

                Write-Host "Removing PIP $pipName"
            
                # Remove the PIP
                Remove-AzureRmPublicIpAddress -Name $pipName -ResourceGroupName $resourceGroup -Force
            }
        }
        
        Write-Host "Locating OS disk URI"
        
        # Remove the OS disk VHD
        $osDiskUri = $existingVM.StorageProfile.osDisk.vhd.uri
        
        Write-Host "Got disk URI $osDiskUri"
       
        if ($osDiskUri) {
            $matches = [regex]::Match($osDiskUri, "^https?://(?<storageaccountname>.*).blob.core.windows.net/(?<containername>.*?)/(.*/)?(?<blobname>.*\.vhd)$")
            
            if (!$matches.Success) {
                Write-Error "Could not parse blob URI $osDiskURI"
            }
            else {
                $storageAccountName = $matches.Groups["storageaccountname"].Value
                $storageContainer = $matches.Groups["containername"].Value
                $storageBlob = $matches.Groups["blobname"].Value
                Write-Host "Attempting to delete $storageBlob in container $storageContainer from account $storageAccountName"
                
                $context = .\Get-Storage-Context.ps1 $storageAccountName
                $blobs = Get-AzureStorageBlob -Container $storageContainer -Context $context -Blob $storageBlob
                if ($blobs.Count -gt 1) {
                    Write-Error "Unexpected number of blobs found"
                    $blobs
                }
                elseif ($blobs.Count -eq 0) {
                    Write-Error "No blobs matching $storageBlob found in container $storageContainer"
                }
                else {
                    $blobs | Remove-AzureStorageBlob
                }
            }
        }
        else {
            Write-Error "Could not locate blob URI for OS disk"
        }
        
        Write-Host "Succesfully Deleted $VMName and associated resources"
        break
    }
}


<#
.SYNOPSIS
    Capture-VM
.DESCRIPTION
    Captures a VM image for use
.PARAMETER ImageBaseName
    Base name for the final image name.
.PARAMETER VMName
    VM Name to capture
.PARAMETER ResourceGroupName
    Resource group name containing the VM (if not specified, VM is located in the standard RGs)
#>

param (
    [ValidateSet('ubuntu1404','ubuntu1504', 'ubuntu1604', 'ubuntu1610', 'win2008', 'win2012', 'win2016', 'centos71', 'rhel72', 'freebsd', 'suse132', 'suse421', 'deb82', 'fedora23', 'fedora24', 'deb84')]
    [string]$ImageBaseName = $(Read-Host -prompt "VM image base name of image to capture."),
    [string]$ResourceGroupName = $null,
    [string]$VMName = $(Read-Host -prompt "VM name to capture"),
    [switch]$ForceShutdown = $false,
    [string]$TargetContainer = 'dotnetci',
    [string]$Suffix = $null
)

# Ensure logged in
$ctx = Get-AzureRmContext
if (!$ctx) {
    Exit
}

Write-Host "Looking up VM $VMName"

# If the RG was not specified, read from the list
$resourceGroups = @($ResourceGroupName)
if (!$ResourceGroupName) {
    $resourceGroups = .\Get-Available-Resource-Groups.ps1
}

foreach ($resourceGroup in $resourceGroups) {
    $existingVM = Get-AzureRmVM -ResourceGroupName $resourceGroup -Name $VMName -Status -ErrorAction SilentlyContinue
    
    if (!$existingVM) {
        continue
    }
    
    Write-Host "Found $VMName in $resourceGroup"
    
    $gotPowerState = $false
    # Ensure that the VM has been shutdown
    foreach ($status in $existingVm.Statuses) {
        if ($status.Code.StartsWith('PowerState')) {
            if ($status.Code -eq "PowerState/stopped") {
                # was shut down, can be stopped
                Write-Host "VM is stopped, shutting down"
                Stop-AzureRmVM -ResourceGroupName $resourceGroup -Name $VMName -Force
            }
            elseif ($status.Code -eq "PowerState/deallocated") {
                # Nothing to do
                Write-Host "VM is already deallocated, skipping shutdown"
            }
            else {
                if ($ForceShutdown) {
                    Write-Warning "$VMName not currently shut down $($status.Code), forcing."
                    Stop-AzureRmVM -ResourceGroupName $resourceGroup -Name $VMName -Force
                }
                else {
                    Write-Error "$VMName not currently shut down, please ensure it is generalized and stopped before capturing."
                    Exit 1
                }
            }
            $gotPowerState = $true
            break
        }
    }
    
    if (!$gotPowerState) {
        Write-Error "Unable to determine power state, exiting"
        Exit 1
    }
    
    Write-Host "Marking VM as generalized"
    Set-AzureRmVM -ResourceGroupName $resourceGroup -Name $VMName -Generalized 
    
    # Create a temporary json file
    $tempFileName = [System.IO.Path]::GetTempFileName()
    
    # Construct the image name prefix
    $curDate = Get-Date
    $formattedMonth = "{0:D2}" -f $($curDate.Month)
    $formattedDay = "{0:D2}" -f $($curDate.Day)
    $imageName = "$ImageBaseName-$($curDate.Year)$formattedMonth$formattedDay"
    
    if ($Suffix) {
        $imageName += "-$Suffix"
    }
    
    Write-Host "Saving image with prefix $imageName"
    
    # Save the image.  Note that the parameters to this aren't the most obvious.
    # It gets saved automatically to "system/Microsoft.Compute/Images/<container name>"
    Save-AzureRmVMImage -ResourceGroupName $resourceGroup -Name $VMName -DestinationContainerName $TargetContainer -VHDNamePrefix $imageName -Path $tempFileName
    
    # Read in the data from the temp file 
    $imageData = Get-Content $tempFileName | ConvertFrom-Json
    
    $imageLocation = $imageData.resources.properties.storageProfile.osDisk.image.uri
    Write-Host "New image is located at $imageLocation"
 
    $imageProperties = @{'Image'=$imageLocation;
                         'Name'=$imageName;}
                
    Write-Output $imageProperties
    
    # Delete the VM (now not useful)
    .\Delete-VM $VMName
    Exit 0
}

Write-Error "Could not find VM $VMName in any resource group"

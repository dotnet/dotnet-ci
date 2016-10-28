<#
.SYNOPSIS
    Capture-And-Deploy-VM
.DESCRIPTION
    Captures a VM image for use, deploys it to the necessary stoirage accounts, and pends a change to the image list
.PARAMETER ImageBaseName
    Base name for the final image name.
.PARAMETER VMName
    VM Name to capture
.PARAMETER ResourceGroupName
    Resource group name containing the VM (if not specified, VM is located in the standard RGs)
#>

param (
    [ValidateSet('ubuntu1404','ubuntu1504', 'ubuntu1604', 'ubuntu1610', 'win2012', 'win2016', 'centos71', 'rhel72', 'freebsd', 'suse132', 'suse421', 'deb82', 'fedora23', 'deb84')]
    [string]$ImageBaseName = $(Read-Host -prompt "VM image base name of image to capture."),
    [string]$VMName = $(Read-Host -prompt "VM name to capture"),
    [switch]$ForceShutdown = $false,
    [string]$TargetContainer = 'dotnetci',
    [string]$Suffix = $null
)

# Capture the VM

$imageProperties = .\Capture-VM -ImageBaseName $ImageBaseName -VMName $VMName -ForceShutdown $ForceShutdown -TargetContainer $TargetContainer -Suffix $Suffix

if (!$imageProperties) {
    Write-Error "Failed to capture $VMName, exiting"
    Exit 1
}

# Deploy the VHD
.\Deploy-VHD.ps1 -VHDUri $imageProperties.Image -TargetStorageAccounts 'All'

# Pend the change to the images file
$images = .\Get-Images.ps1 
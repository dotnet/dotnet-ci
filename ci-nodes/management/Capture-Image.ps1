<#
.SYNOPSIS
    Capture-Image
.DESCRIPTION
    Captures an image of a machine in the dotnet-ci pool.  This image can be used with Cycle-Nodes.
.PARAMETER Machine
    Name of the machine to capture.
.PARAMETER VersionNumber
    Version number of the image being saved.	
.PARAMETER Description
	Additional descriptive info about the image being captured.
.COMMENTS
	This script captures an image for use in the dotnet-ci pool.
	This is largely straightforward wrapper around Save-AzureVMImage, with special predetermined
	formatting for the image names and part of the image description.
	
	Machine MUST be generalized before calling this script. 
#>

param (
    [string]$Machine = $(Read-Host -prompt "Machine to capture."),
    [int]$VersionNumber = $(Read-Host -prompt "Version number of the image."),
    [string]$Description = $null
)

$ServiceName="dotnet-ci-nodes"

# Do some housekeeping.  Look up the VM.  Check the OS and current state, and construct the potential image name to check whether it exists.

Write-Host "Looking up $Machine in $ServiceName Azure cloud service"

$existingVM = Get-AzureVM -ServiceName $ServiceName -Name $Machine

if (!$existingVM)
{
	throw "Could not find $Machine, did you get the VM name correct?"
}

Write-Host "Checking machine state"

# Check the state

if ($existingVM.PowerState -ne "Stopped")
{
	throw "VM must be generalized and stopped or shut down before capture!"
}

# Construct a name for the VM
# There's no way to know what the VM OS is, but we do know the previous machine name,
# which has a standard name.  So attempt to find the OS names in the vm name, and use that

if ($Machine -match "win")
{
	# Windows
	$osType = "win"
	$osDescription = "Windows"
}
elseif ($Machine -match "ub")
{
	# Ubuntu
	$osType = "ub"
	$osDescription = "Ubuntu 14.04"
}
elseif ($Machine -match "fbsd")
{
	# FreeBSD
	$osType = "fbsd"
	$osDescription = "FreeBSD 10.1"
}
else
{
	throw "Unknown OS type.  Expected $Machine to contain 'win, ub, or fbsd'"
}

$imageNameBase = "dci-$osType-bld"
$imageName = "$imageNameBase-$VersionNumber"

Write-Host "New image will have name $imageName, checking for availability"

# Check for the existence of this image, and then also check for $VersionNumber-1 if $$VersionNumber is > 1

if (Get-AzureVMImage -ImageName $imageName -ErrorAction SilentlyContinue)
{
	Write-Host "Existing images with the same base name:"
	& Get-AzureVMImage | findstr "$imageNameBase"
	throw "Version $VersionNumber (image name $imageName) already used, please choose another"
}

if ($VersionNumber -gt 1)
{
	$previousVersion = $VersionNumber-1
	$previousImageName = "$imageNameBase-$previousVersion"
	if (-not (Get-AzureVMImage -ImageName $previousImageName -ErrorAction SilentlyContinue))
	{
		Write-Host "Existing images with the same base name:"
		& Get-AzureVMImage | findstr "$imageNameBase"
		throw "Please choose the next version number in the series."
	}
}

# Construct the description

$finalDescription = ".NET CI $osDescription Build/Test Image, Version $VersionNumber"

if ($Description)
{
	$finalDescription += ". $Description"
}

Write-Host "Image will have description: $finalDescription"
Write-Host "Attempting to Capture"

Save-AzureVMImage -ServiceName $ServiceName -Name $Machine -ImageName $imageName -OSState "Generalized"  -ImageLabel $finalDescription

Write-Host "Success"
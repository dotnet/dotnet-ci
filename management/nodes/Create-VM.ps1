<#
.SYNOPSIS
    Create-VM
.DESCRIPTION
    Create a new VM for the purposes of onboarding/updating an image for use in CI
.PARAMETER ImageName
    Image name to create a VM with
.PARAMETER ImageVHD
    Image name to create a VM with
.PARAMETER VMName
    Name of the VM to create.
.PARAMETER VNetName
    Name of the virtual network to put the VM in.
.PARAMETER VNetSubnetName
    Name of the virtual network subnet to put the VM in.
.PARAMETER ResourceGroupName
    Name of the resource group to put the VM under.  Defaults to the user storage VM group
.PARAMETER StorageAccountName
    Name of the storage account containing the VM image (also gets the target VM)
.PARAMETER User
	User name to create the VM with
.PARAMETER Password
	Password for the user
.PARAMETER VMSize
	Size of the VM to create. 
#>

param (
    [string]$ImageName = $null,
    [string]$ImageVHD = $null,
    [ValidateSet('Windows','Linux')]
    [string]$OperatingSystem = $null,
    [string]$VMName = $(Read-Host -prompt "VM name to deploy"),
    [string]$ResourceGroupName = 'dotnet-ci-user-vms',
    [string]$Location = 'westus2',
    [string]$StorageAccountName = 'dotnetciuservmstorage2',
    [string]$VNetName = 'dotnet-ci-user-vms-vnet',
    [string]$VNetSubnetName = 'dotnet-ci-user-vms-vnet-subnet',
    [string]$VMSize = 'Standard_D3_v2'
)

# Parameter checks
if (-not ($ImageName -or $ImageVHD)) {
    Write-Error "Must supply either image name or image VHD location"
    Exit 1
}
if ($ImageVHD -and -not $OperatingSystem) {
    Write-Error "When specifying a VHD, must specify image OS type (Windows Or Linux)"
    Exit 1
}

# Ensure logged in
$ctx = Get-AzureRmContext
if (!$ctx) {
    Exit
}

# Check to see whether the RG exists

$existingRg = Get-AzureRmResourceGroup -Name $ResourceGroupName -ErrorAction Stop
if (!$existingRg) {
    Write-Error "Resource group $ResourceGroupName doesn't exist"
    Exit 1
}

$existingVM = Get-AzureRmVM -ResourceGroupName $ResourceGroupName -Name $VMName -ErrorAction SilentlyContinue
if ($existingVM) {
    Write-Error "VM name $VMName already exists, please choose another (or delete this VM"
    Exit 1
}

# Grab the storage account info
$storageAccountInfo = Get-AzureRmStorageAccount $StorageAccountName -ResourceGroupName $ResourceGroupName -ErrorAction Stop

if (!$storageAccountInfo) {
    Write-Error "Storage account $StorageAccountName not found in Resource Group $ResourceGroupName"
}

$sourceVHDUri = $imageVHD
$osType = $OperatingSystem
# Grab the image VHD location
if (!$imageVHD) {
    $sourceInfo = .\Get-Images.ps1 $ImageName
    
    if ($sourceInfo) {
        $osType = $sourceInfo.OSType
        $sourceVHDUri = .\Get-VHD-Location.ps1 $ImageName -StorageAccountName $StorageAccountName
    }
    else {
        Write-Error "Could not locate $ImageName"
        Exit 1
    }
}

if ($sourceVHDUri) {
    Write-Output "Deploying ${sourceVHDUri}:"
    Write-Output "  OS: $osType"
}

# Check to see whether the network exists
$vnet = Get-AzureRmVirtualNetwork -Name $VNetName -ResourceGroupName $ResourceGroupName -ErrorAction SilentlyContinue

if (!$vnet) {
    Write-Output "Could not locate vnet $VNetName, creating"
    
    $subnetConfig = New-AzureRmVirtualNetworkSubnetConfig -Name $VNetSubnetName -AddressPrefix 10.0.0.0/24
    $vnet = New-AzureRmVirtualNetwork -Name $VNetName -ResourceGroupName $ResourceGroupName -Location $Location -AddressPrefix 10.0.0.0/16 -Subnet $subnetConfig
}

$PIPName = $VMName + "PIP"
$NICName = $VMName + "NIC"

# Create a public IP for the VM
$pip = New-AzureRmPublicIpAddress -Name $PIPName -ResourceGroupName $ResourceGroupName -Location $Location -AllocationMethod Dynamic

# Create a NIC
$nic = New-AzureRmNetworkInterface -Name $NICName -ResourceGroupName $ResourceGroupName -Location $Location -SubnetId $vnet.Subnets[0].Id -PublicIpAddressId $pip.Id

Write-Output "  NIC: $NICName"
Write-Output "  PIP: $PIPName"

# Grab credentials
$cred = Get-Credential -Message "Type the user name and password of the administrator account"

# Create the VM configuration
$vm = New-AzureRmVMConfig -VMName $VMName -VMSize $VMSize
if ($osType -eq "Windows") {
    $vm = Set-AzureRmVMOperatingSystem -VM $vm -Windows -ComputerName $VMName -Credential $cred -ProvisionVMAgent
}
else {
    $vm = Set-AzureRmVMOperatingSystem -VM $vm -Linux -ComputerName $VMName -Credential $cred
}

# Attach the network interface
$vm = Add-AzureRmVMNetworkInterface -VM $vm -Id $nic.Id

# Set the VHD uri for the OS disk
$blobEndpoint = $storageAccountInfo.PrimaryEndpoints.Blob
$osDiskName = $VMName + '-' + 'osdisk'
$blobPath = "vhds/$osDiskName.vhd"
$osDiskURI = '{0}{1}' -f $blobEndpoint, $blobPath

Write-Output "  OS Disk Name: $osDiskNAme"
Write-Output "  OS Disk URI: $osDiskURI"

if ($osType -eq "Windows") {
    $vm = Set-AzureRmVMOSDisk -VM $vm -Name $osDiskName -VHDUri $osDiskURI -SourceImageUri $sourceVHDUri -CreateOption fromImage -Windows
}
else {
    $vm = Set-AzureRmVMOSDisk -VM $vm -Name $osDiskName -VHDUri $osDiskURI -SourceImageUri $sourceVHDUri -CreateOption fromImage -Linux
}

New-AzureRmVM -ResourceGroupName $ResourceGroupName -Location $Location -VM $vm

# Retrieve the connection info
Write-Output "`r`nRetrieving connection information..."
.\Get-Connection-Info.ps1 $VMName -ResourceGroupName $ResourceGroupName
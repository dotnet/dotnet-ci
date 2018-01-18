<#
.SYNOPSIS
    Get-VHDLocation
.DESCRIPTION
    Given an image name, retrieve a VHD URI, either with or without a storage account
.PARAMETER ImageName
    Image name to create a VM with
.PARAMETER StorageAccountName
    Name of the VM to create.
#>

param (
    [string]$ImageName = $(Read-Host -prompt "VM image to lookup"),
    [string]$StorageAccountName = 'dotnetciuservmstorage3',
    [string]$ImageCsv = 'Images.csv'
)

# Read Images.csv
$image = .\Get-Images.ps1 -ImageName $ImageName -Exact

if (!$image) {
    Write-Error "Could not find $ImageName"
}

Write-Output "https://$StorageAccountName.blob.core.windows.net/$($image.Container)/$($image.BlobName)"
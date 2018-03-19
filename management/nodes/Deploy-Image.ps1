<#
.SYNOPSIS
    Deploy-Image.ps1
.DESCRIPTION
    Deploys an image in the storage to a set of targets specified in the image file
#>

param (
    [string]$ImageName,
    [string]$SourceStorageAccount = 'dotnetciuservmstorage3'
)

$imageInfo = .\Get-Images.ps1 -ImageName $ImageName

if (!$imageInfo) {
    Write-Error "Couldn't find image $ImageName"
}

$vhdUri = .\Get-VHD-Location.ps1 -ImageName $ImageName -StorageAccountName $SourceStorageAccount

if (!$vhdUri) {
    Write-Error "Couldn't get VHD URI"
}

.\Deploy-VHD.ps1 -VHDUri $vhdUri -TargetStorageAccounts $imageInfo.StorageAccounts

<#
.SYNOPSIS
    Check-All-Images.ps1
.DESCRIPTION
    Check all the images in the store
#>

# Grab the image info
$images = .\Get-Images.ps1

foreach ($image in $images) {
    .\Check-Image.ps1 -ImageName $image.ImageName
}
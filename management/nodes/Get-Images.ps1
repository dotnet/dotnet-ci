<#
.SYNOPSIS
    Get-Images.ps1
.DESCRIPTION
    Retrieves a set of available .NET CI images and their descriptions from Azure
.PARAMETER ImageName
    Optional name of the image to match against.  Substring match.
.PARAMETER ImageCsv
    Csv file of image infoc
#>

param (
    [string]$ImageName = $null,
    [switch]$Exact = $false,
    [string]$ImageCsv = 'Images.csv'
)

# Read Images.csv

if ($ImageName) {
    if ($Exact) {
        Get-Content $ImageCsv | Select-String '^[^#]' | ConvertFrom-Csv | Where-Object { $_.ImageName -eq $ImageName }
    }
    else {
        Get-Content $ImageCsv | Select-String '^[^#]' | ConvertFrom-Csv | Where-Object { $_.ImageName -match $ImageName }
    }
} else {
    Get-Content $ImageCsv | Select-String '^[^#]' | ConvertFrom-Csv
}
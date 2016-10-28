<#
.SYNOPSIS
    Parse-VHD-Uri.ps1
.DESCRIPTION
    Parse out a VHD URI
.PARAMETER VHDUri
#>

param (
    [string]$VHDUri
)

# Parse out the source URI info
$uriInfo = [regex]::Match($VHDUri, "^https?://(?<storageaccountname>.*).blob.core.windows.net/(?<containername>.*?)/((?<virtualpath>.*)/)?(?<blobname>.*\.vhd)$")

if (!$uriInfo.Success) {
    Write-Error "Could not parse $VHDUri"
    Exit
}

$uriProperties = @{'StorageAccount'=$uriInfo.Groups["storageaccountname"].Value;
                'ContainerName'=$uriInfo.Groups["containername"].Value;
                'Blob'=$uriInfo.Groups["blobname"].Value;
                'VirtualPath'=$uriInfo.Groups["virtualpath"].Value}

Write-Output $uriProperties
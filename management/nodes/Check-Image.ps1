<#
.SYNOPSIS
    Check-Image.ps1
.DESCRIPTION
    Checks that an image was correctly deployed
.PARAMETER ImageName
    Image to check
#>

param (
    [string]$ImageName
)

# Right now we can really only check that the blob exists and is somewhere north of the expected amount

# Ensure logged in
$ctx = Get-AzureRmContext
if (!$ctx) {
    Exit
}

Write-Output "Checking $ImageName"

# Grab the image info
$imageInfo = .\Get-Images.ps1 -ImageName $ImageName -Exact
if (!$imageInfo) {
    Write-Error "Could not locate image $ImageName"
    Exit
}

# Check that the script is valid and available

if (!(Test-Path $imageInfo.StartupScriptPath)) {
    Write-Error "Could find find startup script named $($imageInfo.StartupScriptPath).  Please ensure a startup script is checked in"
}

# Walk all the storage accounts and ensure they all have the image
# If the storage account is marked as 'All', read list of target storage accounts
$targetAccounts = $imageInfo.StorageAccounts
if ($targetAccounts -eq 'All') {
    $targetAccounts = Get-Content 'StorageAccounts.txt' | Select-String '^[^#]' | % { $_.ToString() }
}
else {
    Write-Error "Unexpected storage account name $targetAccounts.  Semicolon separated list NYI"
    Exit
}

$firstSize = -1
$firstStorageAccount = $null

foreach ($targetAccount in $targetAccounts) {
    $targetContext = .\Get-Storage-Context.ps1 -StorageAccountName $targetAccount
    
    $targetVHDUri = .\Get-VHD-Location.ps1 -ImageName $ImageName -StorageAccountName $targetAccount
    
    # Look up the blob VHD
    $blobInfo = Get-AzureStorageBlob -Container $imageInfo.Container -Blob $imageInfo.BlobName -Context $targetContext -ErrorAction SilentlyContinue
    if (!$blobInfo) {
        Write-Error "Could not locate $ImageName at VHD location $targetVHDUri"
        continue
    }
    
    if ($blobInfo.Length -eq 0) {
        Write-Error "Size of VHD in $targetAccount is 0"
        Exit
    }
    
    if (!$firstStorageAccount) {
        $firstSize = $blobInfo.Length
        $firstStorageAccount = $targetAccount
    }
    elseif($firstSize -ne $blobInfo.Length) {
        Write-Error "Size of VHD in $targetAccount ($($blobInfo.Length)) doesn't match $firstStorageAccount ($firstSize)"
    }
    
    Write-Output "$ImageName in $targetAccount is correct."
}

Write-Output "All locations verified"
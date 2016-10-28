<#
.SYNOPSIS
    Deploy-VHD.ps1
.DESCRIPTION
    Deploys a VHD to a set of storage accoutns
.PARAMETER VHDUri
    Uri of VHD to copy
.PARAMETER TargetStorageAccounts
    List of storage accounts that the VHD needs to go to. Defaults to All
.PARAMETER TargetContainer
    Container to put the blob in.  If not specified, defaults to the container name of the source URI
.PARAMETER TargetVirtualPath
    Virtual path to put the blob in.  If not specified, defaults to the virtual path of the source URI
.PARAMETER TargetBlobName
    Blob name to copy to.  If not specified, defaults to the blob name of the source URI
#>

param (
    [string]$VHDUri,
    $TargetStorageAccounts = 'All',
    [string]$TargetContainer = $null,
    [string]$TargetVirtualPath = $null,
    [string]$TargetBlob = $null
)

# Ensure logged in
$ctx = Get-AzureRmContext
if (!$ctx) {
    Exit
}

# If the storage account is marked as 'All', read list of target storage accounts
$targetAccounts = $TargetStorageAccounts
if ($targetAccounts -eq 'All') {
    $targetAccounts = Get-Content 'StorageAccounts.txt' | Select-String '^[^#]' | % { $_.ToString() }
}

$targetAccounts = [System.Collections.ArrayList]$targetAccounts

# Parse out the source URI info
$uriInfo = [regex]::Match($VHDUri, "^https://(?<storageaccountname>.*).blob.core.windows.net/(?<containername>.*?)/((?<virtualpath>.*)/)?(?<blobname>.*\.vhd)$")

if (!$uriInfo.Success) {
    Write-Error "Could not parse $VHDUri"
    Exit
}

$sourceStorageAccountName = $uriInfo.Groups["storageaccountname"].Value
$sourceStorageContainer = $uriInfo.Groups["containername"].Value
$sourceStorageBlob = $uriInfo.Groups["blobname"].Value
$sourceStorageVirtualPath = $uriInfo.Groups["virtualpath"].Value

$targetStorageContainer = $TargetContainer
$targetStorageBlob = $TargetBlob
$targetStorageVirtualPath = $TargetVirtualPath

if (!$targetStorageContainer) {
    $targetStorageContainer = $sourceStorageContainer
}
if (!$targetStorageBlob) {
    $targetStorageBlob = $sourceStorageBlob
}
if (!$targetStorageVirtualPath) {
    $targetStorageVirtualPath = $sourceStorageVirtualPath
}

# For whatever reason I couldn't get Remove to work,
$targetAccounts.Remove($sourceStorageAccountName)
                
Write-Output "Copy details:"
Write-Output "  Storage Account $sourceStorageAccountName -> $targetAccounts"
Write-Output "  Container $sourceStorageContainer -> $targetStorageContainer"
if ($targetStorageVirtualPath -or $sourceStorageVirtualPath) {
    Write-Output "  Virtual Path $sourceStorageVirtualPath -> $targetStorageVirtualPath"
}
Write-Output "  Blob $sourceStorageBlob -> $targetStorageBlob"

$resourceGroups = Get-Content 'ResourceGroups.txt' | Select-String '^[^#]'

$sourceStorageAccountKeys = $null
# Walk each RG and look up the storage key
foreach ($resourceGroup in $resourceGroups) {
    $sourceStorageAccountKeys = Get-AzureRmStorageAccountKey $sourceStorageAccountName -ResourceGroupName $resourceGroup -ErrorAction SilentlyContinue
    if ($sourceStorageAccountKeys) {
        if ($sourceStorageAccountKeys[0]) {
            $sourceStorageAccountKey = $sourceStorageAccountKeys[0].Value
        }
        else {
            $sourceStorageAccountKey = $sourceStorageAccountKeys.Key1
        }
        break
    }
}

if (!$sourceStorageAccountKey) {
    Write-Error "Could not find storage account $sourceStorageAccountName or locate the storage key"
    Exit
}

# Create a storage context for the source
$sourceContext = New-AzureStorageContext -StorageAccountName $sourceStorageAccountName -StorageAccountKey $sourceStorageAccountKey
$sourceBlob = $null
if ($sourceStorageVirtualPath) {
    $sourceBlob = Get-AzureStorageBlob -Blob "$sourceStorageVirtualPath/$sourceStorageBlob" -Container $sourceStorageContainer -Context $sourceContext -ErrorAction SilentlyContinue
}
else {
    $sourceBlob = Get-AzureStorageBlob -Blob $sourceStorageBlob -Container $sourceStorageContainer -Context $sourceContext -ErrorAction SilentlyContinue
}
if (!$sourceBlob) {
    Write-Error "Could not locate source blob $sourceStorageBlob"
    Exit
}


$blobCopies = [System.Collections.ArrayList]@()

foreach ($targetAccount in $targetAccounts) {
    if ($targetStorageVirtualPath) {
        $targetUri = "https://$targetAccount.blob.core.windows.net/$targetStorageContainer/$targetStorageVirtualPath/$targetStorageBlob"
    }
    else {
        $targetUri = "https://$targetAccount.blob.core.windows.net/$targetStorageContainer/$targetStorageBlob"
    }
    
    # Locate the storage account key for the target
    $targetStorageAccountKey = $null
    # Walk each RG and look up the storage key
    foreach ($resourceGroup in $resourceGroups) {
        $targetStorageAccountKeys = Get-AzureRmStorageAccountKey $targetAccount -ResourceGroupName $resourceGroup -ErrorAction SilentlyContinue
        if ($targetStorageAccountKeys) {
            if ($targetStorageAccountKeys[0]) {
                $targetStorageAccountKey = $targetStorageAccountKeys[0].Value
            }
            else {
                $targetStorageAccountKey = $targetStorageAccountKeys.Key1
            }
            break
        }
    }
    
    if (!$targetStorageAccountKey) {
        Write-Error "Could not find target storage account $targetAccount or locate the storage key, skipping"
        continue
    }

    # Create a storage context for the target
    $targetContext = New-AzureStorageContext -StorageAccountName $targetAccount -StorageAccountKey $targetStorageAccountKey
    
    # Ensure that the container is created if not existing
    $existingContainer = Get-AzureStorageContainer -Context $targetContext -Name $targetStorageContainer -ErrorAction SilentlyContinue
    
    if (!$existingContainer) {
        Write-Output "Target storage container $targetStorageContainer doesn't exist, creating"
        New-AzureStorageContainer -Context $targetContext -Name $targetStorageContainer
    }
    
    $fullTargetBlobName = $targetStorageBlob
    if ($targetStorageVirtualPath) {
        $fullTargetBlobName = "$targetStorageVirtualPath/$targetStorageBlob"
    }
    
    $newBlobCopy = Start-AzureStorageBlobCopy -CloudBlob $sourceBlob.ICloudBlob -Context $sourceContext -DestContext $targetContext -DestContainer $targetStorageContainer -DestBlob $fullTargetBlobName
    
    if ($newBlobCopy) {
        $blobCopies.Add($newBlobCopy)
    }
    
    Write-Output "Started $vhdURI -> $targetUri"
}

# Waiting till all copies done
$allFinished = $false
while (!$allFinished) {
    $allFinished = $true
    $operationIndex = 0
    foreach ($blobCopy in $blobCopies) {
        $operationIndex++
        $blobCopyState = $blobCopy | Get-AzureStorageBlobCopyState
        if ($blobCopyState.Status -eq "Pending")
        {
            $allFinished = $false
            $percent = ($blobCopyState.BytesCopied * 100) / $blobCopyState.TotalBytes
            $percent = [math]::round($percent,2)
            $blobCopyName = $blobCopyState.CopyId
            Write-Progress -Id $operationIndex -Activity "Copying ($operationIndex)... " -PercentComplete $percent -CurrentOperation "Copying $blobCopyName"
        }
    }
    Start-Sleep -s 30
}

Write-Output "All operations complete"

<#
.SYNOPSIS
    Get-Storage-Context.ps1
.DESCRIPTION
    Get a storage context for a storage account
.PARAMETER StorageAccountName
    Storage account to get a context for
#>

param (
    [string]$StorageAccountName
)

# Ensure logged in
$ctx = Get-AzureRmContext
if (!$ctx) {
    Exit
}

$resourceGroupName = .\Get-Resource-Group.ps1 -StorageAccountName $StorageAccountName

$storageAccountKeys = Get-AzureRmStorageAccountKey $StorageAccountName -ResourceGroupName $resourceGroupName -ErrorAction SilentlyContinue
if (!$storageAccountKeys) {
    Write-Error "Could not get storage account key for $StorageAccountName"
    Exit
}

# Workaround for different powershell versions
if ($storageAccountKeys[0]) {
    $storageAccountKey = $storageAccountKeys[0].Value
}
else {
    $storageAccountKey = $storageAccountKeys.Key1
}

New-AzureStorageContext -StorageAccountName $StorageAccountName -StorageAccountKey $storageAccountKey

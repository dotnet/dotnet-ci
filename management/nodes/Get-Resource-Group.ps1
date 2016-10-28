<#
.SYNOPSIS
    Get-Resource-Group.ps1
.DESCRIPTION
    Get the resource group for a storage account
.PARAMETER StorageAccountName
    Storage account to find
#>

param (
    [string]$StorageAccountName
)

# Ensure logged in
$ctx = Get-AzureRmContext
if (!$ctx) {
    Exit
}

$resourceGroups = .\Get-Available-Resource-Groups.ps1

# Walk each RG and look up the storage key
foreach ($resourceGroup in $resourceGroups) {
    $storageAccount = Get-AzureRmStorageAccount $StorageAccountName -ResourceGroupName $resourceGroup -ErrorAction SilentlyContinue
    if ($storageAccount) {
        Write-Output $resourceGroup
    }
}

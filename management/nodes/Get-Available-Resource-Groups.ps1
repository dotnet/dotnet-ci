<#
.SYNOPSIS
    Get-Available-Resource-Groups.ps1
.DESCRIPTION
    Gets the available resource groups
#>

Get-Content 'ResourceGroups.txt' | Select-String '^[^#]' | % { $_.ToString() }
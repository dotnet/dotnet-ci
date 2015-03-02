<#
.SYNOPSIS
    Cycle-Nodes
.DESCRIPTION
    Cycles Nodes in the dotnet-ci azure pool
.PARAMETER Password
    Password to create the dotnet-bot account with
.PARAMETER Pattern
    Pattern of machine names to cycle
.PARAMETER NewImage
    New image to place on those machines
.PARAMETER AllowMissingMachines
    If machines are not currently in azure, allows creation
.PARAMETER Verbose
    Verbose tracing of the script
#>

param (
    [string]$Password = $(Read-Host -assecurestring -prompt "Password for user"),
    [string]$Pattern = $(Read-Host -prompt "Pattern for replacement"),
    [string]$NewImage = $null,
    [switch]$AllowMissingMachines = $false,
    [switch]$Verbose = $false
)

$ServiceName="dotnet-ci-nodes"
$Username="dotnet-bot"

# Read the inventory

$machines = Get-Content inventory.txt | ConvertFrom-Csv

# Walk the machines, append the current count and check against the regex provided.  If it matches,
# then cycle the machine to the new image.

foreach ($machine in $machines)
{
    for ($i = 1; $i -le $machine.Count; $i++)
    {
        $fullMachineName = $machine.BaseName + $i

        if ($Verbose)
        {
            Write-Host -NoNewLine "Checking machine $fullMachineName against pattern $Pattern..."
        }
        
        if ($fullMachineName -match $Pattern)
        {
            if ($Verbose)
            {
                Write-Host "Matched"
            }
            
            $imageToUse = $NewImage

            if ($imageToUse -eq $null)
            {
                $imageToUse = $NewImage
            }

            Write-Host "Cycling $fullMachineName to $NewImage"

            switch ($machine.OSType)
            {
                "Windows"
                {
                    # Check for existence of the old machine

                    $existingVM = Get-AzureVM -ServiceName $ServiceName -Name $fullMachineName

                    if (-not $existingVM)
                    {
                        if ($AllowMissingMachines)
                        {
                            Write-Warning "VM $fullMachineName did not exist...creating"
                        }
                        else
                        {
                            throw "VM $fullMachineName did not exist."
                        }
                    }
                    else
                    {
                        # Delete the old machine
                    
                        Remove-AzureVM -ServiceName $ServiceName -Name $fullMachineName -DeleteVHD
                    }

                    # Create the new one

                    $newVM = New-AzureQuickVM -Windows -ServiceName $ServiceName -Name $fullMachineName -ImageName $imageToUse -AdminUsername $Username -Password $Password -InstanceSize $machine.Size
                    
                    if (!$newVM)
                    {
                        throw "Could not create $fullMachineName"
                    }
                }
                "Linux"
                {
                    throw "Linux cycling not supported"
                }
                default
                {
                    throw "Unknown OS type $machine.OSType"
                }
            }
        }
        else
        {
            if ($Verbose)
            {
                Write-Host "Not Matched"
            }
        }
    }
}


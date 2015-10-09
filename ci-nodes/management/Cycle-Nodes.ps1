<#
.SYNOPSIS
    Cycle-Nodes
.DESCRIPTION
    Cycles Nodes in the dotnet-ci azure pool
.PARAMETER Password
    Password to create the dotnet-bot account with
.PARAMETER Machines
    Pattern of machine names to cycle
.PARAMETER NewImage
    New image to place on those machines
.PARAMETER AllowMissingMachines
    If machines are not currently in azure, allows creation
.PARAMETER Auto
    Automatic cycling of nodes.  Connects to Jenkins through the CLI to take machines offline, wait for idle, and then cycle
    the machine to the new image.
.PARAMETER DryRun
    Make no changes, dry run only
.PARAMETER IdleWait
    If Auto is on, number of minutes to wait before checking whether a machine is idle
.PARAMETER MaxIdleTimes
    Maximum number of wait tries before aborting an image update for a busy machine.
.PARAMETER PrivateKey
    Private SSH key required for automatic image update
.PARAMETER Verbose
    Verbose tracing of the script
#>

param (
    [string]$Password = $(Read-Host -assecurestring -prompt "Password for user"),
    [string]$Machines = $(Read-Host -prompt "Regex pattern for machines to cycle"),
    [string]$NewImage = $(Read-Host -prompt "New image to cycle machines to."),
    [switch]$Auto = $false,
    [switch]$DryRun = $false,
    [string]$PrivateKey = $null,
    [int]$IdleWait = 10,
    [int]$MaxIdleTimes = 24,
    [switch]$AllowMissingMachines = $false,
    [switch]$Verbose = $false
)

$Username="dotnet-bot"
$JenkinsInstance="http://dotnet-ci.cloudapp.net"
$JenkinsCLIJarURL="$JenkinsInstance/jnlpJars/jenkins-cli.jar"
$JenkinsCLIJar="$env:TEMP\jenkins-cli.jar"

Write-Host "Reading inventory from inventory.txt"

# Read the inventory

$inventory = Get-Content inventory.txt | ConvertFrom-Csv

# Make a list of the machines that need to get updates.

$machinesThatNeedUpdates = @()

foreach ($machine in $inventory)
{
    for ($i = 1; $i -le $machine.Count; $i++)
    {
        $fullMachineName = $machine.BaseName + $i

        if ($Verbose)
        {
            Write-Host -NoNewLine "Checking machine $fullMachineName against pattern $Machines..."
        }
        
        if ($fullMachineName -match $Machines)
        {
            Write-Host "Matched"
            
            $machinesThatNeedUpdates += @{
                FullMachineName = $fullMachineName;
                Port = [int]$machine.RdpSshStart + [int]$i;
                OSType = $machine.OSType;
                Cycled = $false;
                Size = $machine.Size;
                ServiceName = $machine.ServiceName
                }
        }
        else {
            Write-Host "Not Matched"
        }
    }
}
            
if ($Verbose) {
    Write-Host "The following machines will be updated: "
    foreach($entry in $machinesThatNeedUpdates) {
        $entry.GetEnumerator() | Sort-Object Name
        Write-Host
    }
}

# Check to see whether the new vm image actually exists

$newImageCheck = Get-AzureVMImage $NewImage

if (!$newImageCheck)
{
    throw "VM $NewImage did not exist, aborting"
}

# 1. Walk the machine update list and set everything offline.
# 2. Walk the machine update list and check if idle.  If machine is idle, cycle and set online
# 3. At end of loop, if not all machines were updated, wait $IdleWait mins and restart go back to 2.
# 4. If not all machines updated after MaxIdleWaits, set all machines back online.

if ($Auto)
{
    # Download Jar to temp
    WGet $JenkinsCLIJarURL -OutFile $JenkinsCLIJar
    
    if (!$DryRun) {
        Write-Host "Taking machines offline for auto-update"
        
        foreach ($machine in $machinesThatNeedUpdates)
        {
            $fullMachineName = $machine.FullMachineName
            
            # Determine whether the node exists
            & java -jar $JenkinsCLIJar -i $PrivateKey -s $JenkinsInstance get-node $fullMachineName
            if (-not $?)
            {
                $machine.Exists = $false
                Write-Host "Node $fullMachineName doesn't exist, skipping"
                continue
            }
            
            & java -jar $JenkinsCLIJar -i $PrivateKey -s $JenkinsInstance offline-node $fullMachineName -m "Taking offline for automated image update"
            
            if (-not $?)
            {
                throw "Failed to take $fullMachineName offline"
            }
        }
    }
}

$waitsLeft = $MaxIdleTimes
$notAllUpdated = $false

do
{
    $notAllUpdated = $false

    foreach ($machine in $machinesThatNeedUpdates)
    {
        # If already updated, skip
        
        if ($machine.Cycled)
        {
            continue
        }
        
        $fullMachineName = $machine.FullMachineName
        $port = $machine.Port
        
        # If in auto-mode, check to see whether the machine is quiet now

        if ($Auto -and $machine.Exists)
        {
            Write-Output "Checking status of $fullMachineName"
            
            $idleOutput = & java -jar $JenkinsCLIJar -i $PrivateKey -s $JenkinsInstance groovy check-is-idle.groovy $fullMachineName
            if ($idleOutput -contains "Busy")
            {
                Write-Host "$fullMachineName is Busy, waiting till later"
                # Machine is busy.
                $notAllUpdated = $true
                continue
            }
            elseif ($idleOutput -contains "Idle")
            {
                # Ready to go.
                if ($Verbose)
                {
                    Write-Host "$fullMachineName is Idle and ready to be cycled"
                }
            }
            else
            {
                # Unknown response from the script
                throw "Unknown response '$idleOutput' from check-is-idle.groovy script"
            }
        }

        Write-Host "Cycling $fullMachineName to $NewImage with RDP/SSH on $port"
        
        if ($DryRun) {
            # Dry run, mark as cycled and continue
            $machine.Cycled = $true
            continue;
        }
        
        # Check for existence of the old machine

        $existingVM = Get-AzureVM -ServiceName $machine.ServiceName -Name $fullMachineName

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
        
            Remove-AzureVM -ServiceName $machine.ServiceName -Name $fullMachineName -DeleteVHD
        }
        
        $machineSize = $machine.Size
        $osType = $machine.OSType
        
        switch ($osType)
        {
            "Windows"
            {
                # Create the new one

                $newVM = New-AzureQuickVM -Windows -ServiceName $machine.ServiceName -Name $fullMachineName -ImageName $NewImage -AdminUsername $Username -Password $Password -InstanceSize $machineSize
                
                if (!$newVM)
                {
                    throw "Could not create $fullMachineName"
                }
                
                Get-AzureVM -ServiceName $machine.ServiceName -Name $fullMachineName | Set-AzureEndpoint -Name "RemoteDesktop" -PublicPort $port -LocalPort 3389 -Protocol TCP | Update-AzureVM
                
                Write-Host "Remember, mstsc /v:$machine.ServiceName.cloudapp.net:$port in a few minutes to start the jenkins process"
            }
            "Linux"
            {
                $newVM = New-AzureQuickVM -Linux -ServiceName $machine.ServiceName -Name $fullMachineName -ImageName $NewImage -LinuxUser $Username -Password $Password -InstanceSize $machineSize
                
                if (!$newVM)
                {
                    throw "Could not create $fullMachineName"
                }
                
                # Alter the endpoint so that the SSH port is predictable
                
                Get-AzureVM -ServiceName $machine.ServiceName -Name $fullMachineName | Set-AzureEndpoint -Name "SSH" -PublicPort $port -LocalPort 22 -Protocol TCP | Update-AzureVM
            }
            default
            {
                throw "Unknown OS type $osType"
            }
        }
        
        # Done cycling.  Set the machine online if in auto mode
        
        if ($Auto -and $machine.Exists)
        {
            if ($osType -eq "Linux")
            {
                Write-Host "[Temporary workaround] Temporary disk on $fullMachineName ($machine.ServiceName.cloudapp.net:$port) may not be writeable, please SSH and chmod 777 the disk"
            }
            else
            {
                & java -jar $JenkinsCLIJar -i $PrivateKey -s $JenkinsInstance online-node $fullMachineName
                
                if (-not $?)
                {
                    throw "Failed to bring $fullMachineName back online"
                }
            }
        }

        $machine.Cycled = $true
    }
    
    # If in auto mode and we haven't updated everything, wait the idle time mins
    
    if ($Auto -and $notAllUpdated)
    {
        if ($waitsLeft -eq 0)
        {
            # Print those machines that haven't been updated
            
            foreach ($entry in $machinesThatNeedUpdates)
            {
                if ($entry.Cycled)
                {
                    continue
                }
                
                $entry.GetEnumerator() | Sort-Object Name
                Write-Host
            }
            throw "Failed to update all machines, some left."
        }
        
        $waitsLeft--
        Sleep $($IdleWait * 60)
    }
}
while ($notAllUpdated)

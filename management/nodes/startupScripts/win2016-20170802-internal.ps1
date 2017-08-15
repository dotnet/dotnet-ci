Set-ExecutionPolicy Unrestricted

$machineSetupLog = 'C:\setup.log'
$jenkinsServerUrl = $args[0]
echo ('Jenkins Server URL: {0}' -f $jenkinsServerUrl) | Out-File -Append $machineSetupLog
$vmName = $args[1]
echo ('Virtual Machine Name: {0}' -f $vmName) | Out-File -Append $machineSetupLog
$secret = $args[2]
echo ('Secret: {0}' -f $secret) | Out-File -Append $machineSetupLog

$winLogonRegistryPath = 'hklm:\software\microsoft\windows nt\currentversion\winlogon\'
$jenkinsDirectory = 'C:\Jenkins'
$jenkinsLaunchCmd = Join-Path -Path $jenkinsDirectory -ChildPath 'launch.cmd'
$jenkinsStartupScript = Join-Path -Path $jenkinsDirectory -ChildPath 'jenkins-windows-startup.ps1'
$jenkinsLogFile = Join-Path -Path $jenkinsDirectory -ChildPath 'log.txt'
# The image needs to have LUA off to run properly. If you want the VM to have administrative permissions for
# everything set $enableLUA to $false.
$enableLUA = $true
$enableLUARegistryPath = 'hklm:\software\Microsoft\Windows\CurrentVersion\policies\system\'

# Create the Jenkins directory
if (!(Test-Path -Path $jenkinsDirectory))
{
    Write-Output ('Creating Jenkins Directory: {0}' -f $jenkinsDirectory) | Out-File -Append $machineSetupLog
    New-Item -Path $jenkinsDirectory -Force -ItemType Directory
}

# Set the time zone
tzutil /s "Pacific Standard Time"

# Generate the launch cmd
$content = 'powershell.exe {0} -URL {1} -VMName {2} -Secret {3} > {4}' -f $jenkinsStartupScript, $jenkinsServerUrl, $vmName, $secret, $jenkinsLogFile
Write-Output ('Creating Jenkins Launcher. File: {0}; Content: {1}' -f $jenkinsLaunchCmd, $content) | Out-File  -Append $machineSetupLog
$content | Out-File -FilePath $jenkinsLaunchCmd -Encoding ascii -Force

# Set up auto-login
Write-Output 'Registering dotnet-bot for auto-login' | Out-File -Append $machineSetupLog

New-ItemProperty -Path $winLogonRegistryPath -Name 'DefaultUserName' -Value 'dotnet-bot' -PropertyType string -Force
New-ItemProperty -Path $winLogonRegistryPath -Name 'DefaultPassword' -Value '<pass>' -PropertyType string -Force

New-ItemProperty -Path $winLogonRegistryPath -Name 'AutoAdminLogon' -Value '1' -PropertyType string -Force

# Enable LUA
if ($enableLUA)
{
    Write-Output 'Enabling UAC/LUA' | Out-File -Append $machineSetupLog
    New-ItemProperty -Path $enableLUARegistryPath -Name 'EnableLUA' -Value 1 -PropertyType DWORD -Force
}

# Register the logon task if it doesn't exist
$scheduledTaskExists = Get-ScheduledTask | Where-Object { $_.TaskName -like "Jenkins-Startup" }

if (!$scheduledTaskExists) {
    Write-Output 'Registering the launch script to run on user login' | Out-File -Append $machineSetupLog
    $scheduledTaskTrigger = New-ScheduledTaskTrigger -AtLogOn -User dotnet-bot
    $scheduledTaskAction = New-ScheduledTaskAction -Execute C:\Jenkins\launch.cmd
    $scheduledTaskSettings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -Priority 4 -ExecutionTimeLimit ([TimeSpan]::Zero)
    $scheduledTaskPrincipal = New-ScheduledTaskPrincipal -UserId dotnet-bot -LogonType Interactive
    Register-ScheduledTask Jenkins-Startup -Action $scheduledTaskAction -Trigger $scheduledTaskTrigger -Principal $scheduledTaskPrincipal -Settings $scheduledTaskSettings
}

Write-Output 'Rebooting' | Out-File -Append $machineSetupLog
Restart-Computer -Force

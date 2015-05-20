This folder contains scripts that start the job executors on various machines for the Azure cloud plug-in.

How to configure a machine to start an job executors automatically (Azure).

Scripts involved:

* bootstrap-windows.cmd and bootstrap-windows.ps1 - Place in root folder (C).
* jenkins-windows-startup.ps1 - Place in jenkins userContent folder.

Add a scheduled task that calls bootstrap-windows.cmd on startup.  Should be configured to start a few minutes (say 5) after startup to ensure that powershell and other necessary components can be initialized.  The cmd file invokes bootstrap-windows.ps1, which connects to the Jenkins system and downloads jenkins-windows-startup.ps1.

In this way, the startup script can be altered easily without remaking the image.  The bootstrap is just minimal enough to get off the ground.
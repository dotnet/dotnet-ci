# DotNet CI Images

|Image Name|Operating System|Configuration Notes|
|---|---|---|
|dci-ub-bld-2|Ubuntu 14.04 LTS|Configured to run connection script on startup through the "jenkins-slave" service (runs as dotnet-bot).  Includes cmake, java and clang in image.|
|dci-win-bld-3|Windows Server 2012 R2|Configured to run connection script on startup through scheduled task.  Includes cmake, jav, perl, and VS2013 Community Edition|

# Table of machines

|Machine Name|Current Image|VM or Machine Size|Labels|
|---|---|---|
|dci-ub-fbld-1|dci-ub-bld-2|Standard_D3|ubuntu, ubuntu-fast|
|dci-ub-fbld-2|dci-ub-bld-2|Standard_D3|ubuntu, ubuntu-fast|
|dci-win-bld-1|dci-win-bld-3|Basic_A3|windows|
|dci-win-bld-2|dci-win-bld-3|Basic_A3|windows|
|dci-win-bld-3|dci-win-bld-3|Basic_A3|windows|
|dci-win-bld-4|dci-win-bld-3|Basic_A3|windows|
|dci-win-bld-5|dci-win-bld-3|Basic_A3|windows|
|dci-win-bld-6|dci-win-bld-3|Basic_A3|windows|
|dci-win-bld-7|dci-win-bld-3|Basic_A3|windows|
|dci-win-bld-8|dci-win-bld-3|Basic_A3|windows|
|dci-win-fbld-1|dci-win-bld-3|Standard_D3|windows, windows-fast|
|dci-win-fbld-2|dci-win-bld-3|Standard_D3|windows, windows-fast|
|dci-win-fbld-3|dci-win-bld-3|Standard_D3|windows, windows-fast|
|dci-win-fbld-4|dci-win-bld-3|Standard_D3|windows, windows-fast|
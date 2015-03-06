# DotNet CI Images

|Image Name|Operating System|Configuration Notes|
|---|---|---|
|dci-ub-bld-3|Ubuntu 14.04 LTS|Configured to run connection script on startup through the "jenkins-slave" service (runs as dotnet-bot).|
|dci-win-bld-5|Windows Server 2012 R2|Configured to run connection script on startup through scheduled task.  Includes cmake, java, perl, python, gnuwin32, and VS2013 Community Edition|

# Table of machines

|Machine Name|Current Image|VM or Machine Size|Labels|
|---|---|---|
|dci-ub-fbld-1|dci-ub-bld-3|Standard_D3|ubuntu, ubuntu-fast|
|dci-ub-fbld-2|dci-ub-bld-3|Standard_D3|ubuntu, ubuntu-fast|
|dci-win-fbld-1|dci-win-bld-5|Standard_D3|windows, windows-fast|
|dci-win-fbld-2|dci-win-bld-5|Standard_D3|windows, windows-fast|
|dci-win-fbld-3|dci-win-bld-5|Standard_D3|windows, windows-fast|
|dci-win-fbld-4|dci-win-bld-5|Standard_D3|windows, windows-fast|
|dci-win-fbld-5|dci-win-bld-5|Standard_D3|windows, windows-fast|
|dci-win-fbld-6|dci-win-bld-5|Standard_D3|windows, windows-fast|
|dci-win-fbld-7|dci-win-bld-5|Standard_D3|windows, windows-fast|
|dci-win-fbld-8|dci-win-bld-5|Standard_D3|windows, windows-fast|
|dci-win-fbld-9|dci-win-bld-5|Standard_D3|windows, windows-fast|
|dci-win-fbld-10|dci-win-bld-5|Standard_D3|windows, windows-fast|

# Software

## dci-ub-fbld-3

|Software|Notes|
|clang-3.5|Install as 3.5 not clang.|
|libunwind-dev|Required for coreclr|
|lldb-3.5-dev|Required for coreclr|
|cmake|2.8 is latest available on deb|
|git|Standard git|
|git|Standard git|
|java8|Standard java|
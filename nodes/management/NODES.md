This document contains information about the current state of the machines used in Jenkins.  Please update as software changes in case we need to rebuild.

# Table of machines

|Machine Name|Current Image|VM or Machine Size|Notes|
|---|---|---|---|
|dci-mac-bld-1|N/A|Standard_D3||
|dci-mac-bld-2|N/A|Standard_D3|Roslyn on mac uses this exclusively|
|dci-fbsd-fbld-1|dci-fbsd-bld-4|Standard_D3||
|dci-fbsd-fbld-2|dci-fbsd-bld-4|Standard_D3||
|dci-ub-fbld-1|dci-ub-bld-8|Standard_D3||
|dci-ub-fbld-2|dci-ub-bld-8|Standard_D3||
|dci-win-fbld-1|dci-win-bld-22|Standard_D3||
|dci-win-fbld-2|dci-win-bld-22|Standard_D3||
|dci-win-fbld-3|dci-win-bld-22|Standard_D3||
|dci-win-fbld-4|dci-win-bld-22|Standard_D3||
|dci-win-fbld-5|dci-win-bld-22|Standard_D3||
|dci-win-fbld-6|dci-win-bld-22|Standard_D3||
|dci-win-fbld-7|dci-win-bld-22|Standard_D3||
|dci-win-fbld-8|dci-win-bld-22|Standard_D3||
|dci-win-fbld-9|dci-win-bld-?? + mods|Standard_D3|Out for investigation|
|dci-win-fbld-10|dci-win-bld-22|Standard_D3||
|dci-win-fbld-11|dci-win-bld-22|Standard_D3||
|dci-win-fbld-12|dci-win-bld-22|Standard_D3||
|dci-win-fbld-13|dci-win-bld-22|Standard_D3||
|dci-win-fbld-14|dci-win-bld-22|Standard_D3||
|dci-win-fbld-15|dci-win-bld-22|Standard_D3||

# Image details

## dci-fbsd-bld-4

FreeBSD 10.1 image, custom created from an image in the VM library, then modified.  Uses rc.d to start up the jenkins node.  See the jenkins bsd startup script in this repo.  Pulled the azure Waagent from the github repo.

|Software|Notes|
|---|---|
|clang-3.5||
|llvm-3.5||
|libunwind-dev||
|lldb-3.5-dev||
|cmake 2.8||
|git||
|java8||

## dci-ub-bld-8

Ubuntu 14.04 image, created from the stock image in the Azure library.  Uses init.d to start the jenkins node.  This is problematic as it doesn't successfully mark the temporary disk as writeable by non-root.  Needs to be fixed.

|Software|Notes|
|---|---|
|clang-3.5||
|llvm-3.5||
|libunwind-dev||
|lldb-3.5-dev||
|cmake|2.8 is latest available on deb|
|git||
|java8||
[python 2.7.6|Could upgrade to latest but LLVM uses older version|
|libsll-dev| For crypto library support|
|cppcheck|For coreclr outerloop checking job|
|sloccount|For coreclr outerloop checking job|
|gettext||
|mono|For roslyn|
|mono-devel|For roslyn|
|mono-xbuild|For roslyn|
|zlib1g||
|zlib1g-dev||

## dci-win-bld-22

Ubuntu 14.04 image, created from the stock image in the Azure library.  Uses init.d to start the jenkins node.  This is problematic as it doesn't successfully mark the temporary disk as writeable by non-root.  Needs to be fixed.

|Software|Notes|
|---|---|
|clang 3.5||
|llvm 3.5||
|cmake|3.2|
|git||
|java8||
[Strawbery Perl||
[python 3.2||
|VS 2013 Community||

## Mac mini's

Basic Mac Mini machines.  Currently sitting in 3512.

|Software|Notes|
|---|---|
|xcode command line tools||
|cmake|3.2|
|git||
|java8||
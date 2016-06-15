@echo off
cd /d %userprofile%\Desktop\reboot\
FOR /f "tokens=1,2" %%i in (arguments.txt) do py.exe -3 reboot.py %%i %%j > "result.txt"
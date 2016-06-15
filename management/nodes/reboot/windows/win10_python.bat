SET /p URL=<D:\reboot_url.txt
powershell -command "& { (New-Object Net.WebClient).DownloadFile('%URL%', '%userprofile%\Desktop\reboot\reboot.py') }"
echo %1 %2 > %userprofile%\Desktop\reboot\arguments.txt
schtasks /run /tn "RebootScript"
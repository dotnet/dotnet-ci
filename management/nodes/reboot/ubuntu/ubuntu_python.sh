URL=$(cat /media/$(logname)/SHARE/reboot_url.txt)
curl -s -o $HOME/Desktop/reboot.py -O $URL
python3 $HOME/Desktop/reboot.py $1 $2
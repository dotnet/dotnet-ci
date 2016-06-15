URL=$(cat /Volumes/SHARE/reboot_url.txt)
curl -s -o $HOME/Desktop/reboot.py -O $URL
/usr/local/bin/python3 $HOME/Desktop/reboot.py $1 $2
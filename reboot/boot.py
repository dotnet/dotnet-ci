# -*- coding: utf-8 -*-
import os
import sys
import fileinput
import subprocess
import platform
import shutil
import re
print("windows=1\nubuntu=2\nOSX=4")

WINDOWS = "1"
UBUNTU = "2"
OSX = "4"

def find_file(filename,path):
    for root,dirs,files in os.walk(path):
        for name in files:
            if name==filename:
                return os.path.abspath(os.path.join(root, name))

def replace_text(file,tempfile,destext):
    f = open( file,'r+' )
    open(tempfile, 'w').write(re.sub(".*?default_selection\\s+\\d", destext, f.read()))
    f.close()

if(len(sys.argv) != 3):
    print("Invalid usage. Expected 2 arguments: FROM_OS_LABEL, TO_OS_LABEL")
    exit()

if(sys.argv[1] == WINDOWS):
    print ("Call Windows tasks")
    subprocess.call("mountvol z: /s")
    result=find_file('refind.conf','z:\\')
    if os.path.exists("z:\\refind_backup.conf"):
        print ("The copy of orignal refind.conf already exists")
    else:
        shutil.copy(result,"z:\\refind_backup.conf")
    print (result)
    tempfile="d:\\refind1.conf"
    replace_text(result,tempfile,"default_selection " + sys.argv[2])
    shutil.copy(tempfile,result);
    print ("The next boot OS has changed. Shutting down.")
    subprocess.call("shutdown /t 5 /r")

elif(sys.argv[1] == UBUNTU):
    print ("Call Linux tasks")
    os.system("sudo -bash")
    result=find_file('refind.conf','/boot/efi')
    print (result)
    
    # backup refind.conf
    if os.path.exists("/boot/efi/refind_backup.conf"):
       print ("The copy of orignal refind.conf already exists")
    else:
       shutil.copy(result,"/boot/efi/refind_backup.conf")

    #replace the boot text
    tempfile = "/boot/efi/refind_temp.conf"
    #tempfile = "/home/peptest/Desktop/refind_temp.conf"
    replace_text(result,tempfile,"default_selection " + sys.argv[2])
    shutil.copy(tempfile,result)
    print ("The next boot OS has changed. Shutting down.")
    os.system("sudo reboot")

elif(sys.argv[1] == OSX):
    #mount EFI partition
    print ("Call IOS tasks")
    if os.path.exists("/Volumes/efi"):
        print ("Folder /Volumes/efi already exists")
    else:
        os.mkdir("/Volumes/efi")
    os.system("sudo mount -t msdos /dev/disk0s1 /Volumes/efi")

    #find refind.conf
    result=find_file('refind.conf','/Volumes/efi')
    print (result)
    
    # backup refind.conf
    if os.path.exists("/Volumes/efi/refind_backup.conf"):
       print ("The copy of orignal refind.conf already exists")
    else:
       shutil.copy(result,"/Volumes/efi/refind_backup.conf")

    #replace the boot text
    tempfile = "/Volumes/efi/refind_temp.conf"
    #tempfile = "/Users/peptest/Desktop/refind_temp.conf"
    replace_text(result,tempfile,"default_selection " + sys.argv[2])
    shutil.copy(tempfile,result)
    print ("The next boot OS has changed. Shutting down.")
    os.system("sudo reboot")
    
else:
    print ("No operating system found with label " + sys.argv[1])

### Prerequisites

Follow the setup steps from v-jiba to get a triple-boot machine</br>
*Note: To connect your laptop to the same network as your master computer, you may need a wired connection if you have driver issues*

*Note: Consider looking at <https://github.com/dotnet/dotnet-ci-internal/blob/master/docs/NODE-STARTUP.md> for how to setup your connection to Jenkins*

**Note**: This setup was made assuming that there is a shared drive between all operating systems.</br>
 + This drive should contain a file called ```reboot_url.txt``` which specifies where to find Python file that manages rebooting the operating system</br>
 + **This shared drive is assumed to be named:**</br>
    ```SHARE``` on Unix systems</br>
    ```D:\``` on Windows

*Note*: You can find the address of the master machine using either ```ipconfig``` (Windows) or ```ifconfig``` (Unix)

### For All OS
1. Once you install the plugin, you need to go to ```Manage Jenkins > Configure System > Cloud``` and select ```Add New Cloud > Os Provisioner```
2. Place in the ```SHARE``` drive a file called ```reboot_url.txt``` 
3. In the ```reboot_url``` file, add a link to where the reboot script is stored.</br>
  *Example*: <https://github.com/dotnet/dotnet-ci/blob/master/management/nodes/reboot/tripleboot-reboot-script.py>

### Windows
1. Setup auto-login to the admin account (See <https://support.microsoft.com/en-us/kb/324737>)
2. Turn off sleep mode (from Power Options in Control Panel)
3. Exctract the contents of <https://github.com/dotnet/dotnet-ci/tree/master/management/nodes/reboot/windows> to a folder called ```reboot``` on your desktop
  1. Download TortoiseSVN from <https://tortoisesvn.net/downloads.html>
  2. Right-click on your desktop and select ```TurtoiseSVN>Export```
  3. For URL of repository enter ```https://github.com/dotnet/dotnet-ci/trunk/management/nodes/reboot/windows``` (*Note: The URL uses ```trunk``` and not ```tree/master```*)
  4. For the Export directory enter ```C:\Useres\USERNAME_HERE\Desktop\reboot```
  5. Press OK
4. Setup the agent for this OS on Jenkins</br>
	**If using Java Web Start**:
  1. Install Java <https://java.com/en/download/>
  2. Go to the Jenkins page for your new node from the Windows machine you're setting up
  3. Select ```Launch Agent from Browser``` and save the file as ```slave-agent.jnlp``` and place it on your desktop.
  4. Launch the program once and accept the Java security prompt
  5. Open cmd prompt and type ```cd %userprofile%\Desktop\reboot```
  6. Run the command ```launch_jenkins_task.bat```</br>
	*Note*: This will make the computer connect to Jenkins automatically on logon
5. Consider doing something to get rid of the "Do you want to run this application" popup when you run Jenkins
6. Install Python3 (<http://python.org/downloads/windows/>)</br>
	**Note:** Select ```Add Python to PATH``` from the installer. Be sure to select make sure the command ```py.exe -3``` is recognized after installation.
7. To run the reboot script as admin without the UAC prompt, follow these steps:
  1. Open a cmd **as administrator** and go type ```cd %userprofile%\Desktop\reboot```
  2. Run the command ```create_reboot_task.bat```</br>
		*Note: This will allow you to run the reboot script without UAC prompt. See <http://www.sevenforums.com/tutorials/11949-elevated-program-shortcut-without-uac-prompt-create.html>*
8. Make the ```reboot_script.bat``` file read-only (for security as it can be run as admin by any user)
  1. Right-click on ```reboot_script.bat``` and select ```properties```
  2. On ```General```, tick the box ```Read-only```
  3. Go the ```Security``` tab and select ```Advanced```
  4. Click on ```Disable Inheritance``` and select ```Convert inherited permissions into explicit permissions on this object```
  5. Press ```Ok```
  6. On the ```Security``` tab, click ```Edit```
  7. Select the user which Jenkins will run under and uncheck ```Write```
  8. Press ```Ok``` and close the ```Properties``` window.
9. Consider taking extra steps to disable the "Your Java version is out of date" message
10. When adding this to a multi-os agent on Jenkins, user the following reboot command: ```C:\Users\USERNAME_HERE\Desktop\reboot\win10_python.bat```</br>
		_**Explanation**:
		This will call ```win10_python.bat``` which will save information about the os to launch into ```arguments.txt``` and then call the Windows reboot task</br>
		The reboot task will then call ```reboot_script.bat``` with admin priviledges which will load the ```arguments.txt``` and then download and run the reboot python file_

### Ubuntu
1. Setup auto-login to the admin account
  1. ```System Settings > User Accounts```
  2. Press ```Unlock``` at the top-right
  3. set ```Automatic login``` to on
2. Turn off sleep mode
  1. ```System Settings > Power```
  2. Set ```Put computer to sleep when inactive for``` as ```Never```
3. ```sudo apt-get update```
4. ```sudo apt-get upgrade```
5. Install Java using ```sudo apt-get install default-jre```
6.  Download all the files from https://github.com/dotnet/dotnet-ci/tree/master/management/nodes/reboot/ubuntu into a folder called ```reboot``` on your desktop
  1. ```sudo apt-get install subversion```
  2. ```svn export https://github.com/dotnet/dotnet-ci/trunk/management/nodes/reboot/ubuntu $HOME/Desktop/reboot```</br>
7. If the ```SHARE``` drive was not set to mount on boot automatically (you can check ```etc/fstab``` to see if the ```SHARE``` drive is missing)
  1. Open the ```/media/SHARE``` folder inside the GUI file viewer to mount the ```SHARE``` drive.
  2. Run ```sudo /bin/sh $HOME/Desktop/reboot/mount_cmd.sh```</br>
8. Install Python3 (```sudo apt-get install python3```)
9. Modify the last line of ```/etc/sudoers``` to allow the program to run under root provilege</br>
  ```sudo sh -c "echo '\n$USER ALL=(root) NOPASSWD: $HOME/Desktop/reboot/ubuntu_python.sh' >> /etc/sudoers```
10. Use ```chmod``` to set your rebot script to only be writable by the root user:</br>
  ```chmod 755 $HOME/Desktop/reboot/ubuntu_python.sh```</br>
11. Set up remote loggin through SSH: ```sudo apt-get install openssh-server```
12. Follow the steps for ```Jenkins through SSH```
13. In the ```reboot command``` when creating a ```Multi-OS Agent```, add a sudo to the path where the reboot script is stored</br>
	*Example*: ```sudo $HOME/Desktop/reboot/ubuntu_python.sh```

### OSX
1. Setup auto-login to the admin account
  1. ```System Preferences > Users & Groups```
  2. Click the lock at the bottom left
  3. Go to ```Login Options``` and set the ```Automatic login``` field
2. Turn off sleep mode
  1. ```System Preferences > Energy Saver```
  2. Set ```Turn display off after``` to ```Never```
3. Download the latest JDK for OSX from <http://www.oracle.com/technetwork/java/javase/downloads>
4. '''xcode-select --install'''
5. Download all the files from https://github.com/dotnet/dotnet-ci/tree/master/management/nodes/reboot/osx into a folder called ```reboot``` on your desktop
  1. ```svn export https://github.com/dotnet/dotnet-ci/trunk/management/nodes/reboot/ubuntu $HOME/Desktop/reboot```
  2. Accept the certificate
6. Install Python3 from <https://www.python.org/downloads/mac-osx/>
7. Modify the last line of ```/etc/sudoers``` to allow the program to run under root provilege</br>
  ```sudo sh -c "echo '\n$USER ALL=(root) NOPASSWD: $HOME/Desktop/reboot/osx_python.sh' >> /etc/sudoers```
8. Use ```chmod``` to set your rebot script to only be writable by the root user:</br>
  ```chmod 755 $HOME/Desktop/reboot/osx_python.sh```</br>
9. Set up remote loggin through SSH
  1. ```System Preferences > Sharing```
  2. Check ```Remote Login```
10. Follow the steps for ```Jenkins through SSH```
11. In the ```reboot command``` when creating a ```Multi-OS Agent```, add a sudo to the path where the reboot script is stored</br>
	*Example*: ```sudo $HOME/Desktop/reboot/osx_python.sh```

## Jenkins through SSH</br>
  (reference: <http://systemscitizen.com/2014/05/02/ubuntu-jenkins-slave-using-ssh/>)</br>
  1. ```sudo sh -c "echo '\nAllowUsers' $USER >> /etc/ssh/sshd_config"```
  2. ```mkdir $HOME/.ssh```
  3. ```chmod 700 $HOME/.ssh``` (*Note: This is required for ssh to run*)
  4. ```echo "" | ssh-keygen -t rsa -N ""```
  5. ```cd $HOME/.ssh```
  5. ```cat id_rsa.pub >> authorized_keys```
  6. Open your Jenkins web page
  7. Navigate to ```Credentials``` and click on ```global > Add Credentials```
  8. Select ```SSH Username with private key```
  9. Enter the result of ```echo $USER``` into the username field
  10. Select the option ```Enter directly``` and copy/paste *ALL* of the contents of ```$HOME/.ssh/id_rsa``` into the private key
  11. Enter description and press ```Save```
  12. To find the ```Host``` address, look at the ```inet``` field from running the command ```ifconfig | grep "inet " | grep -v 127.0.0.1 -m 1```
  13. When creating the Jenkins agent, select ```Launch slave agents on Unix machines via SSH``` with your new credentials select and the ```Host``` as the result of the previous step
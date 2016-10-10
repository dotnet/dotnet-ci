# Make the /mnt folder writeable
sudo chmod 777 /mnt/
# Restart docker since now the mnt drive is writeable
sudo service docker restart

# Remove the file that allows dotnet-bot paswordless, nointeractive sudoing (since we are done sudoing now).
sudo rm /etc/sudoers.d/zzz-dotnet-bot
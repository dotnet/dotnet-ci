# Make the /mnt folder writeable
chmod 777 /mnt/resource

# Make dotnet-bot sudo not prompt for passwords
bash -c 'echo "dotnet-bot ALL=NOPASSWD: ALL" | (EDITOR="tee -a" visudo)'

# Remove the file that allows dotnet-bot paswordless, nointeractive sudoing (since we are done sudoing now).
rm /etc/sudoers.d/zzz-dotnet-bot
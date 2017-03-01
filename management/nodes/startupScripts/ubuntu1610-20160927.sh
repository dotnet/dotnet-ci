# Test mkdir
mkdir /mnt/j
# Make the /mnt folder writeable
chmod 777 /mnt/j

# Remove the file that allows dotnet-bot paswordless, nointeractive sudoing (since we are done sudoing now).
rm /etc/sudoers.d/zzz-dotnet-bot
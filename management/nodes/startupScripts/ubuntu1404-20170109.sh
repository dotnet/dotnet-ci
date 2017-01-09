# Restart docker since now the mnt drive is writeable
service docker restart

# Remove the file that allows dotnet-bot paswordless, nointeractive sudoing (since we are done sudoing now).
rm /etc/sudoers.d/zzz-dotnet-bot

# Disable huge pages temporarily.
bash -c 'echo never > /sys/kernel/mm/transparent_hugepage/enabled'

# Test mkdir
mkdir /mnt/j
# Make the /mnt folder writeable
chmod 777 /mnt/j

if [ $? -ne 0 ]; then
    echo "Could not mkdir, VM not ready for use"
fi
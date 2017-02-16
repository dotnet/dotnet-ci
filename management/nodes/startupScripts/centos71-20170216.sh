# Remove the file that allows dotnet-bot paswordless, nointeractive sudoing (since we are done sudoing now).
rm /etc/sudoers.d/zzz-dotnet-bot

# Disable huge pages temporarily.
bash -c 'echo never > /sys/kernel/mm/transparent_hugepage/enabled'

# Occasionally the resource disk doesn't exist
if [ ! -d "/mnt/resource/" ]; then
    exit 1
fi

mkdir /mnt/resource/j
chmod -R 777 /mnt/resource/j
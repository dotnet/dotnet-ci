# Make dotnet-bot sudo not prompt for passwords
bash -c 'echo "dotnet-bot ALL=NOPASSWD: ALL" | (EDITOR="tee -a" visudo)'
# Make the /mnt folder writeable
chmod -R 777 /mnt/
# Restart docker since now the mnt drive is writeable
service docker restart
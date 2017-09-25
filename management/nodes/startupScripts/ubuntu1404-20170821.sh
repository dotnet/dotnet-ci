# Check that Azure's temporary storage was mounted correctly
if ! mountpoint -q "/mnt"; then
        echo "Azure's tmp storage not mounted"
	exit 1
fi

# Make the /mnt folder writeable
chmod 777 /mnt/

# Restart docker since now the mnt drive is writeable, then ensure that the daemon is running properly with a hello-world
service docker restart
docker run hello-world
if [ $? -ne 0 ]
then
    exit 1
fi

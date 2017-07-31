# Make the /mnt folder writeable
chmod 777 /mnt/

# Restart docker since now the mnt drive is writeable, then ensure that the daemon is running properly with a hello-world
service docker restart
docker run hello-world
if [ $? -ne 0 ]
then
    exit 1
fi
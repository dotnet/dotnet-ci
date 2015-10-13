# Reset the path because /local/bin doesn't get included
PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin
HOSTNAME=$(scutil --get LocalHostName)
JENKINS_MASTER=$1
JENKINS_CREDENTIALS=$2
# Grab slave jar file
wget $JENKINS_MASTER/jnlpJars/slave.jar -O ~/slave.jar
# launch
echo "java -jar ~/slave.jar -jnlpUrl $JENKINS_MASTER/computer/$HOSTNAME/slave-agent.jnlp -jnlpCredentials $JENKINS_CREDENTIALS"
java -jar ~/slave.jar -jnlpUrl $JENKINS_MASTER/computer/$HOSTNAME/slave-agent.jnlp -jnlpCredentials $JENKINS_CREDENTIALS

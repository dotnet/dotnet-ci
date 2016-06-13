import hudson.model.*
import hudson.slaves.OfflineCause;
  
for (node in Hudson.getInstance().getNodes())
{
  println("Checking " + node.name);
  def computer = node.getComputer()
  if (computer.isIdle())
  {
    println("  Cleaning " + node.name);
    if (node.getComputer().isOffline()) {
      println("  Offline, skipping");
      continue
    }
    
    println("  Attempting to take offline.");
   	computer.setTemporarilyOffline(true);
    if (!computer.isIdle()) {
      	println("  Not idle after offline, skipping!");
        computer.setTemporarilyOffline(false);
        continue;
    }
    println("  Cleaning workspace");
    def workspacePath = node.getRootPath();
    println("  About to wipe" + workspacePath.getRemote());
    if (workspacePath.exists())
    {
      try {
        deleteDirContents(computer, workspacePath, out)
        println("  Deleted from location " + workspacePath)
      } catch(e) {
        println("  Delete failed with: " + e)
      }
    }
    else
    {
      println("  Nothing to delete at " + workspacePath)
    }
    // Grab the temp dir
    def tempPath = computer.getEnvironment().get('TEMP', null)
    if (tempPath != null) {
      def tempFilePath = node.createPath(tempPath);
      println("  Cleaning temp path: " + tempFilePath.getRemote())
      if (tempFilePath.exists()) {
        deleteDirContents(computer, tempFilePath, out)
      }
    }
    else {
      // Try /tmp/
      def tempFilePath = node.createPath('/tmp/');
      println("  Cleaning temp path: " + tempFilePath.getRemote())
      if (tempFilePath.exists()) {
        deleteDirContents(computer, tempFilePath, out)
      }
    }
    // Bring back online
    println("  Done, bringing back online.");
    computer.setTemporarilyOffline(false);
  }
  else {
    println("  Not Idle");
  }
}

def static deleteDirContents(def computer, def directory, def output) {
  def command = ''
  if (computer.isUnix()) {
    command = "/bin/sh -c 'rm -rf *'"
  } else {
    command = "cmd.exe /C mkdir %USERPROFILE%\\emptydir & robocopy /MIR /R:2 /NFL /NDL /NJH /NJS /nc /ns /np %USERPROFILE%\\emptydir ${directory}"
  }
      
  try {
    output.println ("   Running ${command}")
    def launcher = directory.createLauncher(hudson.model.TaskListener.NULL);
    def starter = launcher.launch().pwd(directory).stdout(output).stderr(output).cmdAsSingleString(command)
    starter.join()
  }
  catch(e) {
    output.println("    Failed to deleteDirContents on " + directory)
  }
}
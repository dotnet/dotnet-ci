import hudson.model.*
import hudson.slaves.OfflineCause;
  
for (node in Hudson.getInstance().getNodes())
{
  if (node.getComputer().isIdle()) {
    println("Cleaning node.name");
    println("  Attempting to take " + node.name + " offline.");
   	node.getComputer().setTemporarilyOffline(true);
    println("  Cleaning workspace");
    def workspacePath = node.getRootPath();
    println("  About to wipe" + workspacePath.getRemote());
    // Bring back online
    println("  Done, bringing back online.");
    if (workspacePath.exists())
    {
      try {
        workspacePath.deleteRecursive()
        println("  Deleted from location " + workspacePath)
      } catch(e) {
        println("  Delete failed with: " + e)
      }
    }
    else
    {
      println("  Nothing to delete at " + workspacePath)
    }
    node.getComputer().setTemporarilyOffline(false);
  }
}
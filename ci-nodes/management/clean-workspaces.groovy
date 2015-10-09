import hudson.model.*
import hudson.slaves.OfflineCause;
  
for (node in Hudson.getInstance().getNodes())
{
  if (node.getComputer().isIdle()) {
    println("Cleaning " + node.name);
    if (node.getComputer().isOffline()) {
      println("  Offline, skipping");
      continue
    }
    
    println("  Attempting to take offline.");
   	node.getComputer().setTemporarilyOffline(true);
    if (!node.getComputer().isIdle()) {
      	println("  Not idle after offline, skipping!");
        node.getComputer().setTemporarilyOffline(false);
        continue;
    }
    println("  Cleaning workspace");
    def workspacePath = node.getRootPath();
    println("  About to wipe " + workspacePath.getRemote());
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
    // Bring back online
    println("  Done, bringing back online.");
    node.getComputer().setTemporarilyOffline(false);
  }
}
import hudson.model.*
import hudson.slaves.OfflineCause;
  
for (node in Hudson.getInstance().getNodes())
{
  println("Checking " + node.name);
  def computer = node.getComputer()
  if (computer.isIdle())
  {
    println("  Attempting to take offline. " + node.name);
    if (node.getComputer().isOffline()) {
      println("  Offline, skipping");
      continue
    }
    
   	computer.setTemporarilyOffline(true);
    if (!computer.isIdle()) {
      	println("  Not idle after offline, skipping!");
        computer.setTemporarilyOffline(false);
        continue;
    }
  }
  else {
    println("  Not Idle");
  }
}
import hudson.model.*
import hudson.slaves.OfflineCause;

def node = Hudson.getInstance().getNode($NODE_NAME))
println("Attempting to take this machine offline: " + node.name);
def computer = node.getComputer()

if (node.getComputer().isOffline()) {
    println("  Offline, skipping");
}
else {
    computer.setTemporarilyOffline(true);
}
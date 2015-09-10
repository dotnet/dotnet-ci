import hudson.model.*

println "Checking status of node: " + args[0];

def node = Hudson.getInstance().getNode(args[0]);

if (node == null) {
    println("Node not found!");
} else if (node.getComputer().isIdle()) {
    println("Idle");
} else {
    println("Busy");
}
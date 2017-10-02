import hudson.model.ComputerSet

for (int i = 0; i < ComputerSet.monitors.size(); i++) {
  if (ComputerSet.monitors.getAt(i) instanceof hudson.node_monitors.SwapSpaceMonitor) {
    ComputerSet.monitors.remove((ComputerSet.monitors.getAt(i)))
  }
}
// Removes offline nodes from AzureVMAgents that weren't taken offline for user reasons

import jenkins.model.Jenkins
import com.microsoft.azure.vmagent.AzureVMAgent
import com.microsoft.helix.helixagents.HelixAgent

def nodes = Jenkins.instance.getNodes()

nodes.each { node ->
  if (node instanceof AzureVMAgent || node instanceof HelixAgent) {
    if (node.getComputer() != null) {
      if (node.getComputer().isTemporarilyOffline()) {
        def cause = node.getComputer().getOfflineCause()
        if (!(cause instanceof hudson.slaves.OfflineCause.UserCause) && 
            (!(node instanceof HelixAgent) || !(cause instanceof hudson.slaves.OfflineCause.SimpleOfflineCause))) {
          println "Removing " + node.getComputer().getName() + " "
          Jenkins.instance.removeNode(node)
        }
      }
    }
  }
}

return true

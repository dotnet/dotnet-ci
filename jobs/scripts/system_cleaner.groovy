import hudson.model.*
import jenkins.model.*


deleteDisabledNonRootChildren(Jenkins.instance.items, '')

def deleteDisabledNonRootChildren(items, folder) {
    for (item in items) {
        if (item.class.canonicalName != 'com.cloudbees.hudson.plugins.folder.Folder') {
            if (item.class.canonicalName != 'org.jenkinsci.plugins.workflow.job.WorkflowJob') {
                // Workflow jobs don't derive from AbstractProject, so cannot be disabled
            }
            if (item.disabled) {
                println("About to delete " + folder + "/" + item.name)
                item.delete();
            }
        } else {
            deleteDisabledNonRootChildren(((com.cloudbees.hudson.plugins.folder.Folder) item).getItems(), folder + "/" + item.name)
        }
    }
}
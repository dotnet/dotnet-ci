import hudson.model.*
import jenkins.model.*


deleteDisabledNonRootChildren(Jenkins.instance.items)

def deleteDisabledNonRootChildren(items) {
    def removedAllItems = true
    for (item in items) {
        if (item.class.canonicalName != 'com.cloudbees.hudson.plugins.folder.Folder') {
            if (item.class.canonicalName == 'org.jenkinsci.plugins.workflow.job.WorkflowJob') {
                // Workflow jobs don't derive from AbstractProject, so cannot be disabled.  In this case, print the potential
                // orphaned item and continue
                println (item.fullName + " cannot be deleted or disabled (is workflow job)!")
                continue
            }
            if (item.disabled) {
                println("About to delete " + item.fullName)
                item.delete();
            } else {
                removedAllItems = false
            }
        } else {
            def removedAllItemsFromFolder = 
                deleteDisabledNonRootChildren(((com.cloudbees.hudson.plugins.folder.Folder) item).getItems())
            if (removedAllItemsFromFolder) {
                println("About to delete " + item.fullName)
                // Delete the folder too
                item.delete()
            }
            else {
                // We didn't remove everything from the folder, so make sure removeAllItems is updated
                removedAllItems = false
            }
        }
    }
    
    return removedAllItems
}
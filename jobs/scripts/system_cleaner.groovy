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
            // Delete everything that's disabled
            boolean doDelete = item.disabled
            // Also delete if it's not disabled, but is somewhere under GenPRTest and is NOT a generator
            doDelete |= (item.fullName.indexOf('GenPRTest') != -1 && item.name.indexOf('generator') == -1)
            // Also delete it if the full path starts with /GenPRTest and it's name is not 'dotnet_dotnet-ci_generator_prtest'
            doDelete |= item.fullName.indexOf('GenPRTest') == 0 && item.name != 'dotnet_dotnet-ci_generator_prtest'
            if (doDelete) {
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
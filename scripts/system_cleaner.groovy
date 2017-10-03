import hudson.model.*
import jenkins.model.*

// This denotes a value by which if we are about to delete more than X items,
// that don't have GenPRTest in their full name, then we should break and fail the job.
// This is to prevent cases where some component might go haywire and cause deletion of a lot of data.
def nonGenPRTestSafetyBreak = 500
def genPRTestSafetyBreak = 10000

(removedAllItemsFromFolder, nonGenPRTestCount, genPRTestCount) = deleteDisabled(Jenkins.instance.items, true)

println ("About to delete:")
println ("  " + genPRTestCount + " generation test items")
println ("  " + nonGenPRTestCount + " real items")
println ("  " + nonGenPRTestCount + genPRTestCount + " total")

// Safety break checks
assert nonGenPRTestCount < nonGenPRTestSafetyBreak
assert genPRTestCount < genPRTestSafetyBreak

(removedAllItemsFromFolder, nonGenPRTestCount, genPRTestCount) = deleteDisabled(Jenkins.instance.items, false)

return true

def deleteDisabled(items, calculateOnly) {
    def removedAllItems = true
    def nonGenPRTestCount = 0
    def genPRTestCount = 0
    for (item in items) {
        if (item.class.canonicalName != 'com.cloudbees.hudson.plugins.folder.Folder') {
            // ISSUE-These exclusions are related to those projects that aren't derived from
            // AbstractProject.  The WorkflowJob exclusion is fixed in newer versions of plugins.
            if (item.class.canonicalName == 'org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject') {
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
                if (item.fullName.indexOf('GenPRTest') != -1) {
                    genPRTestCount++
                } else {
                    nonGenPRTestCount++
                }
                if (!calculateOnly) {
                    println("About to delete " + item.fullName)
                    item.delete();
                }
                else {
                    println("Would delete " + item.fullName)
                }
            } else {
                removedAllItems = false
            }
        } else {
            def removedAllItemsFromFolder
            def nonGenDeleted
            def genDeleted
            (removedAllItemsFromFolder, nonGenDeleted, genDeleted) = 
                deleteDisabled(((com.cloudbees.hudson.plugins.folder.Folder) item).getItems(), calculateOnly)
            nonGenPRTestCount += nonGenDeleted
            genPRTestCount += genDeleted
            if (removedAllItemsFromFolder) {
                if (item.fullName.indexOf('GenPRTest') != -1) {
                    genPRTestCount++
                } else {
                    nonGenPRTestCount++
                }
                // Delete the folder too
                if (!calculateOnly) {
                    println("About to delete " + item.fullName)
                    item.delete()
                }
                else {
                    println("Would delete " + item.fullName)
                }
            }
            else {
                // We didn't remove everything from the folder, so make sure removeAllItems is updated
                removedAllItems = false
            }
        }
    }
    
    return [removedAllItems, nonGenPRTestCount, genPRTestCount]
}

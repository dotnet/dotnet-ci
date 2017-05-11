import hudson.model.*
import jenkins.model.*

def GeneratorJobName = build.buildVariableResolver.resolve("GeneratorJobName")
def GeneratorBuildNumber = build.buildVariableResolver.resolve("GeneratorBuildNumber")

println("Retrieving info about ${GeneratorJobName}:${GeneratorBuildNumber}")

def generatorJob = Jenkins.instance.getItemByFullName(GeneratorJobName)
// Get the specific build
AbstractBuild currentBuild = generatorJob.getBuild(GeneratorBuildNumber)
assert currentBuild != null
// Retrieve the succesful build before that
AbstractBuild previousBuild = currentBuild.getPreviousSuccessfulBuild()
assert previousBuild != null

// Get the sets of jobs generated.
Set<Item> currentJobs = currentBuild.getAction(javaposse.jobdsl.plugin.actions.GeneratedJobsBuildAction).items
Set<Item> previousJobs = previousBuild.getAction(javaposse.jobdsl.plugin.actions.GeneratedJobsBuildAction).items
assert currentJobs != null
assert previousJobs != null

println("Diffing ${generatorJob.fullName}:${currentBuild.number} against ${generatorJob.fullName}:${previousBuild.number}")

// We are trying to deal with the following issue:  When a line is removed from the repo list, or changed in such a way
// that the generator no longer exists, the old generated jobs aren't deleted or disabled.  They stay around.  This is inefficient
// and annoying.  What this script does is diff the previous and current builds and determine whether generator jobs have been
// deleted.  If they have, then we walk the folder structure that was generated and disable any jobs.  These would be cleaned up later.

// We are interested in cases where a generator (or generator_prtest) existed before, but not now

// Log some basic stuff

println("Generators, before:")
previousJobs.each { job ->
    // If the job's name is "generator"
    if (job.name == "generator") {
        println( "  " + job.fullName )
    }
}
        
println("Generators, after:")
currentJobs.each { job ->
    // If the job's name is "generator"
    if (job.name == "generator") {
        println( "  " + job.fullName )
    }
}

// Now walk and find deleted ones (in previous but not now)
// There is a special case.  If there are overlapping subfolders.  For instance, if I had
// 
// dotnet/coreclr/master/generator
// 
// which generated a few jobs under a perf folder
//
// dotnet/coreclr/master/perf/myfoojob
// 
// and removed that generator, but added another in a subfolder with a generator
//
// dotnet/coreclr/master/perf/generator
//
// We don't want to disable the second generator.  We do, however, want to disable all jobs under dotnet/coreclr/master
// since there may have been removed jobs under 'perf' that are no longer generated in the new groovy file.  The solution is simple
// though, as we walk and disable jobs under dotnet/coreclr/master, as we find other jobs named generator that are in the currentJobs list,
// add them to a list to queue after this script is done.

println("Removed generators:")

def foldersToDisable = []
previousJobs.each { previousJob ->
    // If the previous job was a generator.
    if (previousJob.name == "generator") {
        boolean found = false
        // Check the current jobs for a generator with the same full name
        currentJobs.each { currentJob ->
            if (currentJob.fullName == previousJob.fullName) {
                found = true
                return
            }
        }
        if (!found) {
            println("  " + previousJob.fullName)
            // Add the job's folder to the disable list
            def parentFullName = previousJob.getParent().fullName
            assert parentFullName != '' && parentFullName != '/'
            foldersToDisable += parentFullName
        }
    }
}

println ("Disabling jobs:")

// Now that we have our list, go through and disable, checking that each item named generator or generator_prtest isn't
// in the current list

def generatorsToRequeue = []
foldersToDisable.each { folderName ->
    println("Disabling items under folder ${folderName}")
    def folder = Jenkins.instance.getItemByFullName(folderName)
    assert folder.class.canonicalName == 'com.cloudbees.hudson.plugins.folder.Folder'
    generatorsToRequeue += disableItemsUnderFolderNotInList(((com.cloudbees.hudson.plugins.folder.Folder) folder).getItems(), currentJobs)
}

def disableItemsUnderFolderNotInList(items, currentItems) {
    def generatorsToRequeue = []
    items.each { item ->
        if (item.class.canonicalName != 'com.cloudbees.hudson.plugins.folder.Folder') {
            // Skip already disabled items
            if (!item.disabled) {
                boolean isGeneratorAndFoundInCurrent = false
                // If the item name is 'generator' or 'generator_prtest', check to see whether it exists in the current list.
                if (item.name == "generator" || item.name == "generator_prtest") {
                    currentItems.each { currentItem ->
                        if (currentItem.fullName == item.fullName) {
                            isGeneratorAndFoundInCurrent = true
                            return
                        }
                    }
                }
                if (!isGeneratorAndFoundInCurrent) {
                    // Disable
                    println("  " + item.fullName)
                    // Reenable when we're sure this works properly
                    item.disabled = true
                }
                else if (item.name == "generator") {
                    // Requeue
                    generatorsToRequeue += item
                }
            }
        } else {
            generatorsToRequeue += disableItemsUnderFolderNotInList(((com.cloudbees.hudson.plugins.folder.Folder) item).getItems(), currentItems)
        }
    }
    
    return generatorsToRequeue
}

// Now requeue the jobs.

println ("Requeuing generators")

generatorsToRequeue.each { generator ->
    println ("  Requeuing " + generator.fullName)
    // Schedule
    generator.scheduleBuild2(0)
}

println("Done")
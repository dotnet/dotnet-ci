import hudson.model.*
import jenkins.model.*
  
// Grab the parameters

def resolver = build.buildVariableResolver
String FolderName = resolver.resolve("FolderName").toString()
boolean DisableSubFolderItems = resolver.resolve("DisableSubFolderItems").toString().toBoolean()
boolean DryRunOnly = resolver.resolve("DryRunOnly").toBoolean()
assert FolderName != '' && FolderName != '/'
if (!FolderName.startsWith('/')) {
    FolderName = "/" + FolderName
}

print ('Disabling jobs in ' + FolderName)
if (DisableSubFolderItems) {
	print (' and any folders under that')
}
if (DryRunOnly) {
  	print ('  (dry run)')
}

// Disable a folder for deletion later.
disableFolder(FolderName, DryRunOnly, DisableSubFolderItems, Jenkins.instance.items)

def disableFolder(String folderName, boolean dryRunOnly, boolean disableSubfolders, items, String currentFolderName = '') {
  for (item in items) {
    if (item.class.canonicalName != 'com.cloudbees.hudson.plugins.folder.Folder') {
      if (folderName == currentFolderName || (disableSubfolders && currentFolderName.startsWith(folderName))) {
        if (!dryRunOnly) {
            println ("Disabling ${currentFolderName}/" + item.name);
            item.disabled=true
            item.save()
        }
        else {
            println ("Would Disable ${currentFolderName}/" + item.name);
        }
      }
    } else {
      if (folderName != currentFolderName || disableSubfolders) {
		disableFolder(folderName, dryRunOnly, disableSubfolders, ((com.cloudbees.hudson.plugins.folder.Folder) item).getItems(), currentFolderName + "/" + item.name)
      }
    }
  }
}
import hudson.model.*
import jenkins.model.*


deleteDisabledNonRootChildren(Jenkins.instance.items, '')

def deleteDisabledNonRootChildren(items, folder) {
  for (item in items) {
    if (item.class.canonicalName != 'com.cloudbees.hudson.plugins.folder.Folder') {
      if (item.disabled) {
        println("About to delete " + folder + "/" + item.name)
        item.delete();
      }
    } else {
      deleteDisabledNonRootChildren(((com.cloudbees.hudson.plugins.folder.Folder) item).getItems(), folder + "/" + item.name)
    }
  }
}
import hudson.model.*
import jenkins.model.*
import com.microsoft.azure.vmagent.AzureVMCloud
import com.microsoft.azure.vmagent.util.AzureUtil
import com.microsoft.azure.util.AzureCredentials
import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.domains.DomainRequirement
import hudson.security.ACL

// This script relies on the Azure VM Agents plugin 0.4.5 or later.
// Expected incoming parameters:
// CloudSubscriptionCredentialsId - Id of credentials that drive this cloud. Serves as a lookup for the subscription id 
//                                  so that the cloud name can be located.
// VmTemplateDeclarations - File, relative to root of dotnet-ci repo containing the list of vm templates
// TestOnly (boolean) - If true, then this attempts to do everything up to adding the actual images to the cloud.
//                      This allows for testing of image changes, printing what would be done.
def CloudSubscriptionCredentialsId = build.buildVariableResolver.resolve("CloudSubscriptionCredentialsId")
def VmTemplateDeclarations = build.buildVariableResolver.resolve("VmTemplateDeclarations")
def TestOnly = build.buildVariableResolver.resolve("TestOnly")

// Since this is running on the master, we need to be clever to get the path to the file.
// Potentially if this was changed to a pipeline, we could separate out the system groovy step
// into another section, read the file first and only execute a few things on the master.  But nbd.
def currentBuild = Thread.currentThread().executable
def fullPathToTemplates ="${currentBuild.workspace.toString()}/${VmTemplateDeclarations}"
println fullPathToTemplates 

// First let's do some basic processing and checks.  The general rule for this script is that if there are 
// any errors, we bail out before clearing the existing templates.  This means that even if the input list gets
// screwed up, we don't mess up the existing template config

// Get the cloud name and find the cloud itself
AzureVMCloud cloud = getCloud(CloudSubscriptionCredentialsId)

// Read the file incoming
def file = new File(fullPathToTemplates)
def templateDeclarationText = file.readLines()

templateDeclarationText.each { line ->
    println line
    // Skip comment lines
    boolean skip = (line ==~ / *#.*/);
    line.trim()
    skip |= (line == '')
    if (skip) {
        // Return from closure
        return;
    }
}

def AzureVMCloud getCloud(String credentialsId) {
    String cloudName = getCloudName(credentialsId)
    println "Looking up cloud with name ${cloudName}"
    def cloud = Jenkins.getInstance().getCloud(cloudName)

    assert cloud != null : "Could not find cloud specified by credentials id ${credentialsId}"
    return cloud
}

def String getCloudName(String credentialsId) {
    def subscriptionId = AzureCredentials.getServicePrincipal(credentialsId).getSubscriptionId()
    println "Cloud specified by credentials ${credentialsId} has subscription id ${subscriptionId}"
    String cloudName = AzureUtil.getCloudName(subscriptionId)

    assert cloudName != null && cloudName != "" : "Cloud name not valid"

    println "Cloud specified by credentials ${credentialsId} has cloud name is ${cloudName}"
    return cloudName
}
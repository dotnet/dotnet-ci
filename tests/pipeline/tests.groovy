import org.dotnet.ci.pipelines.Pipeline
import jobs.generation.Utilities

// This file is pretty barebones.  It basically just sets up a pipeline which then calls the tests.
// This is opposed to setting up all the tests here, which is a little more difficult to square between PR and non-PR scenarios,
// as well as for verification purposes.
def testingPipeline = Pipeline.createPipeline(this, 'tests/pipeline/pipeline-tests.groovy')

// Trigger this pipeline on pushes and PRs.
// TODO: VSTS PRs
if (VersionControlLocation != 'VSTS') {
    testingPipeline.triggerPipelineOnEveryPR("CI Tests (${ServerName})")
}
testingPipeline.triggerPipelineOnPush()

// Make the call to generate the help job
Utilities.createHelperJob(this, QualifiedRepoName, TargetBranchName,
    "Welcome to the ${QualifiedRepoName} Repository",  // This is prepended to the help message
    "Have a nice day!")  // This is appended to the help message.  You might put known issues here.
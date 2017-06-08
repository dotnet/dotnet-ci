import org.dotnet.ci.pipelines.Pipeline

// This file is pretty barebones.  It basically just sets up a pipeline which then calls the tests.
// This is opposed to setting up all the tests here, which is a little more difficult to square between PR and non-PR scenarios,
// as well as for verification purposes.
def testingPipeline = Pipeline.createPipeline(this, 'tests/pipeline/pipeline-tests.groovy')

// Trigger this pipeline on pushes and PRs.
// TODO: VSTS PRs
if (VersionControlLocation != 'VSTS') {
    testingPipeline.triggerPipelineOnEveryPR('CI Tests')
}
testingPipeline.triggerPipelineOnPush()
import org.dotnet.ci.pipelines.Pipeline

// Tests creation of a pipeline that triggers on pushes with non-string parameter types
def newPipeline = Pipeline.createPipeline(this, 'simple_pr.groovy')

try {
    newPipeline.triggerPipelineOnEveryPR('CI Test', ['Hello':false])
}
catch (AssertionError e) {
    assert e.getMessage().indexOf("VSTS PR pipelines are NYI") != -1 : "Expected 'VSTS PR pipelines are NYI'"
}
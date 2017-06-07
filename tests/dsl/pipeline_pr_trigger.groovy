import org.dotnet.ci.pipelines.Pipeline

// Tests creation of a pipeline that triggers on pushes with non-string parameter types
def newPipeline = Pipeline.createPipeline(this, 'simple_pr.groovy')

assert newPipeline.triggerPipelineOnEveryPR('CI Test', ['Hello':false]) == null : "Expected VSTS PR trigger was NYI"
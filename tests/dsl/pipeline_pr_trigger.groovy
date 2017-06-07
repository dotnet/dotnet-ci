import org.dotnet.ci.pipelines.Pipeline

// Tests creation of a pipeline that triggers on pushes with non-string parameter types
def newPipeline = Pipeline.createPipeline(this, 'simple_pr.groovy')

newPipeline.triggerPipelineOnEveryPR('CI Test', ['Hello':false])
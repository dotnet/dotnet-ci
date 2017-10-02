import org.dotnet.ci.pipelines.Pipeline

// Tests creation of a pipeline that triggers on pushes with non-string parameter types
def newPipeline = Pipeline.createPipeline(this, 'simple_periodic.groovy')

newPipeline.triggerPipelineManually(['Hello':false])
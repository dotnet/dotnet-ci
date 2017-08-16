import org.dotnet.ci.pipelines.Pipeline

// Tests creation of a pipeline that triggers on pushes
def newPipeline = Pipeline.createPipeline(this, 'simple_push.groovy')

// No parameters
newPipeline.triggerPipelineOnPush()

// Parameters
newPipeline.triggerPipelineOnPush(['Hello':'World'])
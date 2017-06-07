import org.dotnet.ci.pipelines.Pipeline
import jobs.generation.GenerationSettings

// Set for SDK testing
GenerationSettings.setSDKTest()

// Tests creation of a pipeline that triggers on pushes
def newPipeline = Pipeline.createPipeline(this, 'foopipeline.groovy')

// No parameters
newPipeline.triggerPipelineOnPush()

// Parameters
newPipeline.triggerPipelineOnPush(['Hello':'World'])
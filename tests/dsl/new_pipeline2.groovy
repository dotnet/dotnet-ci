import org.dotnet.ci.pipelines.Pipeline
import jobs.generation.GenerationSettings

// Set for SDK testing
GenerationSettings.setSDKTest()

// Tests creation of a pipeline that triggers on pushes with non-string parameter types
def newPipeline = Pipeline.createPipeline(this, 'foopipeline.groovy')

// Parameters
newPipeline.triggerPipelineOnPush(['Hello':false])
// Import the pipeline declaration classes.
import org.dotnet.ci.pipelines.Pipeline

// Declare a new pipeline.
def windowsPipeline = Pipeline.createPipeline(this, 'windows-pipeline.groovy')

// Over the array of 'Debug' and 'Release' configurations, generate a pipeline
// job that runs windows-pipeline on each push, passing in the parameter
// 'Configuration' with the value of 'configuration'
['Debug', 'Release'].each { configuration ->
    windowsPipeline.triggerPipelineOnPush(['Configuration':configuration])
}

// Over the array of 'Debug' and 'Release' configurations, generate a pipeline
// job that runs linux-pipeline.groovy on each PR, passing in the parameter
// 'Configuration' with the value of 'configuration'
def linuxPipeline = Pipeline.createPipeline(this, 'linux-pipeline.groovy')
// Pass Debug/Release configuration
['Debug', 'Release'].each { configuration ->
    linuxPipeline.triggerPipelineOnEveryPR(['Configuration':configuration])
}
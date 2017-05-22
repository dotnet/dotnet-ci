/**
  * Loads a pipeline from a file in the workspace.  Effectively just 'load' but under a workspace.
  * @return New pipeline, which can be invoked.
  */
def call(String pipelineFile) {
    simpleNode('Windows_NT','latest') {
        checkout scm
        return load(pipelineFile)
    }
}
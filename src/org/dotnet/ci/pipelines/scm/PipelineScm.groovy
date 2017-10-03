package org.dotnet.ci.pipelines.scm;

interface PipelineScm {
    String getBranch()
    void emitScmForPR(def job, String pipelineFile)
    void emitScmForNonPR(def job, String pipelineFile)
    String getScmType()
}
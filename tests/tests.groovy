// Import the utility functionality.

import jobs.generation.JobReport;
import jobs.generation.Utilities;
import org.dotnet.ci.pipelines.Pipeline

// Expected VSTS or GitHub SCM
assert binding.variables.get("VersionControlLocation") != null && 
       (binding.variables.get("VersionControlLocation") == 'VSTS' || 
       binding.variables.get("VersionControlLocation") != 'GitHub') : "Expected what version control this server targets (VSTS or GitHub)"
       
// The input project name (e.g. dotnet/corefx)
def project = QualifiedRepoName
// The input branch name (e.g. master)
def branch = TargetBranchName

// This file is pretty barebones.  It basically just sets up a pipeline which then calls the tests.
// This is opposed to setting up all the tests here, which is a little more difficult to square between PR and non-PR scenarios,
// as well as for verification purposes.
def testingPipeline = Pipeline.createPipeline(this, VersionControlLocation, project, branch, 'tests/root-tests-pipeline.groovy')

// Trigger this pipeline on pushes and PRs
testingPipeline.triggerPipelineOnEveryPR('CI Tests')
testingPipeline.triggerPipelineOnPush()
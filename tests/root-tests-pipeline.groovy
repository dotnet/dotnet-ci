// Entering

echo "Running tests for CI SDK"
echo "Incoming parameters: "
echo "    OrgOrProjectName = ${OrgOrProjectName}"
echo "    RepoName = ${RepoName}"
echo "    GitBranchOrCommit = ${GitBranchOrCommit}"

// DSL tests
node {
    stage ('Source Control Checkout') {
        checkout scm
    }

    stage ('DSL Generation Tests') {
        // Run DSL
        jobDsl targets: 'tests/dsl/new_pipeline.groovy',
           removedJobAction: 'DELETE',
           removedViewAction: 'DELETE',
           lookupStrategy: 'SEED_JOB'
           additionalClasspath: 'src'
    }
}
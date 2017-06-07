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

        // This should pass, but doesn't generate any jobs
        jobDsl targets: 'tests/dsl/new_pipeline1.groovy',
           removedJobAction: 'DELETE',
           removedViewAction: 'DELETE',
           lookupStrategy: 'SEED_JOB',
           additionalClasspath: 'src'

        jobDsl targets: 'tests/dsl/new_pipeline2.groovy',
            removedJobAction: 'DELETE',
            removedViewAction: 'DELETE',
            lookupStrategy: 'SEED_JOB',
            additionalClasspath: 'src'
    }
}
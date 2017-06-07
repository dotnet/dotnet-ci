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
}
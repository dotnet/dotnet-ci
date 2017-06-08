// Entering

echo "Running tests for CI SDK"
echo "Incoming parameters: "
echo "    OrgOrProjectName = ${OrgOrProjectName}"
echo "    RepoName = ${RepoName}"
echo "    GitBranchOrCommit = ${GitBranchOrCommit}"

// Remove the */ from GitBranchOrCommit, the import the library
String libraryImportBranch = GitBranchOrCommit
if (GitBranchOrCommit.indexOf('*/') == 0) {
    libraryImportBranch = GitBranchOrCommit.substring(2)
}

@Library("dotnet-ci@${libraryImportBranch}") _

// Basic tests
// Currently in one file, but could be in multiple

// Simple source control checkout (should always pass, built in functionality)
node {
    stage ('Source Control Checkout') {
        checkout scm
    }
}

// getBranch
node {
    stage ('getBranch - Test 1') {
        checkout scm

        String branch = getBranch()
        assert branch == libraryImportBranch : "Expected getBranch would return ${libraryImportBranch}"
    }
}

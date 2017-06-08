// Entering

@Library("dotnet-ci") _

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

stage ('Check out target library') {
    library "dotnet-ci@${libraryImportBranch}"
}

stage ('Run Tests') {
    // Overall test timeout at 2 hours
    timeout (120) {
        // Basic tests
        // Currently in one file, but could be in multiple.  Run in parallel by default

        parallel (
            // This is an array in the form of:
            // "test name" : { // test code }

            "Raw Node Test" : {
                // Simple source control checkout (should always pass, built in functionality)
                node {
                    checkout scm
                }
            },

            // Test that simple nodes work, of various types
            "simpleNode - Windows_NT - latest" : {
                timeout (60) {
                    simpleNode('Windows_NT', 'latest') { }
                }
            },

            "simpleNode - Ubuntu14.04 - latest" : {
                timeout (60) {
                    simpleNode('Ubuntu14.04', 'latest') { }
                }
            },

            "getBranch" : {
                // getBranch
                node {
                    simpleNode('Windows_NT', 'latest') {
                        checkout scm

                        echo "Checking that getBranch returns ${libraryImportBranch}"
                        String branch = getBranch()
                        assert branch == libraryImportBranch : "Expected getBranch would return ${libraryImportBranch}"
                    }
                }
            },

            // getCommit, varies on unix and windows, so test both
            "getCommit - Windows systems" : {
                simpleNode('Windows_NT', 'latest') {
                    checkout scm

                    echo "Checking that getCommit returns valid commit on NT based system"
                    String commit = getCommit()
                    echo "Got ${commit}"
                    // Check that it's probably a valid hash
                    assert commit.length() == 40 : "Commit doesn't look like a valid hash'"
                }
            },

            "getCommit - Unix systems" : {
                simpleNode('Ubuntu14.04', 'latest') {
                    checkout scm

                    echo "Checking that getCommit returns valid commit on NT based system"
                    String commit = getCommit()
                    echo "Got ${commit}"
                    // Check that it's probably a valid hash
                    assert commit.length() == 40 : "Commit doesn't look like a valid hash'"
                }
            },
        )
    }
}
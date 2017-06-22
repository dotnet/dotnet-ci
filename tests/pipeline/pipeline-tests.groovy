// To figure out what we import, we need to know whether this is a PR or not.
// If a PR, and GitHub, we grab the source branch, which must live in the same repo.
// Note that we can't use the library isPR for this.
boolean isPRTest = false
String repository = env["ghprbGhRepository"]
String libraryImportBranch
if (repository != null && repository != "") {
    echo "This is a GitHub PR test"
    // Check that the PR source branch came from the correct location (otherwise the tests are expected to fail)
    if (repository != 'dotnet/dotnet-ci') {
         error "PRs tests of functionality that changes the CI pipeline SDK are only valid for branches pushed to dotnet/dotnet-ci.  If you need that testing please push your branch to dotnet-ci.  Otherwise ignore this failure"
    }

    libraryImportBranch = env["ghprbSourceBranch"]
    assert libraryImportBranch != null && libraryImportBranch != '' : "Library branch (ghprbSourceBranch) was unexpectedly empty"
}
else {
    echo "This is a VSTS PR test or commit test"
    // TODO: VSTS PR support
    libraryImportBranch = GitBranchOrCommit
    if (libraryImportBranch.indexOf('*/') == 0) {
        libraryImportBranch = libraryImportBranch.substring(2)
    }
}

def libraryName = "dotnet-ci@${libraryImportBranch}"

stage ('Check out target library') {
    echo "Checking out ${libraryName}"
    library libraryName
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

            "Credentials Binding Not Allowed" : {
                // Certain credentials should not be bindable in pipelines
                node {
                    try {
                        withCredentials([usernameColonPassword(credentialsId: 'dotnet-bot-pr-builder-token', variable: 'foobar')]) {
                            error "Bound credentials, shouldn't have worked"
                        }
                    }
                    catch (e) {
                        echo "Got credentials binding failure: ${e.toString()}"
                    }
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
                simpleNode('Windows_NT', 'latest') {
                    checkout scm

                    echo "Checking that getBranch returns a valid value"
                    String branch = getBranch()
                    assert branch != null && branch != '': "Expected getBranch would return non-null"
                }
            },

            "isNullOrEmpty" : {
                assert isNullOrEmpty(null)
                assert isNullOrEmpty('')
                assert !isNullOrEmpty("foo")
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

            "logFolder creation for simpleNode - Unix systems" : {
                simpleNode('Ubuntu14.04', 'latest') {
                    assert getLogFolder() == 'netci-archived-logs' : "Unexpected log folder name"
                    def output = sh script: 'if [ -d "netci-archived-logs" ]; then echo netci-archived-logs exists; fi', returnStdout: true
                    assert output.indexOf('netci-archived-logs exists') != -1 : "Log folder didn't exist"
                }
            },

            "logFolder creation for simpleNode - Windows systems" : {
                simpleNode('Windows_NT', 'latest') {
                    assert getLogFolder() == 'netci-archived-logs' : "Unexpected log folder name"
                    def output = bat script: 'if exist netci-archived-logs echo netci-archived-logs exists', returnStdout: true
                    assert output.indexOf('netci-archived-logs exists') != -1 : "Log folder didn't exist"
                }
            },

            // Test GitHub PR functionality by mocking up the environment variables that
            // isPR and getUser will check for GitHub PRs
            "getUser - GitHub PR" : {
                withEnv(['ghprbPullAuthorLogin=baz', 'ghprbGhRepository=foo/bar']) {
                    assert getUser() == 'baz' : "Expected getUser would return baz"
                }
            },

            // Testing non-PR getUser is tough as it returns different values based on the cause of the run.
            // You can't mock causes in here without opening up untrusted APIs either.
            "getUser - non mocked behavior" : {
                assert getUser() != null : "Expected getUser would return valid value."
            },

            // Utilities tests

            "Utilities - calculateVSTSGitURL - devdiv collection" : {
                // With collection == devdiv, we add "DefaultColleciton" like in most servers
                String url = library(libraryName).jobs.generation.Utilities.calculateVSTSGitURL('devdiv', 'foo/bar')
                assert url == 'https://devdiv.visualstudio.com/DefaultCollection/foo/_git/bar' : "Incorrect url for devdiv collection git URL"
            },

            "Utilities - calculateVSTSGitURL - other collection" : {
                // With collection == devdiv, we add "DefaultColleciton" like in most servers
                String url = library(libraryName).jobs.generation.Utilities.calculateVSTSGitURL('other', 'foo/bar')
                assert url == 'https://other.visualstudio.com/foo/_git/bar' : "Incorrect url for non-devdiv collection git URL"
            },

            "Utilities - calculateGitHubUrl" : {
                // With collection == devdiv, we add "DefaultColleciton" like in most servers
                String url = library(libraryName).jobs.generation.Utilities.calculateGitHubURL('foo/bar')
                assert url == 'https://github.com/foo/bar' : "Incorrect url for github URL"
            },
        )
    }
}
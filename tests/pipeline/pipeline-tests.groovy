// To figure out what we import, we need to know whether this is a PR or not.
// If a PR, and GitHub, we grab the source branch, which must live in the same repo.
// Note that we can't use the library isPR for this.
boolean isPRTest = false
String repository = env.ghprbAuthorRepoGitUrl
String libraryImportBranch
if (ghprbAuthorRepoGitUrl != null && ghprbAuthorRepoGitUrl != "") {
    echo "This is a GitHub PR test"
    // Check that the PR source branch came from the correct location (otherwise the tests are expected to fail)
    if (ghprbAuthorRepoGitUrl.indexOf('dotnet/dotnet-ci') == -1) {
         error "PRs tests of functionality that changes the CI pipeline SDK are only valid for branches pushed to dotnet/dotnet-ci.  If you need that testing please push your branch to dotnet-ci.  Otherwise ignore this failure"
    }

    libraryImportBranch = ghprbSourceBranch
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
                    checkoutRepo()
                }
            },

            // Test that simple nodes work, of various types
            "simpleNode - Windows_NT - latest" : {
                timeout (60) {
                    simpleNode('Windows_NT', 'latest') {
                        checkoutRepo()
                    }
                }
            },

            "simpleNode - Windows_NT - latest dev15.5" : {
                timeout (60) {
                    simpleNode('Windows_NT', 'latest-dev15-5') {
                        checkoutRepo()
                    }
                }
            },
            
            "simpleNode - Windows_NT - RS3 client ES-ES" : {
                timeout (60) {
                    simpleNode('Windows.10.Amd64.ClientRS3.ES.Open') {
                        checkoutRepo()
                    }
                }
            },

            "simpleNode - Windows.10.Amd64.ClientRS3.DevEx.Open" : {
                timeout (60) {
                    simpleNode('Windows.10.Amd64.ClientRS3.DevEx.Open') {
                        checkoutRepo()
                    }
                }
            },

            // Test that simple nodes work, of various types
            "simpleNode - custom timeout" : {
                timeout (60) {
                    simpleNode('Windows_NT', 'latest', 30) {
                        checkoutRepo()
                    }
                }
            },

            // Test that simple nodes work, of various types
            "simpleNode - custom timeout2" : {
                timeout (60) {
                    simpleNode('osx-10.12 || OSX.1012.Amd64.Open', 30) {
                        checkoutRepo()
                    }
                }
            },

            "simpleNode - Ubuntu14.04 - latest" : {
                timeout (60) {
                    simpleNode('Ubuntu14.04', 'latest') {
                        checkoutRepo()
                    }
                }
            },

            "simpleNode - Explicit expression" : {
                timeout (60) {
                    simpleNode('osx-10.12 || OSX.1012.Amd64.Open') {
                        checkoutRepo()
                    }
                }
            },

            "simpleDockerNode1" : {
                timeout (60) {
                    simpleDockerNode('microsoft/dotnet-buildtools-prereqs:rhel7_prereqs_2') {
                        checkoutRepo()
                    }
                }
            },

            "simpleDockerNode1 - custom timeout" : {
                timeout (60) {
                    simpleDockerNode('microsoft/dotnet-buildtools-prereqs:rhel7_prereqs_2', 15) {
                        checkoutRepo()
                    }
                }
            },

            "getBranch" : {
                // getBranch
                simpleNode('Windows_NT', 'latest') {
                    checkoutRepo()

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
            // Test temporarily disabled because of issue with withEnv
            /*"getUser - GitHub PR" : {
                withEnv(['ghprbPullAuthorLogin=baz', 'ghprbGhRepository=foo/bar']) {
                    assert getUser() == 'baz' : "Expected getUser would return baz"
                }
            },*/

            // Testing non-PR getUser is tough as it returns different values based on the cause of the run.
            // You can't mock causes in here without opening up untrusted APIs either.
            "getUser - non mocked behavior" : {
                assert getUser() != null : "Expected getUser would return valid value."
            },

            // Test temporarily disabled because of issue with withEnv
            /*"getUserEmail - GitHub PR" : {
                // Test temporarily disabled because of issue with withEnv
                withEnv(['ghprbPullAuthorEmail=blah@blah.com', 'ghprbGhRepository=foo/bar']) {
                    def userEmail = getUserEmail()
                    assert userEmail == 'blah@blah.com' : "Expected getUserEmail would return blah@blah.com, actually got ${userEmail}"
                }
            },

            // Test temporarily disabled because of issue with withEnv
            "getUserEmail - GitHub PR, no email": {
                withEnv(['ghprbPullAuthorLogin=baz', 'ghprbGhRepository=foo/bar', 'ghprbPullAuthorEmail=']) {
                    def userEmail = getUserEmail()
                    assert userEmail == 'baz@github.login' : "Expected getUserEmail would return baz@github.login, actually got ${userEmail}"
                }
            },*/

            // Testing non-PR getUserEmail is tough as it returns different values based on the cause of the run.
            // You can't mock causes in here without opening up untrusted APIs either.
            "getUserEmail - non mocked behavior" : {
                assert getUserEmail() != null : "Expected getUserEmail would return valid value."
            },

            "vars - waitforHelixRuns - passed work item" : {
                simpleNode('Windows_NT', 'latest') {
                    dir('workItem') {
                        writeFile file: 'run.cmd', text: """
                        echo Passing
                        exit /b 0
                        """
                    }
                    zip zipFile: 'workItem.zip', dir: 'workItem'

                    def helixSource = getHelixSource()
                    // Ask the CI SDK for a Build that makes sense.  We currently use the hash for the build
                    def helixBuild = getCommit()
                    // Get the user that should be associated with the submission
                    def helixCreator = getUser()

                    dir("corefx") {
                        git 'https://github.com/dotnet/corefx'
                        bat 'init-tools.cmd'

                        dir('test') {
                            def fileContent = """
<Project ToolsVersion="14.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
    <Import Project="../Tools/CloudTest.Helix.targets"/>
    <ItemGroup>
        <HelixWorkItem Include="../../workItem.zip">
            <Command>run.cmd</Command>
            <PayloadFile>%(Identity)</PayloadFile>
            <WorkItemId>The Work Item</WorkItemId>
            <TimeoutInSeconds>10</TimeoutInSeconds>
        </HelixWorkItem>
    </ItemGroup>
    <PropertyGroup>
        <CloudDropConnectionString>DefaultEndpointsProtocol=https;AccountName=\$(CloudDropAccountName);AccountKey=\$(CloudDropAccessToken);EndpointSuffix=core.windows.net</CloudDropConnectionString>
        <CloudResultsConnectionString>DefaultEndpointsProtocol=https;AccountName=\$(CloudResultsAccountName);AccountKey=\$(CloudResultsAccessToken);EndpointSuffix=core.windows.net</CloudResultsConnectionString>
        <HelixApiEndpoint>https://helix.dot.net/api/2017-04-14/jobs</HelixApiEndpoint>
        <HelixJobType>test/functional/dotnet-ci</HelixJobType>
        <HelixSource>${helixSource}</HelixSource>
        <BuildMoniker>${helixBuild}</BuildMoniker>
        <HelixCreator>${helixCreator}</HelixCreator>
        <TargetQueues>Windows.10.Amd64.Open</TargetQueues>
        <HelixLogFolder>\$(MSBuildThisFileDirectory)</HelixLogFolder>
        <HelixCorrelationInfoFileName>job-info.json</HelixCorrelationInfoFileName>
        <HelixJobProperties>{ "architecture":"x86", "configuration":"Release", "operatingSystem": "pizza" }</HelixJobProperties>
        <ArchivesRoot>\$(MSBuildThisFileDirectory)</ArchivesRoot>
    </PropertyGroup>
    <Target Name="Build" DependsOnTargets="HelixCloudBuild"/>
</Project>
"""
                            println fileContent
                            writeFile file: 'submit-job.proj', text: fileContent

                            withCredentials([string(credentialsId: 'CloudDropAccessToken', variable: 'CloudDropAccessToken'),
                                 string(credentialsId: 'OutputCloudResultsAccessToken', variable: 'OutputCloudResultsAccessToken')]) {
                                 bat '..\\Tools\\dotnetcli\\dotnet.exe msbuild submit-job.proj /p:CloudDropAccountName=dotnetbuilddrops /p:CloudDropAccessToken=%CloudDropAccessToken% /p:CloudResultsAccountName=dotnetjobresults /p:CloudResultsAccessToken=%OutputCloudResultsAccessToken%'
                            }
                        }

                    }

                    def submittedJson = readJSON file: 'corefx/test/job-info.json'

                    waitForHelixRuns(submittedJson, "The Tests")
                }
            },
            
            "vars - waitforHelixRuns - failed work item" : {
                simpleNode('Windows_NT', 'latest') {
                    dir('workItem') {
                        writeFile file: 'run.cmd', text: """
                        echo Failing
                        exit /b -1
                        """
                    }
                    zip zipFile: 'workItem.zip', dir: 'workItem'

                    def helixSource = getHelixSource()
                    // Ask the CI SDK for a Build that makes sense.  We currently use the hash for the build
                    def helixBuild = getCommit()
                    // Get the user that should be associated with the submission
                    def helixCreator = getUser()

                    dir("corefx") {
                        git 'https://github.com/dotnet/corefx'
                        bat 'init-tools.cmd'

                        dir('test') {
                            def fileContent = """
<Project ToolsVersion="14.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
    <Import Project="../Tools/CloudTest.Helix.targets"/>
    <ItemGroup>
        <HelixWorkItem Include="../../workItem.zip">
            <Command>run.cmd</Command>
            <PayloadFile>%(Identity)</PayloadFile>
            <WorkItemId>The Work Item</WorkItemId>
            <TimeoutInSeconds>10</TimeoutInSeconds>
        </HelixWorkItem>
    </ItemGroup>
    <PropertyGroup>
        <CloudDropConnectionString>DefaultEndpointsProtocol=https;AccountName=\$(CloudDropAccountName);AccountKey=\$(CloudDropAccessToken);EndpointSuffix=core.windows.net</CloudDropConnectionString>
        <CloudResultsConnectionString>DefaultEndpointsProtocol=https;AccountName=\$(CloudResultsAccountName);AccountKey=\$(CloudResultsAccessToken);EndpointSuffix=core.windows.net</CloudResultsConnectionString>
        <HelixApiEndpoint>https://helix.dot.net/api/2017-04-14/jobs</HelixApiEndpoint>
        <HelixJobType>test/functional/dotnet-ci</HelixJobType>
        <HelixSource>${helixSource}</HelixSource>
        <BuildMoniker>${helixBuild}</BuildMoniker>
        <HelixCreator>${helixCreator}</HelixCreator>
        <TargetQueues>Windows.10.Amd64.Open</TargetQueues>
        <HelixLogFolder>\$(MSBuildThisFileDirectory)</HelixLogFolder>
        <HelixCorrelationInfoFileName>job-info.json</HelixCorrelationInfoFileName>
        <HelixJobProperties>{ "architecture":"x64", "configuration":"Debug", "operatingSystem": "pizza" }</HelixJobProperties>
        <ArchivesRoot>\$(MSBuildThisFileDirectory)</ArchivesRoot>
    </PropertyGroup>
    <Target Name="Build" DependsOnTargets="HelixCloudBuild"/>
</Project>
"""
                            println fileContent
                            writeFile file: 'submit-job.proj', text: fileContent

                            withCredentials([string(credentialsId: 'CloudDropAccessToken', variable: 'CloudDropAccessToken'),
                                 string(credentialsId: 'OutputCloudResultsAccessToken', variable: 'OutputCloudResultsAccessToken')]) {
                                bat '..\\Tools\\dotnetcli\\dotnet.exe msbuild submit-job.proj /p:CloudDropAccountName=dotnetbuilddrops /p:CloudDropAccessToken=%CloudDropAccessToken% /p:CloudResultsAccountName=dotnetjobresults /p:CloudResultsAccessToken=%OutputCloudResultsAccessToken%'
                            }
                        }

                    }


                    def submittedJson = readJSON file: 'corefx/test/job-info.json'

                    try {
                        waitForHelixRuns(submittedJson, "The Tests")
                        assert false : "Expected failure from waitForHelixRuns didn't occur"
                    }
                    catch (e) {
                        echo "Got expected failure from waitForHelixRuns"
                    }
                }
            }
        )
    }
}

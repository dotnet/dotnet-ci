// This project will generate new job generators based on changes to the repolist.txt, which will
// in turn cause those generators to run over the job generation in the local repos.

import jobs.generation.Utilities

// About this generator:
//
//  This generator is designed to run every time there is a change to
//    * The dotnet-ci repo's generation dir
//
//  A rerun of this could mean any number of things:
//    * Updated repolist.txt
//    * Updated utilities
//
//  Rerunning this generator will not automatically rerun all of the generators
//  below it (by design).  Users making changes to the overall generation structure
//  should recognize when there is a functional change being made and rerun all of the
//  generated jobs.

// Tests for incoming variables

assert binding.variables.get("ServerName") != null : "Expected string parameter with name ServerName corresponding to name of server"
assert binding.variables.get("RepoListLocation") != null : "Expected path to repo list"
assert binding.variables.get("SDKImplementationBranch") != null : "Expected the SDK implementation branch (default utilities branch)"
assert binding.variables.get("VersionControlLocation") != null && 
       (binding.variables.get("VersionControlLocation") == 'VSTS' || 
       binding.variables.get("VersionControlLocation") == 'GitHub') : "Expected what version control this server targets (VSTS or GitHub)"
boolean isVSTS = binding.variables.get("VersionControlLocation") == 'VSTS'

class Repo {
    String project
    String[] folders
    String branch
    String server
    String definitionScript
    // The location of the Utilities repo
    String utilitiesRepo
    // The branch for the utilities repo
    String utilitiesRepoBranch
    // The vsts collection (if applicable)
    String collection
    // The vsts collection credentials (if applicable)
    String credentials
    // True if this is an SDK test.
    boolean isDSLTest
    // True if the paths for job generation should be filtered, false if it should
    // be triggered on every change. https://github.com/dotnet/core-eng/issues/1531
    boolean useFilteredGenerationTriggers

    def Repo(String project,
             String[] folders,
             String branch,
             String server,
             String definitionScript,
             String utilitiesRepo,
             String utilitiesRepoBranch,
             String collection,
             String credentials,
             boolean isDSLTest,
             boolean useFilteredGenerationTriggers) {
        this.project = project
        this.folders = folders
        this.branch = branch
        this.server = server
        this.definitionScript = definitionScript
        this.utilitiesRepo = utilitiesRepo
        this.utilitiesRepoBranch = utilitiesRepoBranch
        this.collection = collection
        this.credentials = credentials
        this.isDSLTest = isDSLTest
        this.useFilteredGenerationTriggers = useFilteredGenerationTriggers
    }

    // Parse the input string and return a Repo object
    def static parseInputString(String input, String serverName, String defaultUtilitiesBranch, boolean isVSTS, def out) {
        // First element is the repo name.  Should be in <org>/<repo> format
        def projectInfo = input.tokenize()

        assert projectInfo.size() >= 1

        // First element is the repo name
        String project = projectInfo[0]
        String[] folders = null
        String[] subFolders = null
        String branch = null
        // Server name.
        String server = null
        // Project folder name
        String projectFolder = Utilities.getFolderName(project)
        // File name/path is usually netci.groovy, but can be set arbitrarily
        String definitionScript = 'netci.groovy'
        // Repo for Utilities that are used by the job
        String utilitiesRepo = isVSTS ? 'Tools/DotNet-CI-Trusted' : 'dotnet/dotnet-ci'
        // Branch that the utilities should be read from
        String utilitiesRepoBranch = defaultUtilitiesBranch
        // VSTS only: Project collection.
        String collection = null
        // credentials id used to access the repo. Since credentials aren't the same across project collections in VSTS,
        // this is required for VSTS projects.
        String credentials = null
        // Is this a test of the CI SDK DSL functionality?
        boolean isDSLTest = false
        // True if the paths for job generation should be filtered, false if it should
        // be triggered on every change. https://github.com/dotnet/core-eng/issues/1531
        boolean useFilteredGenerationTriggers = false

        // Check whether it contains a single forward slash
        assert project.indexOf('/') != -1 && project.indexOf('/') == project.lastIndexOf('/')

        // Now walk the rest of the elements and set the rest of the properties
        def i = 1
        while (i < (projectInfo.size())) {
            def element = projectInfo[i]

            if (element.startsWith('subFolder=')) {
                // Parse out the folder names
                subFolders = element.substring('subFolder='.length()).tokenize('/')

                // If the folder name was root, just zero it out.  If they chose root, there should
                // only be one element
                assert subFolders.size() >= 1
            }
            else if(element.startsWith('projectFolder=')) {
                // Sets the project folder name
                projectFolder = element.substring('projectFolder='.length())
            }
            else if(element.startsWith('branch=')) {
                branch = element.substring('branch='.length())
            }
            else if(element.startsWith('server=')) {
                server = element.substring('server='.length())
            }
            else if(element.startsWith('definitionScript=')) {
                definitionScript = element.substring('definitionScript='.length())
            }
            else if(element.startsWith('utilitiesRepo=')) {
                // Parse out the folder names
                utilitiesRepo = element.substring('utilitiesRepo='.length())
            }
            else if(element.startsWith('utilitiesRepoBranch=')) {
                // Parse out the folder names
                utilitiesRepoBranch = element.substring('utilitiesRepoBranch='.length())
            }
            // VSTS specific
            else if(element.startsWith('collection=')) {
                collection = element.substring('collection='.length())
            }
            // VSTS specific
            else if(element.startsWith('credentials=')) {
                credentials = element.substring('credentials='.length())
            }
            else if(element.startsWith('isDSLTest=true')) {
                isDSLTest = true
            }
            else if(element.startsWith('useFilteredGenerationTriggers=')) {
                useFilteredGenerationTriggers = element.substring('useFilteredGenerationTriggers='.length()).toBoolean()
            }
            else {
                out.println("Unknown element " + element);
                assert false
            }
            i++
        }

        if (branch == null || branch == '') {
            out.println("Line '${input}' invalid")
            out.println("branch must be specified")
            assert false
        }
        if (server == null || server == '') {
            out.println("Line '${input}' invalid")
            out.println("server must be specified")
            assert false
        }

        // Consistency check for VSTS vs. GitHub
        // If this is a VSTS server and the project collection wasn't specified for a project targeting this server, error.
        // Alternatively, error if the collection was specified 
        if (server == serverName) {
            if (isVSTS && (collection == null || collection == '')) {
                out.println("Line '${input}' invalid")
                out.println("VSTS collection must be specified for ${project} (use collection=)")
                assert false
            }
            else if (!isVSTS && collection != null) {
                out.println("Line '${input}' invalid")
                out.println("VSTS collection shouldn't be specified for ${project} (collection=${collection})")
                assert false
            }
            // Check credentials
            if (isVSTS && (credentials == null || credentials == '')) {
                out.println("Line '${input}' invalid")
                out.println("VSTS repo credentials id must be specified for ${project} (use credentials=)")
                assert false
            }
            else if (!isVSTS && credentials != null) {
                out.println("Line '${input}' invalid")
                out.println("VSTS repo credentials id shouldn't be specified for ${project} (credentials=${credentials})")
                assert false
            }
        }

        folders = [projectFolder]

        // If they asked for subfolders, add them
        if (subFolders != null) {
            folders += subFolders
        }

        // Add the branch after
        folders += Utilities.getFolderName(branch)

        // Construct a new object and return
        return new Repo(project, folders, branch, server, definitionScript, utilitiesRepo,
                        utilitiesRepoBranch, collection, credentials,
                        isDSLTest, useFilteredGenerationTriggers)
    }
}

Repo[] repos = []

// Stream in the repo list (passed in by parameter).
streamFileFromWorkspace(RepoListLocation).eachLine { line ->
    // Skip comment lines
    boolean skip = (line ==~ / *#.*/);
    line.trim()
    skip |= (line == '')
    if (skip) {
        // Return from closure
        return;
    }

    repos += Repo.parseInputString(line, ServerName, SDKImplementationBranch, isVSTS, out)
}

// Post Processing
// Process each repo and determine the other tracked branches, the PR target branches,
// and the PR skip branches

repos.each { repoInfo ->
    def otherRepos = repos.findAll { searchRepoInfo ->
        // Same project
        searchRepoInfo.project == repoInfo.project &&
        // Different branch
        searchRepoInfo.branch != repoInfo.branch
    }

    // Consistency check
    // Find other projects that have the same project, same branch, and same definition script
    assert repos.find { searchRepoInfo ->
        // Not the exact same item
        repoInfo != searchRepoInfo &&
        // Same project
        searchRepoInfo.project == repoInfo.project &&
        // Same server
        searchRepoInfo.server == repoInfo.server &&
        // Same branch
        searchRepoInfo.branch == repoInfo.branch &&
        // Same CI file.  Note this isn't perfect, since there could be overlap
        // based on glob syntax.  But it should prevent most errors.
        searchRepoInfo.definitionScript == repoInfo.definitionScript
    } == null
}

// Now that we have all the repos, generate the jobs
def dslFactory = this
repos.each { repoInfo ->

    // Determine whether we should skip this repo becuase it resides on a different server
    if (repoInfo.server != ServerName) {
        return;
    }

    // Make the folders. Save the root folder for overview generation
    def generatorFolder = ''
    def projectName = ''
    for (folderElement in repoInfo.folders) {
        if (generatorFolder == '') {
            generatorFolder = folderElement
            projectName = folderElement
        }
        else {
            // Append a new folder
            generatorFolder += "/${folderElement}"
        }
        folder(generatorFolder) {}
        Utilities.addStandardFolderView(dslFactory, generatorFolder, projectName)
    }

    // Make the PR test folder
    def generatorPRTestFolder = "${generatorFolder}/GenPRTest"

    // Create a Folder for generator PR tests under that.
    folder(generatorPRTestFolder) {}

    // Generator folder is based on the name of the definition script name.  Now,
    // the definition script is a glob syntax, so we need to do a little processing
    def generatorJobBaseName = 'generator'
    def definitionScriptSuffix = repoInfo.definitionScript

    // Strip out before the last \ or /
    def lastSlash = Math.max(definitionScriptSuffix.lastIndexOf('/'), definitionScriptSuffix.lastIndexOf('\\'))

    if (lastSlash != -1) {
        definitionScriptSuffix = definitionScriptSuffix.substring(lastSlash+1)
    }

    // Now remove * and .groovy
    definitionScriptSuffix = definitionScriptSuffix.replace("*", "")
    definitionScriptSuffix = definitionScriptSuffix.replace(".groovy", "")

    [true, false].each { isPRTest ->
        // In cases where we are making a PR to the utilities repo, we should run the DSL
        // tests by default against the CI SDK from the PR.
        boolean isDSLPRFromSameRepo = isPRTest && repoInfo.isDSLTest && (repoInfo.utilitiesRepo == repoInfo.project)
        def fullGeneratorName = Utilities.getFullJobName(generatorJobBaseName, isPRTest, isPRTest ? generatorPRTestFolder : generatorFolder)
        def jobGenerator = job(fullGeneratorName) {
            // Need multiple scm's
            multiscm {
                git {
                    remote {
                        if (isVSTS) {
                            url(Utilities.calculateVSTSGitURL('mseng', repoInfo.utilitiesRepo))
                            credentials('vsts-dotnet-ci-trusted-creds')
                        }
                        else {
                            url("https://github.com/${repoInfo.utilitiesRepo}")
                        }

                        if (isDSLPRFromSameRepo) {
                            if (isVSTS) {
                                // TODO: VSTS PR refspec
                            }
                            else {
                                refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                            }
                        }
                    }
                    extensions {
                        relativeTargetDirectory('dotnet-ci-sdk')
                        cloneOptions {
                            timeout(30)
                            if (isPRTest) {
                                shallow(true)
                            }
                        }
                    }
                    // If this is a PR DSL test, then pull the SDK from the PR branch
                    if (isDSLPRFromSameRepo) {
                        if (isVSTS) {
                            // TODO: VSTS PR branch
                        }
                        else {
                            branch('${sha1}')
                        }
                    }
                    else {
                        branch("*/${repoInfo.utilitiesRepoBranch}")
                    }
                }
                //
                git {
                    remote {
                        if (isVSTS) {
                            url(Utilities.calculateVSTSGitURL(repoInfo.collection, repoInfo.project))
                            credentials(repoInfo.credentials)
                        }
                        else {
                            github(repoInfo.project)
                        }

                        if (isPRTest) {
                            if (isVSTS) {
                                // TODO: VSTS PR refspec
                            }
                            else {
                                refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                            }
                        }
                    }
                    def targetDir = Utilities.getRepoName(repoInfo.project)
                    // Want the relative to be just the project name
                    extensions {
                        relativeTargetDirectory(targetDir)
                        cloneOptions {
                            timeout(30)
                            if (isPRTest) {
                                shallow(true)
                            }
                        }
                    }

                    // If PR, change to ${sha1}
                    // If not a PR, then the branch name should be the target branch
                    if (isPRTest) {
                        if (isVSTS) {
                            // TODO: VSTS PR branch
                        }
                        else {
                            branch('${sha1}')
                        }
                    }
                    else {
                        branch("*/${repoInfo.branch}")
                    }

                    // Set up polling ignore, unless this is a DSL test
                    if (!repoInfo.isDSLTest && repoInfo.useFilteredGenerationTriggers) {
                        configure { node ->
                            node /'extensions' << 'hudson.plugins.git.extensions.impl.PathRestriction' {
                                // Not sure whether polling takes into account the target dir, so just
                                // put multiple entries
                                includedRegions "${targetDir}/${repoInfo.definitionScript}\n${repoInfo.definitionScript}"
                                excludedRegions ''
                            }
                        }
                    }
                }
            }

            // Add a parameter for the project, so that gets passed to the
            // DSL groovy file
            parameters {
                // VSTS/GitHub specific parameters
                // The intention is to make it so groovy/pipelines are roughly
                // moveable across VSTS and GitHub.  As such the naming doesn't make much sense for some of these
                // parameters.  However, a ton of places reference these parameters already, so we will keep these
                // around but introduce new ones with more generic names.
                if (isVSTS) {
                    stringParam('VSTSCollectionName', repoInfo.collection, 'VSTS collection name')
                    stringParam('VSTSCredentialsId', repoInfo.credentials, 'VSTS credentials id')
                }
                else {
                    stringParam('GithubProject', repoInfo.project, 'Project name passed to the DSL generator')
                    stringParam('GithubProjectName', Utilities.getProjectName(repoInfo.project), 'Project name')
                    stringParam('GithubOrgName', Utilities.getOrgName(repoInfo.project), 'Organization name')
                    stringParam('GithubBranchName', repoInfo.branch, 'Branch name passed to the DSL generator')
                }

                // Pass along the server name so that we could potentially identify it in some status check
                stringParam('ServerName', repoInfo.server, 'Server that the project is hosted on')
                // Generic SCM parameters
                stringParam('QualifiedRepoName', repoInfo.project, 'Full project/repo passed to the DSL generator')
                stringParam('RepoName', Utilities.getRepoName(repoInfo.project), 'Repo name')
                stringParam('OrgOrProjectName', Utilities.getOrgOrProjectName(repoInfo.project), 'Organization/VSTS project name')
                stringParam('BranchName', repoInfo.branch, 'Branch name passed to the DSL generator')
                // Pass along the version control location (useful for tests)
                stringParam('VersionControlLocation', VersionControlLocation, 'Where the version control sits (VSTS or GitHub)')

                // If this repo is a DSL test, then IsTestGeneration is always true
                booleanParam('IsTestGeneration', isPRTest || repoInfo.isDSLTest, 'Is this a test generation?')
            }

            // Add in the job generator logic

            steps {
                jobDsl {
                    String dsltargets = "dotnet-ci-sdk/src/jobs/generation/PreGen.groovy"
                    dsltargets += "\n${Utilities.getRepoName(repoInfo.project)}/${repoInfo.definitionScript}"
                    dsltargets += "\ndotnet-ci-sdk/src/jobs/generation/PostGen.groovy"

                    targets(dsltargets)

                    // Additional classpath should point to the utility repo
                    additionalClasspath('dotnet-ci-sdk/src')

                    // Fail the build if a plugin is missing
                    failOnMissingPlugin(true)

                    // Generate jobs relative to the seed job.
                    lookupStrategy('SEED_JOB')

                    // Run in the sandbox
                    sandbox(true)

                    // PR tests should do nothing with the other jobs.
                    // Non-PR tests should disable the jobs, which will get cleaned
                    // up later.
                    if (isPRTest) {
                        removedJobAction('IGNORE')
                    }
                    else {
                        // Normally this would be DISABLE.  But currently pipeline
                        // jobs have some issues. Until this is fixed, use DELETE
                        removedJobAction('DELETE')
                    }
                    removedViewAction('DELETE')
                }
            }

            // Disable concurrent builds
            concurrentBuild(false)

            // 5 second quiet period before the job can be scheduled
            quietPeriod(5)

            wrappers {
                timestamps()
                // 10 minute execution timeout
                timeout {
                    absolute(10)
                }
            }
        }

        // Set the job to run on any generator enabled node.  Basically just has to have git.
        Utilities.setMachineAffinity(jobGenerator, 'Generators', 'latest')

        if (isPRTest) {
            if (isVSTS) {
                // TODO: VSTS PR trigger
            }
            else {
                // Enable the github PR trigger, but add a trigger phrase so
                // that it doesn't build on every change, except if this is a DSL test, in which case it
                // automatically is triggered
                if(repoInfo.isDSLTest) {
                    Utilities.addDefaultPrivateGithubPRTriggerForBranch(jobGenerator, repoInfo.branch, "Gen CI(${repoInfo.server}) - ${repoInfo.branch}/${repoInfo.definitionScript}", ['Microsoft'], null)
                }
                else {
                    Utilities.addPrivateGithubPRTriggerForBranch(jobGenerator, repoInfo.branch, "Gen CI(${repoInfo.server}) - ${repoInfo.branch}/${repoInfo.definitionScript}", '(?i).*test\\W+ci.*', ['Microsoft'], null)
                }
            }
        }
        else {
            // Enable the push trigger.
            jobGenerator.with {
                triggers {
                    if (repoInfo.useFilteredGenerationTriggers) {
                        scm('H/15 * * * *') {
                            ignorePostCommitHooks(true)
                        }
                    } else {
                        githubPush()
                    }
                }
            }
        }
    }
}

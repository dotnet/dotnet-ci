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

class Repo {
    String project
    String[] folders
    String branch
    String server
    String definitionScript
    // Indicates this is a branch for 'default' PRs. ORs to branches other than the tracked branches would use jobs
    // defined here.  If true, then the generator will receive .* + branch +  for GithubPRTargetBranches with the other tracked
    // branches  in the GithubPRSkipBranches parameter.
    boolean isDefaultPRBranch
    // Indicates this branch provides PR coverage for the listed branches.  If set, the GithubPRTargetBranches will be
    // set to this branch + prsForBranches.  GithubPRSkipBranches remains empty
    String[] additionalPRBranches = []
    // The location of the Utilities repo
    String utilitiesRepo
    // The branch for the utilities repo
    String utilitiesRepoBranch
    // True if the paths for job generation should be filtered, false if it should
    // be triggered on every change. https://github.com/dotnet/core-eng/issues/1531
    boolean useFilteredGenerationTriggers

    // Lazily set up data
    // Branches that should be targeted for PRs against this job definition
    String[] prTargetBranches
    // Branches that should be skipped for PRs against this job definition
    String[] prSkipBranches

    def Repo(String project,
             String[] folders,
             String branch,
             String server,
             String definitionScript,
             boolean isDefaultPRBranch,
             String[] additionalPRBranches,
             String utilitiesRepo,
             String utilitiesRepoBranch,
             boolean useFilteredGenerationTriggers) {
        this.project = project
        this.folders = folders
        this.branch = branch
        this.server = server
        this.definitionScript = definitionScript
        this.isDefaultPRBranch = isDefaultPRBranch
        this.additionalPRBranches = additionalPRBranches
        this.utilitiesRepo = utilitiesRepo
        this.utilitiesRepoBranch = utilitiesRepoBranch
        this.useFilteredGenerationTriggers = useFilteredGenerationTriggers
    }

    // Parse the input string and return a Repo object
    def static parseInputString(String input, def out) {
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
        // File name/path is usually netci.groovy, but can be set arbitrarily
        String definitionScript = 'netci.groovy'
        // Additional PR branches
        String[] additionalPRBranches = []
        // Is the default PR branch?
        boolean isDefaultPRBranch = false
        // Repo for Utilities that are used by the job
        String utilitiesRepo = 'dotnet/dotnet-ci'
        // Branch that the utilities should be read from
        String utilitiesRepoBranch = 'master'
        // True if the paths for job generation should be filtered, false if it should
        // be triggered on every change. https://github.com/dotnet/core-eng/issues/1531
        boolean useFilteredGenerationTriggers = true

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
            else if(element.startsWith('branch=')) {
                branch = element.substring('branch='.length())
            }
            else if(element.startsWith('server=')) {
                server = element.substring('server='.length())
            }
            else if(element.startsWith('definitionScript=')) {
                definitionScript = element.substring('definitionScript='.length())
            }
            else if(element.startsWith('additionalPRBranches=')) {
                // Parse out the folder names
                additionalPRBranches = element.substring('additionalPRBranches='.length()).tokenize(',')
            }
            else if(element.startsWith('isDefaultPRBranch=')) {
                // Parse out the folder names
                isDefaultPRBranch = element.substring('isDefaultPRBranch='.length()).toBoolean()
            }
            else if(element.startsWith('utilitiesRepo=')) {
                // Parse out the folder names
                utilitiesRepo = element.substring('utilitiesRepo='.length())
            }
            else if(element.startsWith('utilitiesRepoBranch=')) {
                // Parse out the folder names
                utilitiesRepoBranch = element.substring('utilitiesRepoBranch='.length())
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

        folders = [Utilities.getFolderName(project)]

        // If they asked for subfolders, add them
        if (subFolders != null) {
            folders += subFolders
        }

        // Add the branch after
        folders += Utilities.getFolderName(branch)

        // Construct a new object and return
        return new Repo(project, folders, branch, server, definitionScript, isDefaultPRBranch, additionalPRBranches, utilitiesRepo, utilitiesRepoBranch, useFilteredGenerationTriggers)
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

    repos += Repo.parseInputString(line, out)
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
        // Same branch
        searchRepoInfo.branch == repoInfo.branch &&
        // Same CI file.  Note this isn't perfect, since there could be overlap
        // based on glob syntax.  But it should prevent most errors.
        searchRepoInfo.definitionScript == repoInfo.definitionScript
    } == null

    repoInfo.prTargetBranches = []
    repoInfo.prSkipBranches = []

    // Determine the prTargetBranches and prSkipBranches
    if (repoInfo.isDefaultPRBranch) {
        repoInfo.prTargetBranches = ['.*']
        repoInfo.prSkipBranches = otherRepos.branch + otherRepos.additionalPRBranches.flatten()
    }
    else {
        // Otherwise, the target branch is the branch + additional Branches
        repoInfo.prTargetBranches = repoInfo.additionalPRBranches
        repoInfo.prSkipBranches = otherRepos.branch + otherRepos.additionalPRBranches.flatten()
    }
    repoInfo.prTargetBranches += ((String[])[repoInfo.branch])
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
        def jobGenerator = job(Utilities.getFullJobName(generatorJobBaseName, isPRTest, isPRTest ? generatorPRTestFolder : generatorFolder)) {
            // Need multiple scm's
            multiscm {
                git {
                    // We grab the utilities repo, the add a suffix of the sdk implementation version
                    remote {
                        url("https://github.com/${repoInfo.utilitiesRepo}")
                    }
                    // On older versions of DSL this is a top level git element called relativeTargetDir
                    extensions {
                        relativeTargetDirectory('dotnet-ci')
                        cloneOptions {
                            timeout(30)
                            if (isPRTest) {
                                shallow(true)
                            }
                        }
                    }
                    // dotnet-ci always pulls from master
                    branch("*/${repoInfo.utilitiesRepoBranch}${SDKImplementationBranchSuffix}")
                }
                //
                git {
                    remote {
                        github(repoInfo.project)

                        if (isPRTest) {
                            refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                        }
                    }
                    def targetDir = Utilities.getProjectName(repoInfo.project)
                    // Want the relative to be just the project name
                    // On older versions of DSL this is a top level git element called relativeTargetDir
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
                        branch('${sha1}')
                    }
                    else {
                        branch("*/${repoInfo.branch}")
                    }

                    // If utilitizing filtered paths, add here.
                    if (repoInfo.useFilteredGenerationTriggers) {
                        // Set up polling ignore
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
                stringParam('GithubProject', repoInfo.project, 'Project name passed to the DSL generator')
                stringParam('GithubBranchName', repoInfo.branch, 'Branch name passed to the DSL generator')
                stringParam('GithubPRTargetBranches', repoInfo.prTargetBranches.join(','), 'Branches that should be tracked for PRs')
                stringParam('GithubPRSkipBranches', repoInfo.prSkipBranches.join(','), 'Branches that should be skipped for PRs')
                booleanParam('IsTestGeneration', isPRTest, 'Is this a test generation?')
            }

            // Add in the job generator logic

            steps {
                dsl {
                    // Loads the PreGen groovy file
                    external("dotnet-ci/jobs/generation/PreGen.groovy")
                    // Loads DSL groovy file from the repo
                    external(Utilities.getProjectName(repoInfo.project) + "/${repoInfo.definitionScript}")
                    // Loads the PostGen groovy file
                    external("dotnet-ci/jobs/generation/PostGen.groovy")

                    // Additional classpath should point to the utility repo
                    additionalClasspath('dotnet-ci')

                    // Generate jobs relative to the seed job.
                    lookupStrategy('SEED_JOB')

                    // PR tests should do nothing with the other jobs.
                    // Non-PR tests should disable the jobs, which will get cleaned
                    // up later.
                    if (isPRTest) {
                        removeAction('IGNORE')
                    }
                    else {
                        removeAction('DISABLE')
                    }
                    removeViewAction('DELETE')
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
        Utilities.setMachineAffinity(jobGenerator, 'Generators', 'latest-or-auto')

        if (isPRTest) {
            // Enable the github PR trigger, but add a trigger phrase so
            // that it doesn't build on every change.
            Utilities.addGithubPRTriggerForBranch(jobGenerator, repoInfo.branch, jobGenerator.name, '(?i).*test\\W+ci.*')
        }
        else {
            // Enable the github push trigger.
            jobGenerator.with {
                triggers {
                    if (useFilteredGenerationTriggers) {
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

package jobs.generation;

class TriggerBuilder {
    public enum TriggerType {
        PERIODIC,
        COMMIT,
        PULLREQUEST
    }
    
    TriggerType triggerType;
    
    // Trigger type specific settings
    // Commit
    
    // Periodic
    String cronString
    // Always run regardless of the commit state
    boolean alwaysRun = false
    
    // PullRequest
    String context = null
    String triggerPhrase = null
    boolean alwaysTrigger = true
    boolean alwaysTriggerModified = false
    List<String> targetBranches = []
    List<String> skipBranches = []
    List<String> permittedOrgs = []
    List<String> permittedUsers = []
    private boolean used = false;
    
    private def TriggerBuilder(TriggerType triggerType) {
        this.triggerType = triggerType
    }
    
    // Commit trigger setup
    
    def static TriggerBuilder triggerOnCommit() {
        return new TriggerBuilder(TriggerType.COMMIT)
    }
    
    // Periodic trigger setup

    // Constructs a new periodic trigger
    // Parameters:
    //  cronString - Cron string to run the job on
    // Returns:
    //  a new periodic trigger that runs on the specified interval
    def static TriggerBuilder triggerPeriodically(String cronString) {
        return new TriggerBuilder(TriggerType.PERIODIC)
    }
    
    // Forces the periodic trigger to run regardless of source change
    def alwaysRunPeriodicTrigger() {
        assert triggerType == TriggerType.PERIODIC
        assert !used
        this.alwaysRun = true
    }
    
    // PR Trigger set up
    // Constructs a new pull request trigger
    def static TriggerBuilder triggerOnPullRequest() {
        return new TriggerBuilder(TriggerType.PULLREQUEST)
    }
    
    // Sets the context that the PR shows up as in the github UI.  If no context is
    // is set, then the job that this trigger is emitted on will be the context.
    // context
    def setGithubContext(String context) {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        this.context = context
        // If the trigger phrase isn't set yet, then set it now
        if (this.triggerPhrase == null) {
            this.triggerPhrase  = "(?i).*test\\W+${java.util.regex.Pattern.quote(context)}.*"
        }
    }

    // Sets the context
    def setContext(String context) {
        
    }
    
    // Sets the trigger phrase.  If a trigger phrase is not explicitly set, test + <context> is used
    // Parameters:
    //  triggerPhrase - Regex trigger phrase to used
    // Notes:
    //  When this is used, the 'always trigger' will be set to false unless that setting
    // is modified
    def setCustomTriggerPhrase(String triggerPhrase) {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        this.triggerPhrase = triggerPhrase
        if (!this.alwaysTriggerModified) {
            this.alwaysTrigger = false
        }
    }
    
    // Sets the PR job to trigger by default.
    def triggerByDefault() {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        this.alwaysTrigger = true
        this.alwaysTriggerModified = true
    }
    
    // Sets the PR job to trigger only when the trigger phrase is commented.
    def triggerOnlyOnComment() {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        this.alwaysTrigger = false
        this.alwaysTriggerModified = true
    }
    
    // Set the job to trigger on the specified branch (adds to list)
    // Parameters:
    //  branch - Branch to trigger on.  Regular expression
    def triggerForBranch(String branch) {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        triggerForBranches([branch])
    }
    
    // Set the job to trigger on the specified branches (adds to list)
    // Parameters:
    //  branches - Array of branches to trigger on. Regular expressions
    def triggerForBranches(List<String> branches) {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        targetBranches.addAll(branches)
    }
    
    // Set the job to not trigger on the specified branch (adds to list)
    // Parameters:
    //  branch - Branch to not trigger on.  Regular expression
    def doNotTriggerForBranch(String branch) {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        doNotTriggerForBranches([branch])
    }
    
    // Set the job to not trigger on the specified branches (adds to list)
    // Parameters:
    //  branches - Branches to not trigger on.  Regular expressions
    def doNotTriggerForBranches(List<String> branches) {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        skipBranches.addAll(branches)
    }
    
    // Set the job to trigger on the specified branches and not on another set.
    // Parameters:
    //  triggerBranches - Branches to trigger on.  Regular expressions
    //  doNotTriggerForBranches - Branches to not trigger on.  Regular expressions
    def setTriggerBranches(List<String> triggerBranches, List<String> notTriggerForBranches) {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        triggerForBranches(triggerBranches)
        doNotTriggerForBranches(notTriggerForBranches)
    }
    
    // Sets the PR to be run only if the submitter is in the specified org.
    // Parameters:
    //  org - Github organization to permit
    // Notes:
    //  By default, jobs are runnable by all submitters
    def permitOrg(String org) {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        permitOrgs([org])
    }
    
    // Sets the PR to be run only if the submitter is in one of the specified orgs.
    // Parameters:
    //  orgs - Array of Github organizations to permit
    // Notes:
    //  By default, jobs are runnable by all submitters 
    def permitOrgs(List<String> orgs) {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        permittedOrgs += orgs
        permittedOrgs = permittedOrgs.flatten()
    }
    
    // Sets the PR to be run only if the submitter is one of the allowed users
    // Parameters:
    //  user - Allow the user to trigger the PR job
    // Notes:
    //  By default, jobs are runnable by all submitters 
    def permitUser(String user) {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        permitUsers([user])
    }
    
    // Sets the PR to be run only if the submitter is one of the allowed users
    // Parameters:
    //  users - Array of users allowed to trigger the PR job
    // Notes:
    //  By default, jobs are runnable by all submitters 
    def permitUsers(List<String> users) {
        assert triggerType == TriggerType.PULLREQUEST
        assert !used
        permittedUsers += users
        permittedUsers = permittedUsers.flatten()
    }
    
    // Emits the trigger for a job
    // Parameters:
    //  job - Job to emit the trigger for.
    def emitTrigger(def job) {
    
        if (triggerType == TriggerType.PULLREQUEST) {
            this.emitPRTrigger(job)
        }
        else if (triggerType == TriggerType.COMMIT) {
            this.emitCommitTrigger(job)
        }
        else {
            assert false
        }
    }
    
    def private emitCommitTrigger(def job) {
        job.with {
            triggers {
                githubPush()
            }
        }

        // Record the push trigger.  We look up in the side table to see what branches this
        // job was set up to build
        JobReport.Report.addPushTriggeredJob(job.name)
        Utilities.addJobRetry(job)
    }
    
    def private emitPRTrigger(def job) {
        assert !used
        
        def boolean permitAllSubmitters = (permittedUsers.size() == 0 && permittedOrgs.size() == 0)
        
        job.with {
            triggers {
                githubPullRequest {
                    useGitHubHooks()
                    // Add default individual admins here
                    admin('mmitche')
                    if (permitAllSubmitters) {
                        permitAll()
                    }
                    else {
                        assert permittedOrgs.size() != 0 || permittedUsers.size() != 0
                        permitAll(false)
                        if (permittedUsers.size() != 0) {
                            permittedUsers.each { permittedUser ->
                                admin(permittedUser)
                            }
                        }
                        if (permittedOrgs.size() != 0) {
                            permittedOrgs.each { permittedOrg ->
                                orgWhitelist(permittedOrg)
                            }
                            allowMembersOfWhitelistedOrgsAsAdmin(true)
                        }
                    }
                    extensions {
                        commitStatus {
                            context(this.context)
                            updateQueuePosition(true)
                        }
                    }
                    
                    if (!alwaysTrigger) {
                        onlyTriggerPhrase(true)
                    }
                    triggerPhrase(this.triggerPhrase)
                    
                    if (targetBranches.size() != 0) {
                        whiteListTargetBranches(targetBranches)
                    }
                    if (skipBranches.size() != 0) {
                        // When this is implemented and rolled out, enable
                        blackListTargetBranches(skipBranches)
                    }
                }
            }
            
            JobReport.Report.addPRTriggeredJob(job.name, (String[])targetBranches.toArray(), this.context, this.triggerPhrase, alwaysTrigger)
        }
        Utilities.addJobRetry(job)
    }
}

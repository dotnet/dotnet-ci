package org.dotnet.ci.triggers;

import jobs.generation.JobReport
import jobs.generation.Utilities

// Trigger builder for interfacing with GitHub.  This is a direct
// copy of the jobs/generation/TriggerBuilder class.  
class GithubTriggerBuilder implements TriggerBuilder {
    public enum TriggerType {
        COMMIT,
        PULLREQUEST
    }
    
    TriggerType _triggerType;
    
    // PullRequest
    String _context = null
    String _triggerPhrase = null
    boolean _alwaysTrigger = true
    boolean _alwaysTriggerModified = false
    List<String> _targetBranches = []
    List<String> _skipBranches = []
    List<String> _permittedOrgs = []
    List<String> _permittedUsers = []
    
    private def GithubTriggerBuilder(TriggerType triggerType) {
        this._triggerType = triggerType
    }
    
    // Commit trigger setup
    
    def static GithubTriggerBuilder triggerOnCommit() {
        return new GithubTriggerBuilder(TriggerType.COMMIT)
    }
    
    // PR Trigger set up
    // Constructs a new pull request trigger
    def static GithubTriggerBuilder triggerOnPullRequest() {
        return new GithubTriggerBuilder(TriggerType.PULLREQUEST)
    }
    
    // Sets the context that the PR shows up as in the github UI.  If no context is
    // is set, then the job that this trigger is emitted on will be the context.
    // context
    def setGithubContext(String context) {
        assert this._triggerType == TriggerType.PULLREQUEST
        this._context = context
        // If the trigger phrase isn't set yet, then set it now
        if (this._triggerPhrase == null) {
            this._triggerPhrase  = "(?i).*test\\W+${context}.*"
        }
    }
    
    // Sets the trigger phrase.  If a trigger phrase is not explicitly set, test + <context> is used
    // Parameters:
    //  triggerPhrase - Regex trigger phrase to use
    // Notes:
    //  When this is used, the 'always trigger' will be set to false unless that setting
    //  is modified
    def setCustomTriggerPhrase(String triggerPhrase) {
        assert this._triggerType == TriggerType.PULLREQUEST
        this._triggerPhrase = triggerPhrase
        if (!this._alwaysTriggerModified) {
            this._alwaysTrigger = false
        }
    }
    
    // Sets the PR job to trigger by default.
    def triggerByDefault() {
        assert this._triggerType == TriggerType.PULLREQUEST
        this._alwaysTrigger = true
        this._alwaysTriggerModified = true
    }
    
    // Sets the PR job to trigger only when the trigger phrase is commented.
    def triggerOnlyOnComment() {
        assert this._triggerType == TriggerType.PULLREQUEST
        this._alwaysTrigger = false
        this._alwaysTriggerModified = true
    }
    
    // Set the job to trigger on the specified branch (adds to list)
    // Parameters:
    //  branch - Branch to trigger on.  Regular expression
    def triggerForBranch(String branch) {
        assert this._triggerType == TriggerType.PULLREQUEST
        triggerForBranches([branch])
    }
    
    // Set the job to trigger on the specified branches (adds to list)
    // Parameters:
    //  branches - Array of branches to trigger on. Regular expressions
    def triggerForBranches(List<String> branches) {
        assert this._triggerType == TriggerType.PULLREQUEST
        this._targetBranches.addAll(branches)
    }
    
    // Set the job to not trigger on the specified branch (adds to list)
    // Parameters:
    //  branch - Branch to not trigger on.  Regular expression
    def doNotTriggerForBranch(String branch) {
        assert this._triggerType == TriggerType.PULLREQUEST
        doNotTriggerForBranches([branch])
    }
    
    // Set the job to not trigger on the specified branches (adds to list)
    // Parameters:
    //  branches - Branches to not trigger on.  Regular expressions
    def doNotTriggerForBranches(List<String> branches) {
        assert this._triggerType == TriggerType.PULLREQUEST
        this._skipBranches.addAll(branches)
    }
    
    // Set the job to trigger on the specified branches and not on another set.
    // Parameters:
    //  triggerBranches - Branches to trigger on.  Regular expressions
    //  doNotTriggerForBranches - Branches to not trigger on.  Regular expressions
    def setTriggerBranches(List<String> triggerBranches, List<String> notTriggerForBranches) {
        assert this._triggerType == TriggerType.PULLREQUEST
        triggerForBranches(triggerBranches)
        doNotTriggerForBranches(notTriggerForBranches)
    }
    
    // Sets the PR to be run only if the submitter is in the specified org.
    // Parameters:
    //  org - Github organization to permit
    // Notes:
    //  By default, jobs are runnable by all submitters
    def permitOrg(String org) {
        assert this._triggerType == TriggerType.PULLREQUEST
        permitOrgs([org])
    }
    
    // Sets the PR to be run only if the submitter is in one of the specified orgs.
    // Parameters:
    //  orgs - Array of Github organizations to permit
    // Notes:
    //  By default, jobs are runnable by all submitters 
    def permitOrgs(List<String> orgs) {
        assert this._triggerType == TriggerType.PULLREQUEST
        this._permittedOrgs += orgs
        this._permittedOrgs = this._permittedOrgs.flatten()
    }
    
    // Sets the PR to be run only if the submitter is one of the allowed users
    // Parameters:
    //  user - Allow the user to trigger the PR job
    // Notes:
    //  By default, jobs are runnable by all submitters 
    def permitUser(String user) {
        assert this._triggerType == TriggerType.PULLREQUEST
        permitUsers([user])
    }
    
    // Sets the PR to be run only if the submitter is one of the allowed users
    // Parameters:
    //  users - Array of users allowed to trigger the PR job
    // Notes:
    //  By default, jobs are runnable by all submitters 
    def permitUsers(List<String> users) {
        assert this._triggerType == TriggerType.PULLREQUEST
        this._permittedUsers += users
        this._permittedUsers = this._permittedUsers.flatten()
    }

    // Returns true if the trigger is a PR (private or PR branch) SCM, vs. 'official'
    public boolean isPRTrigger() {
        return (this._triggerType == TriggerType.PULLREQUEST)
    }
    
    // Emits the trigger for a job
    // Parameters:
    //  job - Job to emit the trigger for.
    void emitTrigger(def job) {
    
        if (this._triggerType == TriggerType.PULLREQUEST) {
            this.emitPRTrigger(job)
        }
        else if (this._triggerType == TriggerType.COMMIT) {
            this.emitCommitTrigger(job)
        }
        else {
            assert false
        }
    }
    
    def private emitCommitTrigger(def job) {
        job.with {
            triggers {
                scm('H/15 * * * *')
            }
        }

        // Record the push trigger.  We look up in the side table to see what branches this
        // job was set up to build
        JobReport.Report.addPushTriggeredJob(job.name)
        Utilities.addJobRetry(job)
    }
    
    def private emitPRTrigger(def job) {
        def boolean permitAllSubmitters = (this._permittedUsers.size() == 0 && this._permittedOrgs.size() == 0)
        
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
                        assert this._permittedOrgs.size() != 0 || this._permittedUsers.size() != 0
                        permitAll(false)
                        if (this._permittedUsers.size() != 0) {
                            this._permittedUsers.each { permittedUser ->
                                admin(permittedUser)
                            }
                        }
                        if (this._permittedOrgs.size() != 0) {
                            this._permittedOrgs.each { permittedOrg ->
                                orgWhitelist(permittedOrg)
                            }
                            allowMembersOfWhitelistedOrgsAsAdmin(true)
                        }
                    }
                    extensions {
                        commitStatus {
                            context(this._context)
                            updateQueuePosition(true)
                        }
                    }
                    
                    if (!_alwaysTrigger) {
                        onlyTriggerPhrase(true)
                    }
                    triggerPhrase(this._triggerPhrase)
                    
                    if (this._targetBranches.size() != 0) {
                        whiteListTargetBranches(this._targetBranches)
                    }
                    if (this._skipBranches.size() != 0) {
                        // When this is implemented and rolled out, enable
                        blackListTargetBranches(this._skipBranches)
                    }
                }
            }
            
            JobReport.Report.addPRTriggeredJob(job.name, (String[])this._targetBranches.toArray(), this._context, this._triggerPhrase, this._alwaysTrigger)
        }
        Utilities.addJobRetry(job)
    }
}

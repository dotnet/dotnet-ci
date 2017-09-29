package org.dotnet.ci.triggers;

import jobs.generation.JobReport
import jobs.generation.Utilities

/**
* Trigger builder for VSTS
*
*/
class VSTSTriggerBuilder implements TriggerBuilder {
    public enum TriggerType {
        COMMIT,
        PULLREQUEST
    }
    
    TriggerType _triggerType
    String _contextString
    String _targetBranch

    /**
     * Construct a new VSTS trigger builder
     *
     * @param triggerType Type of VSTS trigger
     */
    private def VSTSTriggerBuilder(TriggerType triggerType) {
        this._triggerType = triggerType
    }

    /**
     * Construct a new VSTS trigger builder
     *
     * @param triggerType Type of VSTS trigger
     *
     * @param contextString Context string of the job to be triggered
     */
    private def VSTSTriggerBuilder(TriggerType triggerType, String contextString) {
        this._triggerType = triggerType
        this._contextString = contextString
    }

    /**
     * Trigger the pipeline job on a commit
     *
     * @param contextString Context string of the job to be triggered
     *
     * @return New trigger builder
     */
    def static VSTSTriggerBuilder triggerOnCommit(String contextString = null) {
        return new VSTSTriggerBuilder(TriggerType.COMMIT, contextString)
    }
    
    /**
     * Trigger the pipeline job on a PR
     *
     * @param contextString Context string of the job to be triggered
     *
     * @return New trigger builder
     */
    def static VSTSTriggerBuilder triggerOnPullRequest(String contextString = null) {
        return new VSTSTriggerBuilder(TriggerType.PULLREQUEST, contextString)
    }

    /**
     * Set the job to trigger on the specified branch
     *
     * @param branch Branch to trigger on.
     *
     */
    def triggerForBranch(String branch) {
        assert this._triggerType == TriggerType.PULLREQUEST
        this._targetBranch = branch
    }

    /**
     * Returns true if the trigger is a PR trigger
     *
     * @return True if the trigger is a PR trigger
     */
    public boolean isPRTrigger() {
        return (this._triggerType == TriggerType.PULLREQUEST)
    }

    /**
     * Emits the trigger for a job
     *
     * @param job Job to emit the trigger for
     *
     */
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
    
    /**
     * Emits a commit trigger.
     *
     */
    def private emitCommitTrigger(def job) {
        job.with {
            triggers {
                teamPushTrigger {
                    jobContext(this._contextString)
                }
            }
            // Record the push trigger.  We look up in the side table to see what branches this
            // job was set up to build
            JobReport.Report.addPushTriggeredJob(job.name)
        }

        Utilities.addJobRetry(job)
    }

    /**
     * Emits a PR trigger.
     *
     */
    def private emitPRTrigger(def job) {
        job.with {
            triggers {
                teamPRPushTrigger {
                    targetBranches(this._targetBranch)
                    jobContext(this._contextString)
                }
            }
            JobReport.Report.addPRTriggeredJob(job.name, (String[])[this._targetBranch], this._contextString, '', false)
        }

        Utilities.addJobRetry(job)
    }
}

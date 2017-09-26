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
    
    TriggerType _triggerType;
    
    /**
     * Construct a new VSTS trigger builder
     *
     * @param triggerType Type of VSTS trigger
     */
    private def VSTSTriggerBuilder(TriggerType triggerType) {
        this._triggerType = triggerType
    }
    
    /**
     * Trigger the pipeline job on a commit
     *
     * @return New trigger builder
     */
    def static VSTSTriggerBuilder triggerOnCommit() {
        return new VSTSTriggerBuilder(TriggerType.COMMIT)
    }
    
    /**
     * Trigger the pipeline job on a PR
     *
     * @return New trigger builder
     */
    def static VSTSTriggerBuilder triggerOnPullRequest() {
        return new VSTSTriggerBuilder(TriggerType.PULLREQUEST)
    }

    /**
     * Returns true if the trigger is a PR trigger
     *
     * @return True if the trigger is a PR trigger
     */
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
    
    /**
     * Emits a commit trigger.
     *
     */
    def private emitCommitTrigger(def job) {
        job.with {
            triggers {
                teamPushTrigger()
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
                teamPRPushTrigger()
            }
            JobReport.Report.addPRTriggeredJob(job.name)
        }

        Utilities.addJobRetry(job)
    }
}

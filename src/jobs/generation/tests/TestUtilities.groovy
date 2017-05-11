package jobs.generation.tests

import jobs.generation.*

class TestUtilities {
    def static void DisableJob(def myJob) {
        myJob.with {
            disabled(true)
        }
    }

    def static StartTest(def out, String testName, boolean clearJobReport = true) {
        if (clearJobReport) {
            JobReport.Report.clearJobReport()
        }
        out.println("Starting - ${testName}")
    }
    def static EndTest(def out) {
        out.println("  passed")
    }
}
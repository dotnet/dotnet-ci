# Writing CI definitions.

**todo add basic info here on the way that the dsl works**

Many projects have simple definitions, or at least start out with simple definitions.  Clone a repo, run the build and test, gather XUnit.NET test outputs.  While you can write your script from scratch, a lot of utility functionality is available while generating jobs.  This utility functionality is located in (jobs/generation/Utilities.groovy).  Below are annotated examples of CI definitions. The CI definition language is very powerful and can generate a wide variety of jobs very quickly.

* [Simple Build/Test](simple-netci.groovy)

Examples in the wild:

* [dotnet/coreclr](https://github.com/dotnet/coreclr/blob/master/netci.groovy)
* [dotnet/wcf](https://github.com/dotnet/wcf/blob/master/netci.groovy)
  
# How do I...?

This section contains answers to frequently asked questions about how to do certain things.
  
  * Archive artifacts - After creating a job, add a call to Utilities.addArchival.  addArchival takes two parameters: job to add archival for, and the ant style glob pattern indicating what should be archived.  When archiving data, archive only what you need and avoid archiving excessive amounts. A third optional parameters is an exclusion list. Example:
    ```
    // Below we archive everything under bin except anything under obj, and everything under logs.
    Utilities.addArchival(myNewJob, `"bin/**,logs/**"1, `"bin/obj/**"`)
    ```
    
  * Add xunit result gathering - A lot of projects use xunit to report test results.  Jenkins can report and track these results in the UI.  To enable this, you need to tell Jenkins where the xunit result files are.  Make sure your job outputs the results with predictable names so that they can be identified with glob syntax (e.g. `bin/testResults/*.xml` or `bin/**/testResults.xml`).  Use Utilities.addXUnitDotNETResults to add archiving of results:
  ```
  // Below we pull in test results in all files named TestRun*.xml anywhere under the tree rooted at bin.
  Utilities.addXUnitDotNETResults(newJob, 'bin/**/TestRun*.xml')
  ```
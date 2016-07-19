package jobs.generation;

// This class represents a summary builder for a job.  It enables adding generic summaries of various
// types for builds using the groovy postbuild command.  Because only a single groovy script can be executed,
// this class will manage a ccombined script in the background and only emit it when "emitSummaries" is called.

class SummaryBuilder {
    String groovyScript = ''
    
    // Creates a new summary builder
    def SummaryBuilder() {}
    
    // Adds a summary of links from a file in the workspace.  The header is the head of the list
    // and the bullet points are read from a file in the workspace.
    //
    // Parameters:
    //  header - Header text (not a link)
    //  fileName - File in workspace containing a list of links.
    //  summaryIcon - Icon to use.  Defaults to the terminal gif.
    //
    def addLinksSummaryFromFile(String header, String fileName, String summaryIcon = "terminal.gif") {
        groovyScript += """
        // We use the if true to mimic a scoping block
        if (true) {
            // Check whether the file exists:
            def linkFile = manager.build.getWorkspace().child("$fileName")
            if (linkFile.exists()) {
                // Create the new summary
                def newSummary = manager.createSummary("$summaryIcon")
                
                // Append the header
                newSummary.appendText("<b>$header:</b><ul>", false)

                String links = linkFile.readToString()
                links.eachLine { linksToAdd ->
                    newSummary.appendText("<li><a href=\\\"\$linksToAdd\\\">\$linksToAdd</a></li>", false)
                }
            }
        }
        
        """
    }
    
    
    // Emits the postbuild script into the job
    // Currently if the script fails to execute the build will be marked as failed
    def emit(def job) {
        assert groovyScript != ''
        job.with {
            publishers {
                groovyPostBuild(groovyScript, Behavior.MarkFailed)
            }
        }
    }
}
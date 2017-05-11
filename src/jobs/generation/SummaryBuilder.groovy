package jobs.generation;

// This class represents a summary builder for a job.  It enables adding generic summaries of various
// types for builds using the groovy postbuild command.  Because only a single groovy script can be executed,
// this class will manage a ccombined script in the background and only emit it when "emitSummaries" is called.

class SummaryBuilder {
    String groovyScript = """
    // Import causes and save off the original build for usage later
    import hudson.model.Cause
    import hudson.Util
    def originalBuild = manager.build
    """
    
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
    def addLinksSummaryFromFile(String header, String fileName, boolean propagateToUpstream = true) {
        groovyScript += """
        // We use the if true to mimic a scoping block
        if (true) {
            // Check whether the file exists:
            def linkFile = manager.build.getWorkspace().child("$fileName")
            if (linkFile.exists()) {
                // Read the link strings
                String linksText = linkFile.readToString()
                // Parse to array
                String[] links = linksText.split("\\\\r?\\\\n")
                
                // Add the summary text.  Lines is read dynamically in this case.
                ${getSummaryLinkScript(header, "links", propagateToUpstream)}
            }
        }
        
        """
    }
    
    def addSummaryLinks(String header, List<String> links, boolean propagateToUpstream = true) {
        // Create the links array text
        def linksText = "['" + Utilities.joinStrings(links, "','") + "']"
        
        groovyScript += """
        // We use the if true to mimic a scoping block
        if (true) {
            String[] links = $linksText
            ${getSummaryLinkScript(header, "links", propagateToUpstream)}
        }
        """
    }
    
    // Define script text to resolve and expand build variables in a given string)
    private def resolveString(String inputStringName) {
        return "Util.replaceMacro($inputStringName, manager.envVars)"
    }
    
    private def getSummaryLinkScript(String header, String linksVar, boolean propagateToUpstream = true) {
        def summaryText = """
        // Create the new summary
        def newSummary = manager.createSummary("terminal.gif")
        
        // Append the header
        newSummary.appendText("<b>$header:</b><ul>", false)

        if (${linksVar}.size() > 0) {
            ${linksVar}.each { linkToAdd ->
                resolvedLinkToAdd = ${resolveString("linkToAdd")}
                newSummary.appendText("<li><a href=\\\"\$resolvedLinkToAdd\\\">\$resolvedLinkToAdd</a></li>", false)
            }
        }
        else {
            newSummary.appendText("<li>None</li>", false)
        }
        """
        
        if (propagateToUpstream) {
            summaryText += """
            // Attempt to find an upstream job to propagate the summary to.  To do this
            // we grab the causes, and find an upstream cause.
            def causes = manager.build.getCauses()
            for (cause in causes) {
                if (cause instanceof Cause.UpstreamCause) {
                    Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause)cause
                    def upstreamBuild = upstreamCause.getUpstreamRun()
                    // Set the build on the manager, remember we need to set it back afterwards
                    manager.setBuild(upstreamBuild)
                    
                    // Create the new summary
                    def upstreamSummary = manager.createSummary("terminal.gif")
                    
                    // Append the header
                    upstreamSummary.appendText("<b>${header} (from <a href=\\\"/\${originalBuild.getUrl()}\\\">\${originalBuild.getDisplayName()}</a>):</b><ul>", false)
            
                    if (${linksVar}.size() > 0) {
                        ${linksVar}.each { linkToAdd ->
                            resolvedLinkToAdd = ${resolveString("linkToAdd")}
                            upstreamSummary.appendText("<li><a href=\\\"\$resolvedLinkToAdd\\\">\$resolvedLinkToAdd</a></li>", false)
                        }
                    }
                    else {
                        upstreamSummary.appendText("<li>None</li>", false)
                    }
                    
                    // Reset the build back to the original
                    manager.setBuild(originalBuild)
                }
            }
            """
        }
        
        return summaryText
    }
    
    // Emits the postbuild script into the job
    // Currently if the script fails to execute the build will be marked as failed
    def emit(def job) {
        assert groovyScript != ''
        job.with {
            publishers {
                groovyPostBuild {
                    script(groovyScript)
                    behavior(Behavior.MarkFailed)
                    sandbox(true)
                }
            }
        }
    }
}
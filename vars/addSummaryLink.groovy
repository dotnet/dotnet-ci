// Given a header and text, add them to the status page
// as a list

def call (String header, Map urls, boolean isError = false) {
    def newSummary = manager.createSummary("terminal.gif")
    newSummary.appendText("<b>${header}</b><ul>", false)
    if (urls.size() > 0) {
        for ( item in urls ) {
            if(!isError) {
                newSummary.appendText("<li><a href=\""+item.value+"\" \\>"+item.key+"</a></li>", false)
            }
            else {
                newSummary.appendText("<li><a href=\""+item.value+"\" \\>"+item.key+"</a></li>", false, false, false, "red")
            }
            
        }
    }
    newSummary.appendText("</ul>", false)
}
def busyExecutors = Jenkins.instance.computers
                                .collect {
                                  c -> c.executors.findAll { it.isBusy() }
                                }
                                .flatten() // reminder: transforms list(list(executor)) into list(executor)
 
println "Currently executing PRs"
busyExecutors.each { e ->
  def build = e.executable
  def causes = e .executable.getCauses()
  causes.each { cause ->
    if (cause instanceof org.jenkinsci.plugins.ghprb.GhprbCause) {
      println cause.getUrl()
    }
  }
}

return "After restart, please go to each PR and comment 'test this please'"
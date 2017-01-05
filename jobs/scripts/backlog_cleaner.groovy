import org.jenkinsci.plugins.resourcedisposer.AsyncResourceDisposer

def disposer = AsyncResourceDisposer.get()
def backlog = disposer.getBacklog()
println "Removing " + backlog.size() + " items"
backlog.each { item ->
  	disposer.doStopTracking(item.getId())
}
disposer.backlog.clear()
println "Now backlog has " + disposer.getBacklog().size() + " items"
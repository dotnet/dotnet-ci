hudson.model.Hudson.instance.getView('Generators').items.each() {
  if (it instanceof FreeStyleProject) {
     def timeSince = groovy.time.TimeCategory.minus( new Date(), it.getLastBuild().getTime() )
     if (it.getLastBuild().getResult() != Result.SUCCESS) {
      println it.fullDisplayName
      it.scheduleBuild2(60)
    }
  }
}
hudson.model.Hudson.instance.getView('Generators').items.each() {
  if (it instanceof FreeStyleProject) {
     def timeSince = groovy.time.TimeCategory.minus( new Date(), it.getLastBuild().getTime() )
     if (timeSince.getDays() > 5) {
      println it.fullDisplayName + " " + timeSince
      it.scheduleBuild2(60)
    }
  }
}
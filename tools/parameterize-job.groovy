import hudson.plugins.git.BranchSpec;

boolean addParam(job, key, defaultValue, description) { 
    println("[ " + job.name + " ] setting " + key + "=" + defaultValue)
    
    newParam = new StringParameterDefinition(key, defaultValue, description)
    paramDef = job.getProperty(ParametersDefinitionProperty.class)
    
    if (paramDef == null) {
      newArrList = new ArrayList<ParameterDefinition>(1)
      newArrList.add(newParam)
      newParamDef = new ParametersDefinitionProperty(newArrList)
      job.addProperty(newParamDef)
      return true;
    }
    else {
      // Parameters exist! We should check if this one exists already!
      found = paramDef.parameterDefinitions.find{ it.name == key }
      if (found == null) {
        paramDef.parameterDefinitions.add(newParam)
        return true;
      }
      return false;
    }
  
    println(paramDef.parameterDefinitions);
}

// Put the list of jobs here you'd like to modify
def jobList = ['dotnet_buildtools_prtest' ];

for (jobName in jobList) {
  def job = Jenkins.instance.getItem(jobName);
  
  assert job != null;
  
  println('Updating job: ' + jobName);
  
  def gitScm = job.scm
  
  // If the job name ends in _prtest, we need to modify one additional parameter.

  // Grab the current gitScm branch
  
  assert gitScm.branches.size() == 1;
  
  def currentBranch = gitScm.branches[0].name;
    
  if (currentBranch.indexOf('GitBranchOrCommit') == -1) {
    addParam(job, 'GitBranchOrCommit', currentBranch, 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.');

    // Now change the SCM branch specifier so that it takes the 
    def branchSpec = new BranchSpec('$GitBranchOrCommit');
    gitScm.branches = [];
    gitScm.branches.add(branchSpec);
  } else {
    // Already contains a variable.
    println('    Branch already set to: ' + currentBranch); 
  }
  
  // Now read the user remote config and alter the url to now point to a param that defaults
  // to the current url.  If there are multiple user remote configs then we bail
  
  assert gitScm.userRemoteConfigs.size() == 1
  
  def url = gitScm.userRemoteConfigs[0].url;
  
  if (url.indexOf('GitRepoUrl') == -1) {
    addParam(job, 'GitRepoUrl', url, 'Git repo to clone.');  
  	gitScm.userRemoteConfigs[0].url = '$GitRepoUrl'
  } else {
  	// Already set to the new value.
    println('    Url already set to: ' + url);  
  }

  if (jobName.indexOf('_prtest') != -1) {
    // If the job is a prtest job, we need to modify one additional parameter:
    // The refspec for a prtest job must be set by default to: +refs/pull/*:refs/remotes/origin/pr/*
    // For private jobs it should be cleared.
  
    def refspec = gitScm.userRemoteConfigs[0].refspec
    
    // Did someone configure this wrong?
    
    assert refspec != ''
    
    // Check and make sure we didn't already set this:
    
    if (refspec.indexOf('GitRefSpec') == -1) {
      addParam(job, 'GitRefSpec', refspec, 'RefSpec.  WHEN SUBMITTING PRIVATE JOB FROM YOUR OWN REPO, CLEAR THIS FIELD (or it won\'t find your code)');  
  	  gitScm.userRemoteConfigs[0].refspec = '$GitRefSpec'
    } else {
      // Already set to the new value.
      println('    Refspec already set to: ' + refspec); 
    }
  }
  
  job.save();
  
  // print the current params for verification
  println('    Current properties:');
  prop = job.getProperty(ParametersDefinitionProperty.class)
  if(prop != null) {
    for(param in prop.getParameterDefinitions()) {
      try {
        println('      ' + param.name + ' ' + param.defaultValue)
      }
      catch(Exception e) {
        println('      ' + param.name)
      }
    }
  }
}
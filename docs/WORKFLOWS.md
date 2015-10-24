# CI Workflows

## Github PRs

<Explain PR workflow here>

## Private test workflow

There are often times when a dev's fork is ready for testing, but not for GitHub PR.  Typically a dev might do some local testing on the change, but in many cases they may not have the resources or time to do everything.  Luckily, certain jobs can be submitted manually.  To do so:

  1. Log in on the top left.
  2. Locate the job you wish to submit.  You're specifically looking for jobs that are suffixed in _prtest. For example:
http://dotnet-ci.cloudapp.net/job/dotnet_coreclr/job/release_freebsd_prtest/.
  3. Along the left side of the page you'll see "Build With Parameters".  Click that.
  4. Modify the input parameters in the following way:
     a. GitBranchOrCommit - Change to your branch name
     b. GitRepoUrl - Change to the desired repo URL (say https://github.com/mmitche/coreclr.git)
     c. GitRefSpec - Clear this field.
  5. Click Build.
  6. Note the queued build number.  Locating your build later can sometimes be difficult.  If you know the number, you can go directly to the job by appending the job number onto the job url.  Example: http://dotnet-ci.cloudapp.net/job/dotnet_coreclr/job/release_freebsd_prtest/12346/.  If you don't know the number, you may search through the "Build History" on the left side.  Builds submitted manually appear with a small person icon.  Hovering over that icon shows the submitter.

## Commit and Outerloop workflows
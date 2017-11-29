## Onboarding onto .NET CI

Below contains information on how to onboard your project onto Jenkins.

1. Send a PR to dotnet-ci adding your repo to data\repolist.txt.  The server (dotnet-ci, dotnet-ci2 or dotnet-ci3) is specified in the line.  Typically dotnet-ci is used,
2. Ensure your repo is accessible by @dotnet-bot and @mmitche.
3. Configure web hooks for the CI.  You need two entries:
  * A GitHub webhook for push events - Go into the repo settings, click "Webhooks", then click "Add webhook".
      - Payload URL: https://ci.dot.net/github-webhook/ (For projects on dotnet-ci2, use https://ci2.dot.net/github-webhook/, and for projects on dotnet-ci3, use https://ci3.dot.net/github-webhook/)
      - Content type: application/x-www-form-urlencoded
      - "Just send me the push event"
  * A GitHub webhook for pull request events - Go into the repo settings, click "Webhooks", then click "Add webhook".
    - Payload URL: https://ci.dot.net/ghprbhook/ (For projects on dotnet-ci2, use https://ci2.dot.net/ghprbhook/, and for projects on dotnet-ci3, use https://ci3.dot.net/ghprbhook/)
    - Content type: application/x-www-form-urlencoded
    - Shared secret - GitHubPRBuilderSharedSecret from DCIKeyVault
    - "Let me select individual events"
      - Pull request
      - Issue comment
4. Similarly private repos on VSTS also need two entries:
  * A VSTS webhook for push events - Go into the repo settings, click "Service Hooks", then click "Create a new subscription..." ("+" icon)
      - Service: Jenkins, then click "Next"
  - Trigger on this type of event: "Code pushed"
  - Repository: your repo selected from the dropdown list (for example "DotNet-CI-Trusted"), then click "Next"
  - Perform this action: "Trigger Git build"
  - Jenkins base URL: https://dotnet-vsts.westus2.cloudapp.azure.com
  - User name: {youralias}@microsoft.com
  - User API token (or password): Go to https://dotnet-vsts.westus2.cloudapp.azure.com/user/{youralias}@microsoft.com/configure, click "SHOW API TOKEN...", paste "API Token" in.
  - Integration level: "TFS plugin for Jenkins", then click "Test" and "Finish".
  * A VSTS webhook for PR events - Go into the repo settings, click "Service Hooks", then click "Create a new subscription..." ("+" icon)
      - Service: Jenkins, then click "Next"
  - Trigger on this type of event: "Pull request merge commit created"
  - Repository: your repo selected from the dropdown list (for example "DotNet-CI-Trusted"), then click "Next"
  - Perform this action: "Trigger Git build"
  - Jenkins base URL: https://dotnet-vsts.westus2.cloudapp.azure.com
  - User name: {youralias}@microsoft.com
  - User API token (or password): Go to https://dotnet-vsts.westus2.cloudapp.azure.com/user/{youralias}@microsoft.com/configure, click "SHOW API TOKEN...", paste "API Token" in.
  - Integration level: "TFS plugin for Jenkins", then click "Test" and "Finish".	
4. Create a file called netci.groovy in root of your repo in the target branch (this could also be named something different based on the line in the repolist.txt file).
5. Write your CI definition
  * [Pipeline jobs (new)](CI-PIPELINES.md)
  * [Classic jobs](CI-CLASSIC.md)
6. PR the netci.groovy file, /cc @dotnet/dnceng for review and comment "test ci please" to the PR thread.
7. Once the test generation completes, you may examine the jobs for correctness by clicking on the Details link of the job.

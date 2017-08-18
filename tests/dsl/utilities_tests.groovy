// Utilities tests
// Places here because there is some issue (related to https://issues.jenkins-ci.org/browse/JENKINS-42730) which causes odd issues when
// things are imported from vars as well as a dynamically loaded library

import jobs.generation.Utilities

// With collection == devdiv, we add "DefaultColleciton" like in most servers
assert Utilities.calculateVSTSGitURL('devdiv', 'foo/bar') == 'https://devdiv.visualstudio.com/DefaultCollection/foo/_git/bar' : "Incorrect url for devdiv collection git URL"

// With collection == devdiv, we add "DefaultColleciton" like in most servers
assert Utilities.calculateVSTSGitURL('other', 'foo/bar') == 'https://other.visualstudio.com/foo/_git/bar' : "Incorrect url for non-devdiv collection git URL"

// With collection == devdiv, we add "DefaultColleciton" like in most servers
assert Utilities.calculateGitHubURL('foo/bar') == 'https://github.com/foo/bar' : "Incorrect url for github URL"
package org.dotnet.ci.util;

// Contains functionality to deal with agents.
class Agents {
    // Retrieves the machine affinity for a build that needs docker
    // Parameters:
    //  version - Version to use.  Typically either latest or an alias corresponding to the target product
    // Returns:
    //  Label for the VM to use
    static String getDockerAgentLabel(String version) {
        switch (version) {
            // Latest version
            case 'latest':
                return getAgentLabel('Ubuntu16.04', 'latest-docker')
                break
            
            // Current version in use for netcore 2.0
            case 'netcore2.0':
                return getAgentLabel('Ubuntu16.04', '20170216')
                break

            default:
                assert false : "Version ${version} not recognized"
        }
    }

    // Given the name of an OS and image version, get the label that
    // this task would run on
    //
    // Parameters:
    //  job: Job to set affinity for
    //  osName: Name of OS to to run on.
    //  version: Version of the image
    // Returns:
    //  String representing the label that the task should target
    static String getAgentLabel(String osName, String version) {
        if (osName == 'Ubuntu') {
            osName = 'Ubuntu14.04'
        }
        // Special case OSX.  We did not use to have
        // an OS version.  Current OSX job run against 10.11
        if (osName == 'OSX') {
            osName = 'OSX10.11'
        }

        // Move off of "latest" by simply removing the 'or-auto' bit
        version = version.replace('-or-auto', '')

        def machineMap    = [
                            'Ubuntu14.04' :
                                [
                                // Specific image label
                                '201626':'ubuntu1404-201626',
                                // Contains an updated version of mono
                                '20160211':'ubuntu1404-20160211.1',
                                // Contains npm, njs, nvm
                                '20161020':'ubuntu1404-20161020',
                                // Contains 20160211-1 + gdb + mono 4.6.2.16
                                '20170109':'ubuntu1404-20170109',
                                // Contains 20160211-1 + clang 3.9
                                '20170118':'ubuntu1404-20170118',
                                // Contains the rootfs setup for arm builds.
                                '20170821':'ubuntu1404-20170821',
                                // 20170821 + clang 3.9
                                '20170925':'ubuntu1404-20170925',
                                // Contains Mono 5.0.1
                                'arm-cross-latest':'ubuntu1404-20170120',
                                // Contains the rootfs setup for arm64 builds.
                                'arm64-cross-latest':'ubuntu1604-20170526',
                                // Pool of arm64 lab machines 4k page size
                                'arm64-small-page-size':'arm64_ubuntu',
                                // Pool of arm64 lab machines 64k page size
                                'arm64-huge-page-size':'arm64_ubuntu_huge_pages',
                                // Image installing the latest mono-devel
                                'latest-mono-devel':'ubuntu1404-20160211-1-latest-mono',
                                // Latest auto image.
                                'latest':'ubuntu1404-20170925',
                                // For outerloop runs.
                                'outer-latest':'ubuntu1404-20160206-outer',
                                // For internal Ubuntu runs
                                'latest-internal':'ubuntu1404-20160211.1-internal'
                                ],
                            'Ubuntu16.04' :
                                [
                                // 20170526 + clang 3.9
                                '20170925':'ubuntu1604-20170925',
                                // Contains the rootfs setup for arm64 builds.
                                'arm64-cross-latest':'ubuntu1604-20170925',
                                // Contains ubuntu1604-20160803 + gdb + mono 4.6.2.16
                                '20170109':'ubuntu1604-20170109',
                                //Contains Mono 5.0.1
                                '20170731':'ubuntu1604-20170731',
                                // 20170731 + clang 3.9
                                '20170925-1':'ubuntu1604-20170925-1',
                                // Latest auto image.
                                'latest':'ubuntu1604-20170925-1',
                                // ubuntu1604-20160510 + docker.
                                // Move this to latest after validation
                                'latest-docker':'ubuntu1604-20170216',
                                // For outerloop runs.
                                'outer-latest':'ubuntu1604-20170216-outer',
                                // For outerloop runs, using Linux kernel version 4.6.2
                                'outer-linux462': 'ubuntu1604-20160510-20160715outer'
                                ],
                            'Ubuntu16.10' :
                                [
                                // 20170216 + clang 3.9
                                '20170925':'ubuntu1610-20170925',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest':'ubuntu1610-20170925',
                                // For outerloop runs.
                                'outer-latest':'ubuntu1610-20170216-outer',
                                ],
                            // El Capitan
                            'OSX10.11' :
                                [
                                // Latest auto image.
                                'latest':'osx-10.11',
                                // For elevated runs
                                'latest-elevated':'osx-10.11-elevated'
                                ],
                            // Sierra
                            'OSX10.12' :
                                [
                                // Latest auto image.
                                'latest':'osx-10.12 || OSX.1012.Amd64.Open',
                                // For elevated runs
                                'latest-elevated':'osx-10.12-elevated'
                                ],
                            // High Sierra
                            'OSX10.13' :
                                [
                                // Latest auto image.
                                'latest':'osx-10.13',
                                // For elevated runs
                                'latest-elevated':'osx-10.13-elevated'
                                ],
                            // This is Windows Server 2012 R2
                            'Windows_NT' :
                                [
                                // Older images.  VS update 1
                                '20160325' : 'win2012-20160325',
                                // Older images.  VS update 1
                                '20160325-elevated' : 'win2012-20160325-elevated',
                                // Older images.  VS update 3
                                '20160627' : 'win2012-20160627',
                                // Older images.  VS update 3
                                '20160627-elevated' : 'win2012-20160627-elevated',
                                // win2012-20160824 + .NET 4.6.2
                                '20161027' : 'win2012-20161027',
                                // win2016-20170303 + Python 3.2
                                '20170427' : 'win2012-20170809',
                                // win2016-20170303 + Python 3.2
                                '20170427-elevated' : 'win2012-20170809-elevated',
                                // the generic unversioned label except for special cases.
                                // Now contains VS2017
                                'latest':'win2012-20171003',
                                // Win2012.R2 + VS2013.5 + VS2015.3 + VS15.P3
                                'latest-dev15':'win2012-20160707',
                                // Win2012.R2 + VS2013.5 + VS2015.3 + VS15.P4
                                'latest-dev15-preview4':'win2012-20160912',
                                // Win2016 + VS15.P5
                                'latest-dev15-preview5':'win2016-20161013-1',
                                // Win2016 + VS15.RC2
                                'latest-dev15-rc2':'win2016-20170105',
                                // Win2016 + VS15.RC4
                                'latest-dev15-rc':'win2016-20170214',
                                // Win2016 + VS15.0
                                'latest-dev15-0':'win2016-20170712',
                                // For internal runs - Win2016 + VS15.1
                                'latest-dev15-1':'win2016-20170427',
                                // Win2016 + VS15.3 Preview1
                                'latest-dev15-3':'win2016-20170507',
                                // Dev15 image
                                'latest-dev15':'win2012-20160506',
                                // For internal runs
                                'latest-internal':'win2012-20160707-internal',
                                // For internal runs - Win2016 + VS15.RC2
                                'latest-dev15-rc2-internal':'win2016-20170105-internal',
                                // For internal runs - Win2016 + VS15.RC4
                                'latest-dev15-internal':'win2016-20170214-internal',
                                // For internal runs - Win2016 + VS15.0
                                'latest-dev15-0-internal':'win2016-20170712-internal',
                                // For internal runs - Win2016 + VS15.1
                                'latest-dev15-1-internal':'win2016-20170427-internal',
                                // win2016-base + d15prerel-26423.1
                                'latest-d15prerel' : 'win2016-20170427-1',
                                // win2016-base + d15prerel-26423.1
                                'latest-d15prerel-internal' : 'win2016-20170531-internal',
                                // For internal runs which don't need/want the static 'windows-internal' pool
                                'latest-dev15-internal':'win2012-20160707-internal',
                                // win2016-base + Dev15.3 preview 1
                                'latest-dev15-3-preview1' : 'win2016-20170510',
                                // win2016-base + Dev15.3 preview 1
                                'latest-dev15-3-preview1-internal' : 'win2016-20170510-internal',
                                // win2016-base + Dev15.3 preview 2
                                'latest-dev15-3-preview2' : 'win2016-20170613',
                                // win2016-base + Dev15.3 preview 2
                                'latest-dev15-3-preview2-internal' : 'win2016-20170613-internal',
                                // win2016-base + Dev15.3 preview 4
                                'latest-dev15-3-preview4' : 'win2016-20170717',
                                // win2016-base + Dev15.3 preview 4
                                'latest-dev15-3-preview4-internal' : 'win2016-20170717-internal',
                                // win2016-base + Dev15.3 preview 6
                                'latest-dev15-3-preview6' : 'win2016-20170731',
                                // win2016-base + Dev15.3 preview 6
                                'latest-dev15-3-preview6-internal' : 'win2016-20170731-internal',
                                // win2016-base + Dev15.3 preview 7
                                'latest-dev15-3-preview7' : 'win2016-20170802',
                                // win2016-base + Dev15.3 preview 7
                                'latest-dev15-3-preview7-internal' : 'win2016-20170802-internal',
                                // win2016-base + Dev15.3.4
                                'latest-dev15-3' : 'win2016-20170919',
                                // win2016-base + Dev15.3.4
                                'latest-dev15-3-internal' : 'win2016-20170919-internal',
                                // For elevated runs
                                'latest-elevated':'win2012-20171003-elevated',
                                // For arm64 builds
                                'latest-arm64':'win2012-20170810',
                                // For perf runs
                                'latest-perf':'windows-perf-internal',
                                // Win2016
                                'win2016-base': 'win2016-base',
                                // Win2016
                                'win2016-base-internal': 'win2016-base-internal'
                                ],
                            'Windows_2016' :
                                [
                                // First working containers image
                                'win2016-20161018-1':'win2016-20161018-1',
                                // Docker 17.06.1-ee-2
                                'win2016-20170921':'win2016-20170921',
                                'latest-docker':'win2016-20170921',
                                // Latest auto image w/docker (move to latest when possible)
                                'latest-containers':'win2016-20161018-1',
                                // Latest auto image.
                                'latest':'win2016-20160223'
                                ],
                            'Windows 10' :
                                [
                                // Latest auto image.
                                'latest':'win2016-20170303'
                                ],
                            'Windows 7' :
                                [
                                '20161104':'win2008-20170303',
                                // Latest auto image.
                                'latest':'win2008-20170303'
                                ],
                            'RHEL7.2' :
                                [
                                // 20170525 + clang 3.9
                                '20170928':'rhel72-20170928',
                                // 20170928 + libclang-3.9-dev
                                '20171003':'rhel72-20171003',
                                // Latest auto image.
                                'latest':'rhel72-20171003',
                                // For outerloop runs.
                                'outer-latest':'rhel72-20160412-1-outer'
                                ],
                            'CentOS7.1' :
                                [
                                // centos71-20170216 + clang 3.9
                                '20170926':'centos71-20170926',
                                // centos71-20170926 + libclang-3.9-dev
                                '20171005':'centos71-20171005',
                                // Latest auto image.
                                'latest':'centos71-20171005',
                                // For outerloop runs.
                                'outer-latest':'centos71-20171005-outer',
                                ],
                            'Debian8.2' :
                                [
                                '20160323':'deb82-20160323',
                                // Latest auto image.
                                'latest':'deb82-20160323'
                                ],
                            'Debian8.4' :
                                [
                                // 20170214 + clang 3.9
                                '20170925':'deb84-20170925',
                                // Latest auto image.
                                'latest':'deb84-20170925',
                                // For outerloop runs
                                'outer-latest':'deb84-20170925-outer'
                                ],
                            'Fedora24' :
                                [
                                // fedora24-20170420 + clang 3.9
                                '20170926':'fedora24-20170926',
                                // fedora24-20170926 + libclang-3.9-dev
                                '20171005':'fedora24-20171005',
                                // Latest auto image.
                                'latest':'fedora24-20171005',
                                // For outerloop runs
                                'outer-latest':'fedora24-20171005-outer'
                                ],
                            'Tizen' :
                                [
                                // Use ubuntu14.04 images
                                // Contains the rootfs setup for arm/arm64 builds.  Move this label forward
                                // till we have the working build/test, then apply to everything.
                                'arm-cross-latest':'ubuntu1404-20170120',
                                // Latest auto image.
                                'latest':'ubuntu1404-20170120',
                                ],
                                // Some nodes don't have git, which is what is required for the
                                // generators.
                            'Generators' :
                                [
                                'latest':'!windowsnano16 && !performance && !dtap'
                                ]
                            ]
        def versionLabelMap = machineMap.get(osName, null)
        assert versionLabelMap != null : "Could not find os ${osName}"
        def machineLabel = versionLabelMap.get(version, null)
        assert machineLabel != null : "Could not find version ${version} of ${osName}"
        
        return machineLabel
    }
}

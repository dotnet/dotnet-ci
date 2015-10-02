# Configuring your repo for Jenkins CI

This document contains information on how to set up Jenkins CI for your repo.

## Overview

The CI system located at http://dotnet-ci.cloudapp.net serves a large number of projects in the .NET Foundation and Microsoft.  It is a fairly standard Jenkins instance running in Azure with a variety of execution nodes including:
  * Windows Server 2012 2
  * Ubunutu 14.04
  * Centos 7.1
  * OpenSUSE 13.2
  * Mac OSX
  * FreeBSD

### What can Jenkins do?

In the most basic sense, Jenkins is a task scheduling and management system.  Given a set of jobs that need to be run based on certain triggers (e.g. GitHub pushes or pull requests), run those jobs on a specific pool of machines.  While tons of those sort of systems have been created over the years, the great thing about Jenkins is its flexibility and plugin model.  There are thousands of plugins for Jenkins which automate tasks that we would normally need to deal with ourselves.
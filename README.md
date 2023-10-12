# TAP Plug-in

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/tap.svg)](https://plugins.jenkins.io/tap)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/tap.svg?label=changelog)](https://github.com/jenkinsci/tap-plugin/blob/master/CHANGES.md)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/tap.svg?color=blue)](https://plugins.jenkins.io/tap)
[![Jenkins](https://ci.jenkins.io/job/Plugins/job/tap-plugin/job/master/badge/icon?subject=Jenkins%20CI)](https://ci.jenkins.io/job/Plugins/job/tap-plugin/job/master/)
[![JIRA issues](https://img.shields.io/static/v1?label=Issue%20tracking&message=component:%20tap-plugin&color=blue)](https://issues.jenkins.io/browse/JENKINS-64962?jql=component%20%3D%20%27tap-plugin%27%20AND%20resolution%20IS%20EMPTY%20ORDER%20BY%20updated%20DESC)

## Overview

This plug-in adds support to [TAP](https://testanything.org/) test result files to Jenkins.
It lets you specify an ant-like pattern for a directory that contains your TAP files and
scans and creates views for your test results in Jenkins.

TAP Plug-in depends on [tap4j](https://github.com/tupilabs/tap4j) - a TAP implementation
for Java, and on the [Jenkins JUnit Plug-in](https://plugins.jenkins.io/junit/).

> NOTE: You may get errors if the JUnit Plug-in is not active in your Jenkins instance
(see JENKINS-27227 for more).
gocd-s3-artifacts
=================

[![Build Status](https://travis-ci.org/indix/gocd-s3-artifacts.svg?branch=master)](https://travis-ci.org/indix/gocd-s3-artifacts)

<p align="center">
<img src="docs/resources/images/banner.png" width="750" height="200"/>
</p>

Set of plugins to publish, poll and fetch artifacts from Amazon S3

This repo holds two task plugins and a package poller plugin.

- indix.s3publish - Task plugin to push artifacts to S3
- indix.s3fetch - Task plugin to fetch artifacts from S3
- indix.s3material - Package poller plugin to poll for new artifacts in S3.

Documentation
-----

Detailed documentation is available here - http://oss.indix.com/gocd-s3-artifacts/

Build
-----

To build all the plugins, do:

```bash
./build.sh
```

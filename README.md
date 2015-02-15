gocd-s3-artifacts
=================

[![Build Status](https://snap-ci.com/ind9/gocd-s3-artifacts/branch/master/build_image)](https://snap-ci.com/ind9/gocd-s3-artifacts/branch/master)

Set of plugins to publish, poll and fetch artifacts from Amazon S3

This repo holds two task plugins and a package poller plugin.

- indix.s3publish - Task plugin to push artifacts to S3
- indix.s3fetch - Task plugin to fetch artifacts from S3
- indix.s3material - Package poller plugin to poll for new artifacts in S3.

Documentation
-----

Detailed documentation is available here - http://ind9.github.io/gocd-s3-artifacts

Build
-----

To build all the plugins, do:

```bash
sbt clean assembly
```

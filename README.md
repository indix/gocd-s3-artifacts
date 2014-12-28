gocd-s3-artifacts
=================

Set of plugins to publish, poll and fetch artifacts from Amazon S3

This repo holds two task plugins and a package poller plugin.

indix.s3publish - Task plugin to push artifacts to S3
indix.s3fetch - Task plugin to fetch artifacts from S3
indix.s3material - Package poller plugin to poll for new artifacts in S3.

Build
-----

To build all the plugins, do:

```bash
sbt clean assembly
```

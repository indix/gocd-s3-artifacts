GoCD S3 Artifacts
===

This is a collection of necessary GoCD plugins to enable publishing, polling and fetching artifacts from Amazon S3.

We built these at Indix due to our need to share artifacts across many Go servers

There are two task plugins and one material plugin that enable artifacts on S3:

1. **indix.s3publish** - Task plugin to push artifacts to S3.

2. **indix.s3fetch** - Task plugin to fetch artifacts from S3.

3. **indix.s3material** - Package poller plugin to poll for new artifacts in S3.

While the plugins could be used independently of each other, they work best together as a collection of plugins.

If you are interested in a multi-server setup, note that it is not be necessary to install all the three plugins to all the Go servers. If a Go server is going to be only a source of artifacts, only the publish plugin needs to be installed on it. If a Go server is going to need artifacts from other servers, then only the fetch and material plugins are need on it.
Required environment variables
===

The task plugins `Fetch` and `Publish` run on the agents. The `Material` plugin runs on the server.

## Server

The `Material` plugin needs AWS credentials to poll S3 for artifacts. These can be provided through the environment variables `AWS_SECRET_ACCESS_KEY` and  `AWS_ACCESS_KEY_ID`. The credentials are also picked up from the alternate locations that the [AWS Java SDK looks at.][1]

## Agent

Both the `Fetch` and `Task` plugins require the following environment variables to work. These can be set as environment variables on the agents, pipeline or even Go's Environment. The recommended approach is to set these on the agents.

`AWS_SECRET_ACCESS_KEY` and  `AWS_ACCESS_KEY_ID` are the required AWS credentials to upload and download artifacts from S3.

`GO_ARTIFACTS_S3_BUCKET` is the name of the S3 bucket where artifacts will be uploaded. Artifacts are uploaded to the path `s3://<GO_ARTIFACTS_S3_BUCKET>/<PIPELINE_NAME>/<STAGE_NAME>/<JOB_NAME>/<PIPELINE_COUNTER>.<STAGE_COUNTER>` as the root path.

In addition to the above, the `Publish` plugin also requires `GO_SERVER_DASHBOARD_URL` environment variable. `GO_SERVER_DASHBOARD_URL` is the url of the Go server web UI (dashboard). This is needed to reliably determine the dashboard url so that we can provide a trackback url to the source of the artifacts.

[1]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3Client.html#AmazonS3Client()

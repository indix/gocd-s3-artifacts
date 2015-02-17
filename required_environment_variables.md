Required environment variables
===

All the three plugins require the following environment variables to work.

These can be set as environment variables on the agents, pipeline or even Go's Environment. The recommended approach is to set these on the agents.

`AWS_SECRET_ACCESS_KEY` and  `AWS_ACCESS_KEY_ID` are the required AWS credentials to upload and download artifacts from S3.

`GO_ARTIFACTS_S3_BUCKET` is the name of the S3 bucket where artifacts will be uploaded. Artifacts are uploaded to the path 
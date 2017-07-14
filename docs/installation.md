# Installation

Installing the plugin(s) is same as any other Go plugins. Please follow the [GoCD documentation on installing plugins.](https://docs.gocd.org/current/extension_points/plugin_user_guide.html)

Builds of the plugins are available through [Github releases](https://github.com/indix/gocd-s3-artifacts/releases)

## AWS credentials

The plugins use the [default credential provider chain used by the AWS SDK](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html)

The `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` environment variables can also be set from pipelines / GoCD environments, and if set those will be used to get the credentials.

If `AWS_USE_IAM_ROLE` is set to true, the instance profile credentials are used.

`AWS_REGION` environment variable will have to be set in pipeline / GoCD environment / agent / server if not already set.

package com.indix.gocd.s3publish

import java.util.{Map => JMap}

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult
import com.thoughtworks.go.plugin.api.task.{TaskConfig, TaskExecutionContext, TaskExecutor}
import material.store.S3ArtifactStore

class PublishExecutor extends TaskExecutor {

  override def execute(config: TaskConfig, context: TaskExecutionContext): ExecutionResult = {
    val environment = context.environment().asMap()
    if (!checkForAccessKeyId(environment)) return ExecutionResult.failure("AWS_ACCESS_KEY_ID environment variable not present")
    if (!checkForSecretKey(environment)) return ExecutionResult.failure("AWS_SECRET_ACCESS_KEY environment variable not present")

    context.console().printEnvironment(environment, context.environment().secureEnvSpecifier())

    val bucket = config.getValue(PublishTask.BUCKET_NAME)
    val source = config.getValue(PublishTask.SOURCE)
    val store = S3ArtifactStore(s3Client(environment), bucket)
    val filePathOnS3 = destinationOnS3(environment, source)
    store.put(s"${context.workingDir()}/$source", filePathOnS3)
    val finalPathOnS3 = s"s3://$bucket/$filePathOnS3"
    context.console().printLine(s"Pushed $source to $finalPathOnS3")
    ExecutionResult.success(s"Pushed $source to $finalPathOnS3")
  }

  private def destinationOnS3(environment: JMap[String, String], source: String) = {
    val pipeline = environment.get("GO_PIPELINE_NAME")
    val pipelineCounter = environment.get("GO_PIPELINE_COUNTER")
    val stageName = environment.get("GO_STAGE_NAME")
    val stageCounter = environment.get("GO_STAGE_COUNTER")

    // FIXME - May be make this template configurable?
    s"${pipeline}_$stageName/${pipelineCounter}_$stageCounter/$source"
  }

  private def s3Client(environment: JMap[String, String]) = {
    val secretKey = environment.get(PublishTask.AWS_SECRET_ACCESS_KEY)
    val accessKey = environment.get(PublishTask.AWS_ACCESS_KEY_ID)
    val credentials = new BasicAWSCredentials(accessKey, secretKey)
    new AmazonS3Client(credentials)
  }

  private def checkForAccessKeyId(environment: JMap[String, String]) = environment.containsKey(PublishTask.AWS_ACCESS_KEY_ID)

  private def checkForSecretKey(environment: JMap[String, String]) = environment.containsKey(PublishTask.AWS_SECRET_ACCESS_KEY)
}

package com.indix.gocd.s3publish

import com.thoughtworks.go.plugin.api.annotation.Extension
import com.thoughtworks.go.plugin.api.response.validation.{ValidationError, ValidationResult}
import com.thoughtworks.go.plugin.api.task.{Task, TaskConfig, TaskExecutor, TaskView}
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils

@Extension
class PublishTask extends Task {

  import com.indix.gocd.s3publish.PublishTask._

  override def config(): TaskConfig = {
    val config = new TaskConfig
    config.addProperty(BUCKET_NAME)
    config.addProperty(SOURCE)
    config
  }

  override def executor(): TaskExecutor = new PublishExecutor

  override def view(): TaskView = new TaskView {
    override def displayValue(): String = "Publish to S3"

    override def template(): String = IOUtils.toString(getClass.getResourceAsStream("/views/task.template.html"), "UTF-8")
  }

  override def validate(taskConfig: TaskConfig): ValidationResult = {
    // TODO - We can't do a validation from ENV variable here.
    val result = new ValidationResult
    if (StringUtils.isEmpty(taskConfig.getValue(BUCKET_NAME))) {
      result.addError(new ValidationError(BUCKET_NAME, "S3 Bucket name not present"))
    }

    if (StringUtils.isEmpty(taskConfig.getValue(SOURCE))) {
      result.addError(new ValidationError(SOURCE, "Source files to publish not present"))
    }

    result
  }
}

object PublishTask {
  val BUCKET_NAME = "BUCKET"
  val SOURCE = "SOURCE"

  /* We should be getting these from the environment variables */
  val AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY"
  val AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID"
  val FS_SCHEME = "FS_SCHEME"
}

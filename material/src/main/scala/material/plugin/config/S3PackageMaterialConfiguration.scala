package material.plugin.config

import com.thoughtworks.go.plugin.api.material.packagerepository._
import com.thoughtworks.go.plugin.api.response.Result
import com.thoughtworks.go.plugin.api.response.validation.{ValidationError, ValidationResult}
import com.thoughtworks.go.plugin.api.config.Property

import scala.collection.JavaConverters._
import org.apache.commons.lang3.StringUtils
import material.plugin.S3PackageMaterialPoller

case class Config(name: String, displayName: String, order: Int) {
  def toPackageProperty = new PackageMaterialProperty(name)
    .`with`[String](Property.DISPLAY_NAME, displayName)
    .`with`[Integer](Property.DISPLAY_ORDER, order)

  def toProperty = new Property(name)
    .`with`[String](Property.DISPLAY_NAME, displayName)
    .`with`[Integer](Property.DISPLAY_ORDER, order)
}



object S3PackageMaterialConfiguration {
  val S3_BUCKET = "S3_BUCKET"
  val S3_ACCESS_KEY_ID = "S3_AWS_ACCESS_KEY_ID"
  val S3_SECRET_ACCESS_KEY = "S3_AWS_SECRET_ACCESS_KEY"
  val ARTIFACT_NAME = "ARTIFACT_NAME"
  val repoConfigs = List(
    Config(S3_BUCKET, "S3 Bucket", 0),
    Config(S3_ACCESS_KEY_ID, "S3 Access Key ID", 1),
    Config(S3_SECRET_ACCESS_KEY, "S3 Secret Access Key", 2)
  )
  
  val packageConfigs = List(
    Config(ARTIFACT_NAME, "Artifact Name", 0)
  )
}

class S3PackageMaterialConfiguration extends PackageMaterialConfiguration {
  override def getRepositoryConfiguration: RepositoryConfiguration = {
    val repoConfig = new RepositoryConfiguration()
    S3PackageMaterialConfiguration.repoConfigs.map(_.toPackageProperty).foreach(p => repoConfig.add(p))
    repoConfig
  }

  def validate(repoConfig: RepositoryConfiguration, property: String, message: String) = {
    if(repoConfig.get(property) == null || StringUtils.isBlank(repoConfig.get(property).getValue)) {
      val validationResult = new ValidationResult()
      validationResult.addError(new ValidationError(property, message))
      Some(validationResult)
    }else{
      None
    }
  }

  override def isRepositoryConfigurationValid(repoConfig: RepositoryConfiguration): ValidationResult = {
    val errors = List(validate(repoConfig, S3PackageMaterialConfiguration.S3_BUCKET, s"${S3PackageMaterialConfiguration.S3_BUCKET} configuration is missing or value is empty"),
      validate(repoConfig, S3PackageMaterialConfiguration.S3_ACCESS_KEY_ID, s"${S3PackageMaterialConfiguration.S3_ACCESS_KEY_ID} configuration is missing or value is empty"),
      validate(repoConfig, S3PackageMaterialConfiguration.S3_SECRET_ACCESS_KEY, s"${S3PackageMaterialConfiguration.S3_SECRET_ACCESS_KEY} configuration is missing or value is empty")
    ).flatMap(vr => vr.map(_.getErrors.asScala).getOrElse(List[ValidationError]()))

    val validationResult = new ValidationResult()
    validationResult.addErrors(errors.asJava)
    validationResult
  }

  override def isPackageConfigurationValid(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration): ValidationResult = {
    val errors = List(
      validate(repoConfig, S3PackageMaterialConfiguration.ARTIFACT_NAME, s"${S3PackageMaterialConfiguration.ARTIFACT_NAME} configuration is missing or value is empty")
    ).flatMap(vr => vr.map(_.getErrors.asScala).getOrElse(List[ValidationError]()))
    val validationResult = new ValidationResult()
    validationResult.addErrors(errors.asJava)
    validationResult
  }

  override def getPackageConfiguration: PackageConfiguration = {
    val packageConfig = new PackageConfiguration()
    S3PackageMaterialConfiguration.packageConfigs.map(_.toPackageProperty).foreach(p => packageConfig.add(p))
    packageConfig
  }
}
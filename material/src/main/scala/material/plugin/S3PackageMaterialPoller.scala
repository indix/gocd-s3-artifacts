package material.plugin

import com.thoughtworks.go.plugin.api.material.packagerepository._
import com.thoughtworks.go.plugin.api.response.Result
import material.plugin.config.S3PackageMaterialConfiguration
import scala.collection.JavaConverters._
import material.store.PluginConfigToHadoopConfig
import org.apache.hadoop.fs.{Path, FileSystem}
import org.apache.hadoop.conf.Configuration


class S3PackageMaterialPoller extends PackageMaterialPoller {
  def checkConnection(conf: Configuration, path: String) = {
    val pathExist = FileSystem.get(conf).exists(new Path(path))
    if (pathExist)
      new Result().withSuccessMessages(s"Connection to $path is OK.")
    else
      new Result().withSuccessMessages(s"Connection to $path did not succeed.")
  }

  override def getLatestRevision(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration): PackageRevision = ???

  override def checkConnectionToPackage(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration): Result = {
    val s3Bucket = repoConfig.get(S3PackageMaterialConfiguration.S3_BUCKET).getValue
    val artifactDir = repoConfig.get(S3PackageMaterialConfiguration.ARTIFACT_NAME).getValue
    val properties = repoConfig.list().asScala.map(_.asInstanceOf[PackageMaterialProperty])
    val config = PluginConfigToHadoopConfig.updateConfig(properties)
    checkConnection(config, s"$s3Bucket/$artifactDir")
  }

  override def checkConnectionToRepository(repoConfig: RepositoryConfiguration): Result = {
    val s3Bucket = repoConfig.get(S3PackageMaterialConfiguration.S3_BUCKET).getValue
    val properties = repoConfig.list().asScala.map(_.asInstanceOf[PackageMaterialProperty])
    val config = PluginConfigToHadoopConfig.updateConfig(properties)
    checkConnection(config, s3Bucket)
  }

  override def latestModificationSince(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration, revision: PackageRevision): PackageRevision = ???
}

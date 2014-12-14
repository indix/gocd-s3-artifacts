package material.plugin

import com.thoughtworks.go.plugin.api.material.packagerepository._
import com.thoughtworks.go.plugin.api.response.Result
import material.plugin.config.S3PackageMaterialConfiguration
import scala.collection.JavaConverters._
import material.store._
import org.apache.hadoop.fs.{Path, FileSystem}
import org.apache.hadoop.conf.Configuration
import com.amazonaws.services.s3.AmazonS3Client
import material.store.Exists
import material.store.S3ArtifactStoreWithS3Client
import java.util.Date


class S3PackageMaterialPoller extends PackageMaterialPoller {
  val USER = "go"
  override def getLatestRevision(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration): PackageRevision = {
    val s3Bucket = repoConfig.get(S3PackageMaterialConfiguration.S3_BUCKET).getValue
    val artifactName = repoConfig.get(S3PackageMaterialConfiguration.ARTIFACT_NAME).getValue
    val artifactStore = S3ArtifactStoreWithS3Client(new AmazonS3Client(), s3Bucket)
    val revision = artifactStore.latest(artifactName)
    revision match {
      case x: RevisionSuccess => new PackageRevision(x.revision.revision, x.lastModified, USER, x.revisionComments, x.trackBackUrl)
      case f : OperationFailure => throw new RuntimeException(f.th)
     }
  }

  override def checkConnectionToPackage(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration): Result = {
    val s3Bucket = repoConfig.get(S3PackageMaterialConfiguration.S3_BUCKET).getValue
    val artifactName = repoConfig.get(S3PackageMaterialConfiguration.ARTIFACT_NAME).getValue
    val artifactStore = S3ArtifactStoreWithS3Client(new AmazonS3Client(), s3Bucket)
    artifactStore.exists(artifactName) match {
      case e: Exists => new Result().withSuccessMessages(s"Check ${artifactName} exists ${e.message}")
      case f: OperationFailure => new Result().withErrorMessages(f.message)
    }
  }

  override def checkConnectionToRepository(repoConfig: RepositoryConfiguration): Result = {
    val s3Bucket = repoConfig.get(S3PackageMaterialConfiguration.S3_BUCKET).getValue
    val artifactStore = S3ArtifactStoreWithS3Client(new AmazonS3Client(), s3Bucket)
    artifactStore.bucketExists match {
      case e: Exists => new Result().withSuccessMessages(s"Check [${s3Bucket}] exists ${e.message}")
      case f: OperationFailure => new Result().withErrorMessages(f.message)
    }
  }

  override def latestModificationSince(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration, revision: PackageRevision): PackageRevision = {
    // S3 doesn't seem to provide APIs to pull pegged updates
    // This means, we need to do a getLatest for this artifact anyways
    // Finally check to see if the latest revision is newer than the incoming revision
    // and return PackageRevision instance appropriately.
    val packageRevision = getLatestRevision(packageConfig, repoConfig)
    if(Revision(packageRevision.getRevision).compare(Revision(revision.getRevision)) > 0)
      packageRevision
    else
      revision
  }
}

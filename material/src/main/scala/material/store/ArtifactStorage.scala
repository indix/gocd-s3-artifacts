package material.store

import org.apache.hadoop.conf.Configuration
import com.amazonaws.services.s3.AmazonS3Client

case class Revision(revision: String) extends Ordered[Revision] {
  val parts = revision.split('.').map(_.toInt)
  val major = parts(0)
  val minor = parts(1)
  val patch = if (parts.length == 3) parts(2) else 0

  import Ordered.orderingToOrdered
  def compare(that: Revision) = (this.major, this.minor, this.patch) compare (that.major, that.minor, that.patch)
}


sealed trait ArtifactStore {
  def get(from: String, to: String): FSOperationStatus
  def put(from: String, to: String): FSOperationStatus
  def latest(artifactName: String): FSOperationStatus
  def artifactsSince(artifactName: String, revision: Revision): FSOperationStatus
  def exists(key: String): FSOperationStatus
  def bucketExists: FSOperationStatus
}

case class S3ArtifactStoreWithS3Client(s3Client: AmazonS3Client, bucket: String) extends ArtifactStore with StoreHelper {
  override def get(from: String, to: String) = copyToLocal(bucket, from, to, s3Client)
  override def put(from: String, to: String) = copyFromLocal(bucket, from, to, s3Client)
  override def latest(artifactName: String): FSOperationStatus = getLatest(artifactName, bucket, s3Client)
  override def artifactsSince(artifactName: String, revision: Revision): FSOperationStatus = ???
  override def exists(key: String): FSOperationStatus = exists(bucket, key, s3Client)
  override def bucketExists: FSOperationStatus = bucketExists(bucket, s3Client)
}

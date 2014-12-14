package material.store

import org.apache.hadoop.conf.Configuration
import com.amazonaws.services.s3.AmazonS3Client


sealed trait ArtifactStore {
  def get(from: String, to: String): FSOperationStatus
  def put(from: String, to: String): FSOperationStatus
  def latest(materialName: String): String
  def deltaAfter(lastUpdated: Long): List[String]
}

case class S3ArtifactStore(conf: Configuration = new Configuration()) extends ArtifactStore with StoreHelper {
  override def get(from: String, to: String) = ???
  override def put(from: String, to: String) = ???
  override def latest(materialName: String): String = ???
  override def deltaAfter(lastUpdated: Long): List[String] = ???
}

case class S3ArtifactStoreWithS3Client(s3Client: AmazonS3Client, bucket: String) extends ArtifactStore with StoreHelper {
  override def get(from: String, to: String) = copyToLocal(bucket, from, to, s3Client)
  override def put(from: String, to: String) = copyFromLocal(bucket, from, to, s3Client)
  override def latest(materialName: String): String = ???
  override def deltaAfter(lastUpdated: Long): List[String] = ???
}

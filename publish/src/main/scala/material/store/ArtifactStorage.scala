package material.store

import java.io.File

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest


sealed trait ArtifactStore {
  def get(from: String, to: String): Unit
  def put(from: String, to: String): Unit
  def latest(materialName: String): String
  def deltaAfter(lastUpdated: Long): List[String]
}

case class S3ArtifactStore(client: AmazonS3Client, bucket: String) extends ArtifactStore {
  override def get(from: String, to: String) { client.getObject(new GetObjectRequest(bucket, from), new File(to)) }
  // TODO - Add support for pushing entire folders to S3
  override def put(from: String, to: String) { client.putObject(bucket, to, new File(from)) }
  override def latest(materialName: String): String = ???
  override def deltaAfter(lastUpdated: Long): List[String] = ???
}

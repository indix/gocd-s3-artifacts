package material.store

import org.apache.hadoop.conf.Configuration


sealed trait ArtifactStore {
  def get(from: String, to: String): Unit
  def put(from: String, to: String): Unit
  def latest(materialName: String): String
  def deltaAfter(lastUpdated: Long): List[String]
}

case class S3ArtifactStore(conf: Configuration = new Configuration()) extends ArtifactStore with FileHelper {
  override def get(from: String, to: String) = copyToLocal(from, to, conf)
  override def put(from: String, to: String) = copyFromLocal(from, to, conf)
  override def latest(materialName: String): String = ???
  override def deltaAfter(lastUpdated: Long): List[String] = ???
}

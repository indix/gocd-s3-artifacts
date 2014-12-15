package material.store

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectListing}
import scala.util.{Failure, Success, Try}
import java.io.File
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import material.util.LoggerUtil
import scala.collection.JavaConversions._

sealed trait ArtifactStore {
  def get(from: String, to: String): FSOperationStatus
  def put(from: String, to: String): FSOperationStatus
  def latest(artifactName: String): FSOperationStatus
  def artifactsSince(artifactName: String, revision: Revision): FSOperationStatus
  def exists(key: String): FSOperationStatus
  def bucketExists: FSOperationStatus
}

case class S3ArtifactStore(s3Client: AmazonS3Client, bucket: String) extends ArtifactStore  with LoggerUtil {
  override def get(from: String, to: String) = copyToLocal(bucket, from, to, s3Client)
  override def put(from: String, to: String) = copyFromLocal(bucket, from, to, s3Client)
  override def latest(artifactName: String): FSOperationStatus = getLatest(artifactName, bucket, s3Client)
  override def artifactsSince(artifactName: String, revision: Revision): FSOperationStatus = ???
  override def exists(key: String): FSOperationStatus = exists(bucket, key, s3Client)
  override def bucketExists: FSOperationStatus = bucketExists(bucket, s3Client)

  private def mostRecentRevision(listing: ObjectListing) = {
    listing.getCommonPrefixes.asScala.map(_.split("/").last).map(Revision).max
  }

  // TODO: Make this tail recursive
  private def latestOf(client: AmazonS3Client, listing: ObjectListing) : Revision = {
    if(listing.isTruncated) {
      mostRecentRevision(listing)
    } else {
      val newListing = client.listNextBatchOfObjects(listing)
      (mostRecentRevision(newListing) :: latestOf(client, newListing) :: Nil).max
    }
  }

  private def getLatest(artifactName: String, bucket: String, client: AmazonS3Client): FSOperationStatus = {
    Try(client.listObjects(bucket, artifactName)) match {
      case Success(listing) =>
        val recent = latestOf(client, listing)
        val metadata = client.getObjectMetadata(bucket, s"$artifactName/${recent.revision}")
        val lastModified = metadata.getLastModified
        RevisionSuccess(latestOf(client, listing), lastModified, "TrackbackURL", "RevisionComment")
      case Failure(th) => OperationFailure(th)
    }
  }

  private def copyFromLocal(localFile: String, key: String, bucket: String, client: AmazonS3Client): FSOperationStatus = {
    Try(client.putObject(bucket, key, new File(localFile))) match {
      case Success(x) => CopySuccess(x.getContentMd5)
      case Failure(th) => OperationFailure(th)
    }
  }

  private def copyToLocal(key: String, bucket: String, localFilePath: String, client: AmazonS3Client): FSOperationStatus = {
    Try(client.getObject(new GetObjectRequest(bucket, key), new File(localFilePath))) match {
      case Success(x) => CopySuccess(x.getContentMD5)
      case Failure(th) => OperationFailure(th)
    }
  }

  private def createBucket(bucket: String, client: AmazonS3Client) {
    Try(client.createBucket(bucket)) match {
      case Success(x) => CreateBucketSuccess(new DateTime(x.getCreationDate.getTime), x.getOwner.getId)
      case Failure(th) => OperationFailure(th)
    }
  }

  private def exists(bucket: String, key: String, client: AmazonS3Client) = {
    Try(client.getObjectMetadata(bucket, key)) match {
      case Success(x) => if(x != null) Exists(x.getLastModified.getTime) else OperationFailure(throw new RuntimeException(s"$bucket/$key does not exist"))
      case Failure(th) => OperationFailure(th)
    }
  }

  private def bucketExists(bucket: String, client: AmazonS3Client) = {
    Try(client.listBuckets()) match {
      case Success(s) => if(s.exists(_.getName == bucket)) Exists(System.currentTimeMillis()) else OperationFailure(throw new RuntimeException(s"$bucket does not exist"))
      case Failure(th) => OperationFailure(th)
    }
  }
}

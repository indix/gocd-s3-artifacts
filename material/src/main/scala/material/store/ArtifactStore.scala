package material.store

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import scala.util.Try
import java.io.File
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import material.util.LoggerUtil
import scala.collection.JavaConversions._
import scala.util.Failure
import scala.util.Success
import java.util.Date


sealed trait ArtifactStore {
  def get(from: String, to: String): FSOperationStatus
  def put(from: String, to: String): FSOperationStatus
  def latestRevision(artifact: Artifact): FSOperationStatus
  def artifactsSince(artifactName: String, revision: Revision): FSOperationStatus
  def exists(key: String): FSOperationStatus
  def bucketExists: FSOperationStatus
}

object ResponseMetadata{
  val TRACKBACK_URL = "TRACKBACK_URL"
  val USER = "USER"
  val REVISION_COMMENT = "REVISION_COMMENT"
}

case class S3ArtifactStore(s3Client: AmazonS3Client, bucket: String) extends ArtifactStore  with LoggerUtil {
  override def get(from: String, to: String) = copyToLocal(bucket, from, to, s3Client)
  override def put(from: String, to: String) = copyFromLocal(bucket, from, to, s3Client)
  override def latestRevision(artifact: Artifact): FSOperationStatus = getLatestRevision(artifact, bucket, s3Client)
  override def artifactsSince(artifactName: String, revision: Revision): FSOperationStatus = ???
  override def exists(key: String): FSOperationStatus = exists(bucket, key, s3Client)
  override def bucketExists: FSOperationStatus = bucketExists(bucket, s3Client)

  import material.store.ResponseMetadata.{REVISION_COMMENT, TRACKBACK_URL}

  private def mostRecentRevision(listing: ObjectListing) = {
    val prefixes = listing.getCommonPrefixes.asScala
    if (prefixes.size > 0) Some(prefixes.map(_.split("/").last).map(Revision).max)
    else None
  }

  private def latestOf(client: AmazonS3Client, listing: ObjectListing) = {
    def latestOfInternal(client: AmazonS3Client, listing: ObjectListing, latestSoFar: Option[Revision]): Option[Revision] = {
      if (! listing.isTruncated){
        latestSoFar
      }else {
        val objects = client.listNextBatchOfObjects(listing)
        val recent = (latestSoFar, mostRecentRevision(objects)) match {
          case (Some(l), Some(m)) => Some(l.max(m))
          case (Some(l), None) => Some(l)
          case (None, Some(m)) => Some(m)
          case _ => None
        }
        latestOfInternal(client, objects, recent)
      }
    }
    // returning nulls is not the best way to
    latestOfInternal(client, listing, mostRecentRevision(listing))
  }

  private def getLatestRevision(artifact: Artifact, bucket: String, client: AmazonS3Client): FSOperationStatus = {
    val listObjectsRequest = new ListObjectsRequest().withBucketName(bucket).withPrefix(artifact.prefix).withDelimiter("/")
    Try(client.listObjects(listObjectsRequest)) match {
      case Success(listing) =>
        val recent = latestOf(client, listing)
        recent.map{ r =>
          val artifactWithRevision = artifact.copy(revision = recent)
//          val objectMetadataRequest = new GetObjectMetadataRequest(bucket, artifactWithRevision.withRevision)
//          val metadata = client.getObjectMetadata(objectMetadataRequest)
          RevisionSuccess(r, new Date(), TRACKBACK_URL, REVISION_COMMENT)
        }.getOrElse(OperationFailure(new RuntimeException(s"No artifacts under the specified $bucket, key prefix: ${artifact.prefix}")))
      case Failure(th) => OperationFailure(th)
    }
  }

  private def copyFromLocal(bucket: String, localFile: String, remoteFile: String, client: AmazonS3Client): FSOperationStatus = {
    Try(client.putObject(bucket, remoteFile, new File(localFile))) match {
      case Success(x) => CopySuccess(x.getContentMd5)
      case Failure(th) => OperationFailure(th)
    }
  }

  private def copyToLocal(bucket: String, remoteFile: String, localFilePath: String, client: AmazonS3Client): FSOperationStatus = {
    Try(client.getObject(new GetObjectRequest(bucket, remoteFile), new File(localFilePath))) match {
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
    val listRequest = new ListObjectsRequest().withBucketName(bucket).withPrefix(key).withDelimiter("/")
    Try(client.listObjects(listRequest)) match {
      case Success(x) if x != null && x.getCommonPrefixes.size() > 0 => Exists(x.getCommonPrefixes.size())
      case Success(x) => OperationFailure(new RuntimeException(s"No objects under the specified the bucket - $bucket, key_prefix - $key"))
      case Failure(th) => OperationFailure(th)
    }
  }

  private def bucketExists(bucket: String, client: AmazonS3Client) = {
    Try(client.listBuckets()) match {
      case Success(s) => if(s.exists(_.getName == bucket)) Exists(1) else OperationFailure(throw new RuntimeException(s"$bucket does not exist"))
      case Failure(th) => OperationFailure(th)
    }
  }
}
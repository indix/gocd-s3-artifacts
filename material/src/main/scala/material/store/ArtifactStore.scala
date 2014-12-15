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
    listing.getObjectSummaries.asScala.map(_.getKey.split("/").last).map(Revision).max
  }

  // TODO: Make this tail recursive
  private def latestOf(client: AmazonS3Client, listing: ObjectListing) : Revision = {
    def latestOfInternal(client: AmazonS3Client, listing: ObjectListing, latestSoFar: Revision): Revision = {
      if (listing.isTruncated){
        latestSoFar
      }else {
        val objects = client.listNextBatchOfObjects(listing)
        latestOfInternal(client, objects, latestSoFar.max(mostRecentRevision(objects)))
      }
    }
    latestOfInternal(client, listing, mostRecentRevision(listing))
  }

  private def getLatestRevision(artifact: Artifact, bucket: String, client: AmazonS3Client): FSOperationStatus = {
    val listObjectsRequest = new ListObjectsRequest().withBucketName(bucket).withDelimiter("/").withPrefix(artifact.prefix)
    Try(client.listObjects(listObjectsRequest)) match {
      case Success(listing) =>
        val recent = latestOf(client, listing)
        val metadata = client.getObjectMetadata(bucket, artifact.copy(revision = Some(recent)).withRevision)
        val lastModified = metadata.getLastModified
        RevisionSuccess(recent, lastModified, metadata.getUserMetadata.get(TRACKBACK_URL), metadata.getUserMetadata.get(REVISION_COMMENT))
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
    val artifactMetdata: ObjectMetadata = client.getObjectMetadata(bucket, key)
    Try(artifactMetdata) match {
      case Success(x) => if(x != null) Exists(x.getLastModified.getTime) else OperationFailure(throw new RuntimeException(s"$bucket/$key does not exist"))
      case Failure(th) => OperationFailure(th)
    }
  }

  private def bucketExists(bucket: String, client: AmazonS3Client) = {
    Try(client.listBuckets()) match {
      case Success(s) => if(s.exists(_.getName == bucket)) Exists(1) else OperationFailure(throw new RuntimeException(s"$bucket does not exist"))
      case Failure(th) => OperationFailure(th)
    }
  }

  def withRevision(artifact: String, revision: String) = s"${artifact.stripSuffix("/")}/$revision/} "
}
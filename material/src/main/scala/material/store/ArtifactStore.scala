package material.store

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import scala.util.{Failure, Success, Try}
import java.io.File
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import material.util.LoggerUtil
import scala.collection.JavaConversions._
import com.amazonaws.auth.BasicAWSCredentials
import scala.util.Failure
import scala.util.Success

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
    if(!listing.isTruncated) {
      mostRecentRevision(listing)
    } else {
      val newListing = client.listNextBatchOfObjects(listing)
      (mostRecentRevision(newListing) :: latestOf(client, newListing) :: Nil).max
    }
  }

  private def getLatest(artifactName: String, bucket: String, client: AmazonS3Client): FSOperationStatus = {
    val listObjectsRequest = new ListObjectsRequest().withBucketName(bucket).withDelimiter("/").withPrefix(artifactName)
    Try(client.listObjects(listObjectsRequest)) match {
      case Success(listing) =>
        val recent = latestOf(client, listing)
        val metadata = client.getObjectMetadata(bucket, s"$artifactName${recent.revision}/")
        val lastModified = metadata.getLastModified
        RevisionSuccess(recent, lastModified, "TrackbackURL", "RevisionComment")
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
}

object ArtifactStoreDriver extends App {
  val accessKeyId = "***REMOVED***"
  val secretKey = "nEKMO+CxNYP+uv82joYB5dXS1vBdsFQ/hgY6M7Ub"
  val client = S3ArtifactStore(new AmazonS3Client(new BasicAWSCredentials(accessKeyId, secretKey)), "indix-categories-ib")

//
//  println(client)
//  val objects = client.s3Client.listObjects(client.bucket)
//  println(objects.getObjectSummaries.asScala.map(_.getKey).mkString("\n"))
//  println("*"*20)
//  val prefixes = client.s3Client.listObjects(client.bucket, "20140604")
//  println(prefixes.getObjectSummaries.asScala.map(_.getKey).mkString("\n"))
//
//  println("*"*20)
  println(client.exists("20140604"))
//  println(client.exists("20140620"))

  println(client.get("20140620/20140620_Baby_ib.tsv.gz", "/tmp/20140604"))

}

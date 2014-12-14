package material.store

import java.io._
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ListObjectsRequest, ObjectListing, GetObjectRequest}
import scala.util.{Failure, Success, Try}
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import java.util.Date

trait FSOperationStatus {
  def isSuccess: Boolean
  def message: String =  ???
}
trait OpSuccess extends FSOperationStatus {
  override def isSuccess = true
  override def message = "Operation succeeded"
}
trait OpFailure extends FSOperationStatus {
  override def isSuccess = false
  override def message = "Operation failed"
}

case class CreateBucketSuccess(createdAt: DateTime, ownedBy: String) extends FSOperationStatus with OpSuccess
case class CopySuccess(md5: String) extends FSOperationStatus with OpSuccess
case class MoveSuccess(md5: String) extends FSOperationStatus with OpSuccess
case class Exists(fileUpdatedAt: Long) extends FSOperationStatus with OpSuccess
case class RevisionSuccess(revision: Revision, lastModified: Date, trackBackUrl: String, revisionComments: String) extends FSOperationStatus with OpSuccess
case class RevisionSinceSuccess(revisions: List[Revision]) extends FSOperationStatus with OpSuccess

case class OperationFailure(th: Throwable) extends FSOperationStatus with OpFailure {
  override def message = th.getStackTrace.map(_.toString).mkString("\n")
}

trait StoreHelper extends Serializable {
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

  def getLatest(artifactName: String, bucket: String, client: AmazonS3Client): FSOperationStatus = {
    Try(client.listObjects(bucket, artifactName)) match {
      case Success(listing) =>
        val recent = latestOf(client, listing)
        val metadata = client.getObjectMetadata(bucket, s"$artifactName/${recent.revision}")
        val lastModified = metadata.getLastModified
        RevisionSuccess(latestOf(client, listing), lastModified, "TrackbackURL", "RevisionComment")
      case Failure(th) => OperationFailure(th)
    }
  }

  def copyFromLocal(localFile: String, key: String, bucket: String, client: AmazonS3Client): FSOperationStatus = {
    Try(client.putObject(bucket, key, new File(localFile))) match {
      case Success(x) => CopySuccess(x.getContentMd5)
      case Failure(th) => OperationFailure(th)
    }
  }

  def copyToLocal(key: String, bucket: String, localFilePath: String, client: AmazonS3Client): FSOperationStatus = {
    Try(client.getObject(new GetObjectRequest(bucket, key), new File(localFilePath))) match {
      case Success(x) => CopySuccess(x.getContentMD5)
      case Failure(th) => OperationFailure(th)
    }
  }

  def createBucket(bucket: String, client: AmazonS3Client) {
    Try(client.createBucket(bucket)) match {
      case Success(x) => CreateBucketSuccess(new DateTime(x.getCreationDate.getTime), x.getOwner.getId)
      case Failure(th) => OperationFailure(th)
    }
  }

  def exists(bucket: String, key: String, client: AmazonS3Client) = {
    Try(client.getObjectMetadata(bucket, key)) match {
      case Success(x) => Exists(x.getLastModified.getTime)
      case Failure(th) => OperationFailure(th)
    }
  }

  def bucketExists(bucket: String, client: AmazonS3Client) = {
    Try(client.doesBucketExist(bucket)) match {
      case Success(s) => Exists(System.currentTimeMillis())
      case Failure(th) => OperationFailure(th)
    }
  }
}
package material.store

import java.io._
import org.apache.hadoop.fs._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{GetObjectRequest, PutObjectRequest}
import scala.util.{Failure, Success, Try}
import org.joda.time.DateTime

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

case class OperationFailure(th: Throwable) extends FSOperationStatus with OpFailure {
  override def message = th.getStackTrace.map(_.toString).mkString("\n")
}

trait StoreHelper extends Serializable {

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
      case Success(x) =>
        val time = x.getCreationDate.getTime
        CreateBucketSuccess(new DateTime(), x.getOwner.getId)
      case Failure(th) => OperationFailure(th)
    }
  }

  def exists(bucket: String, key: String, client: AmazonS3Client) = {
    Try(client.getObjectMetadata(bucket, key)) match {
      case Success(x) => Exists(x.getLastModified.getTime)
      case Failure(th) => OperationFailure(th)
    }
  }
}

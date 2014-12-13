package material.store

import java.io._
import org.apache.hadoop.fs._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem


trait FileHelper extends Serializable {

  def copy(inFile: String, outFile: String, conf: Configuration) = {
    val fs = FileSystem.get(conf)
    FileUtil.copy(fs, new Path(inFile), fs, new Path(outFile), false, conf)
  }

  def moveFromLocal(localFilePath: String, pathOnHDFS: String, conf : Configuration) = {
    val fs = FileSystem.get(conf)
    fs.moveFromLocalFile(new Path(localFilePath), new Path(pathOnHDFS))
  }

  def copyFromLocal(localFilePath: String, pathOnHDFS: String, conf : Configuration) = {
    val fs = FileSystem.get(conf)
    fs.copyFromLocalFile(new Path(localFilePath), new Path(pathOnHDFS))
  }

  def copyToLocal(remoteFilePath: String, localFilePath: String, conf: Configuration) = {
    val fs = FileSystem.get(conf)
    fs.copyToLocalFile(new Path(remoteFilePath), new Path(localFilePath))
  }

  def overwriteFromLocal(localFilePath: String, pathOnHDFS: String, conf : Configuration) = {
    val fs = FileSystem.get(conf)
    if(new File(localFilePath).exists) fs.copyFromLocalFile(false, true, new Path(localFilePath), new Path(pathOnHDFS))
  }

  def deleteHDFSFile(filePath: String, conf: Configuration) {
    FileSystem.get(conf).delete(new Path(filePath), true)
  }

  def createDir(filePath: String, conf: Configuration) {
    FileSystem.get(conf).mkdirs(new Path(filePath))
  }

  def exists(path: String, conf: Configuration) = {
    FileSystem.get(conf).exists(new Path(path))
  }
}

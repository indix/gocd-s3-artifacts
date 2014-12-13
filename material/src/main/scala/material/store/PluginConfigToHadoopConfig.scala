package material.store

import material.plugin.config.S3PackageMaterialConfiguration
import com.thoughtworks.go.plugin.api.config.Property
import org.apache.hadoop.conf.Configuration


object PluginConfigToHadoopConfig {
  val configMap = Map(
    S3PackageMaterialConfiguration.S3_ACCESS_KEY_ID -> List("fs.s3.awsAccessKeyId", "fs.s3n.awsAccessKeyId"),
    S3PackageMaterialConfiguration.S3_SECRET_ACCESS_KEY -> List("fs.s3.awsSecretAccessKey", "fs.s3n.awsSecretAccessKey")
  )

  def updateConfig(properties: TraversableOnce[Property])(implicit conf: Configuration = new Configuration()) = {
    properties.foreach{ p =>
      configMap.get(p.getKey).map(vs => vs.foreach(v => conf.set(v, p.getValue)))
    }
    conf
  }
}

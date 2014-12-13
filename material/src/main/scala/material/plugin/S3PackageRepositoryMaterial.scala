package material.plugin

import com.thoughtworks.go.plugin.api.material.packagerepository.{PackageMaterialPoller, PackageMaterialConfiguration, PackageMaterialProvider}
import material.plugin.config.S3PackageMaterialConfiguration


class S3PackageRepositoryMaterial extends PackageMaterialProvider {
  override def getConfig: PackageMaterialConfiguration = new S3PackageMaterialConfiguration()
  override def getPoller: PackageMaterialPoller = new S3PackageMaterialPoller()
}

package material.plugin

import com.thoughtworks.go.plugin.api.material.packagerepository.{PackageRevision, RepositoryConfiguration, PackageConfiguration, PackageMaterialPoller}
import com.thoughtworks.go.plugin.api.response.Result


class S3PackageMaterialPoller extends PackageMaterialPoller {
  override def getLatestRevision(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration): PackageRevision = ???

  override def checkConnectionToPackage(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration): Result = ???

  override def checkConnectionToRepository(repoConfig: RepositoryConfiguration): Result = ???

  override def latestModificationSince(packageConfig: PackageConfiguration, repoConfig: RepositoryConfiguration, revision: PackageRevision): PackageRevision = ???
}

package material.plugin.config

import java.util.Date

import com.amazonaws.services.s3.AmazonS3Client
import com.indix.gocd.models.{Revision, RevisionStatus, Artifact}
import com.indix.gocd.s3material.config.{S3PackageMaterialConfiguration}
import com.indix.gocd.s3material.plugin.S3PackageMaterialPoller
import com.indix.gocd.utils.store.S3ArtifactStore
import com.thoughtworks.go.plugin.api.material.packagerepository.{PackageConfiguration, RepositoryConfiguration, PackageMaterialProperty}
import org.mockito.Matchers
import org.scalatest.{WordSpec}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

class S3PackageMaterialPollerSpec extends WordSpec with MockitoSugar  {

    val sut = spy(new S3PackageMaterialPoller());
    val repoConfig = new RepositoryConfiguration();
    repoConfig.add(new PackageMaterialProperty(S3PackageMaterialConfiguration.S3_BUCKET, "S3 Bucket"));
    val mockS3ArtifactStore = mock[S3ArtifactStore];
    doReturn(mockS3ArtifactStore).when(sut).s3ArtifactStore("S3 Bucket");

    val packageConfig = new PackageConfiguration();
    packageConfig.add(new PackageMaterialProperty(S3PackageMaterialConfiguration.PIPELINE_NAME, "Pipeline"));
    packageConfig.add(new PackageMaterialProperty(S3PackageMaterialConfiguration.STAGE_NAME, "Stage"));
    packageConfig.add(new PackageMaterialProperty(S3PackageMaterialConfiguration.JOB_NAME, "Job"));

  "A package material poller, when checking connection to repository" when {
    " the bucket is found " should {
      "return success" in {
        doReturn(true).when(mockS3ArtifactStore).bucketExists();
        val result = sut.checkConnectionToRepository(repoConfig);

        assert(result.isSuccessful() == true);
      }
    }

  "the bucket is not found" should {
      "return failure" in {
        doReturn(false).when(mockS3ArtifactStore).bucketExists();

        val result = sut.checkConnectionToRepository(repoConfig);

        assert(result.isSuccessful() == false);
      }
    }
  }


  "A package material poller" when {
    "getting latest revision" should {
      "include original revision label if it exists" in {

        when(mockS3ArtifactStore.getLatest(Matchers.any[AmazonS3Client], Matchers.any[Artifact])).thenReturn(
          new RevisionStatus(new Revision("2.1"),new Date(), "url", "user", "3.1")
        );

        val result = sut.getLatestRevision(packageConfig, repoConfig);

        assert(result.getRevisionComment().equals("Original revision number: 3.1"));
      }

      "say the original revision label is unavailable if it doesn't exist" in {

        when(mockS3ArtifactStore.getLatest(Matchers.any[AmazonS3Client], Matchers.any[Artifact])).thenReturn(
          new RevisionStatus(new Revision("2.1"),new Date(), "url", "user")
        );

        val result = sut.getLatestRevision(packageConfig, repoConfig);

        assert(result.getRevisionComment().equals("Original revision number: unavailable"));

      }
    }
  }
}

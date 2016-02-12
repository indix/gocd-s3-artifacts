package com.indix.gocd.s3material.plugin;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.util.StringUtils;
import com.indix.gocd.s3material.config.S3PackageMaterialConfiguration;
import com.indix.gocd.models.Artifact;
import com.indix.gocd.models.Revision;
import com.indix.gocd.models.RevisionStatus;
import com.indix.gocd.utils.store.S3ArtifactStore;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialPoller;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;

public class S3PackageMaterialPoller implements PackageMaterialPoller {
    @Override
    public PackageRevision getLatestRevision(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration) {
        String s3Bucket = repositoryConfiguration.get(S3PackageMaterialConfiguration.S3_BUCKET).getValue();
        S3ArtifactStore artifactStore = s3ArtifactStore(s3Bucket);
        RevisionStatus revision = artifactStore.getLatest(s3Client(), artifact(packageConfiguration));
        return new PackageRevision(revision.revision.getRevision(), revision.lastModified, revision.user,
                String.format("Original revision number: %s",
                        StringUtils.isNullOrEmpty(revision.revisionLabel) ? "unavailable" : revision.revisionLabel),
                revision.tracebackUrl);
    }


    @Override
    public PackageRevision latestModificationSince(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration, PackageRevision previouslyKnownRevision) {
        // S3 doesn't seem to provide APIs to pull pegged updates
        // This means, we need to do a getLatest for this artifact anyways
        // Finally check to see if the latest revision is newer than the incoming revision
        // and return PackageRevision instance appropriately.
        PackageRevision packageRevision = getLatestRevision(packageConfiguration, repositoryConfiguration);
        if(new Revision(packageRevision.getRevision()).compareTo(new Revision(previouslyKnownRevision.getRevision())) > 0)
            return packageRevision;
        else
            return null;
    }

    @Override
    public Result checkConnectionToRepository(RepositoryConfiguration repositoryConfiguration) {
        String s3Bucket = repositoryConfiguration.get(S3PackageMaterialConfiguration.S3_BUCKET).getValue();
        S3ArtifactStore artifactStore = s3ArtifactStore(s3Bucket);
        if(artifactStore.bucketExists()){
            return ExecutionResult.success("Success");
        }else{
            return ExecutionResult.failure(String.format("Couldn't find bucket [%s]", s3Bucket));
        }
    }

    @Override
    public Result checkConnectionToPackage(PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration) {
        String s3Bucket = repositoryConfiguration.get(S3PackageMaterialConfiguration.S3_BUCKET).getValue();
        S3ArtifactStore artifactStore = s3ArtifactStore(s3Bucket);
        String prefix = artifact(packageConfiguration).prefix();
        if(artifactStore.exists(s3Bucket, prefix))
            return ExecutionResult.success("Success");
        else
            return ExecutionResult.failure(String.format("Couldn't find artifact at [%s]", prefix));
    }

    private static AmazonS3Client s3Client() {
        // The s3 client has a nice way to pick up the creds.
        // It first checks the env to see if it contains the required key related variables/values
        // If not, it checks the java system properties to see if it's set there(ideally via -D args)
        // If not, it falls back to check ~/.aws/credentials file
        // If not, finally, very insecure way, it tries to fetch from the internal metadata service that each
        // instance comes with(if its exposed).
        return new AmazonS3Client();
    }

    public S3ArtifactStore s3ArtifactStore(String s3Bucket) {
        return new S3ArtifactStore(s3Client(), s3Bucket);
    }

    private Artifact artifact(PackageConfiguration packageConfig) {
        String pipelineName = packageConfig.get(S3PackageMaterialConfiguration.PIPELINE_NAME).getValue();
        String stageName = packageConfig.get(S3PackageMaterialConfiguration.STAGE_NAME).getValue();
        String jobName = packageConfig.get(S3PackageMaterialConfiguration.JOB_NAME).getValue();
        return new Artifact(pipelineName, stageName,jobName);
    }
}

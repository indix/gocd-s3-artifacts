package com.indix.gocd.s3material.plugin;

import com.indix.gocd.s3material.config.S3PackageMaterialConfiguration;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialPoller;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProvider;

@Extension
public class S3PackageRepositoryMaterial implements PackageMaterialProvider {
    @Override
    public PackageMaterialConfiguration getConfig() {
        return new S3PackageMaterialConfiguration();
    }

    @Override
    public PackageMaterialPoller getPoller() {
        return new S3PackageMaterialPoller();
    }
}

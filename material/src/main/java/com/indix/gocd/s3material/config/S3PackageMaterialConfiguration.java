package com.indix.gocd.s3material.config;

import com.indix.gocd.utils.utils.Function;
import com.indix.gocd.utils.utils.Functions;
import com.indix.gocd.utils.utils.Lists;
import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class S3PackageMaterialConfiguration implements PackageMaterialConfiguration {
    public static String S3_BUCKET = "S3_BUCKET";
    public static String PIPELINE_NAME = "PIPELINE_NAME";
    public static String STAGE_NAME = "STAGE_NAME";
    public static String JOB_NAME = "JOB_NAME";
    private Logger LOG = Logger.getLoggerFor(this.getClass());

    public static List<Config> repoConfigs = new ArrayList<Config>() {{
        add(new Config(S3_BUCKET, "S3 Bucket", 0));
    }};

    public static List<Config> packageConfigs = new ArrayList<Config>(){{
        add(new Config(PIPELINE_NAME, "Pipeline Name", 0));
        add(new Config(STAGE_NAME, "Stage Name", 1));
        add(new Config(JOB_NAME, "Job Name", 2));
    }};


    Function<Config, Property> toPackageProperty = new Function<Config, Property>() {
        @Override
        public Property apply(Config input) {
            return input.toPackageProperty();
        }
    };

    @Override
    public RepositoryConfiguration getRepositoryConfiguration() {
        final RepositoryConfiguration repoConfig = new RepositoryConfiguration();
        List<Property> properties = Lists.map(S3PackageMaterialConfiguration.repoConfigs, toPackageProperty);
        Lists.foreach(properties, new Functions.VoidFunction<Property>() {
            @Override
            public void execute(Property property) {
                repoConfig.add(property);
            }
        });
        return repoConfig;
    }

    @Override
    public PackageConfiguration getPackageConfiguration() {
        final PackageConfiguration packageConfig = new PackageConfiguration();
        List<Property> properties = Lists.map(S3PackageMaterialConfiguration.packageConfigs, toPackageProperty);
        Lists.foreach(properties, new Functions.VoidFunction<Property>() {
            @Override
            public void execute(Property property) {
                packageConfig.add(property);
            }
        });
        return packageConfig;
    }

    @Override
    public ValidationResult isRepositoryConfigurationValid(final RepositoryConfiguration repositoryConfiguration) {
        return runValidations(repositoryConfiguration, repoConfigs);
    }

    @Override
    public ValidationResult isPackageConfigurationValid(final PackageConfiguration packageConfiguration, RepositoryConfiguration repositoryConfiguration) {
        return runValidations(packageConfiguration, packageConfigs);
    }

    private static ValidationResult runValidations(final Configuration config, List<Config> configsToBeCheckedFor) {
        List<ValidationResult> validationResults = Lists.map(configsToBeCheckedFor, new Function<Config, ValidationResult>() {
            @Override
            public ValidationResult apply(Config input) {
                return validate(config,
                        input.getName(),
                        String.format("%s configuration is missing or value is empty", input.getName()),
                        input.isRequired());
            }
        });
        List<ValidationError> validationErrors = Lists.flatMap(validationResults, new Function<ValidationResult, List<ValidationError>>() {
            @Override
            public List<ValidationError> apply(ValidationResult input) {
                return input.getErrors();
            }
        });
        ValidationResult finalResult = new ValidationResult();
        finalResult.addErrors(validationErrors);
        return finalResult;
    }

    private static ValidationResult validate(Configuration config, String property, String message, Boolean required) {
        if(required && (config.get(property) == null || StringUtils.isBlank(config.get(property).getValue()))) {
            ValidationResult validationResult = new ValidationResult();
            validationResult.addError(new ValidationError(property, message));
            return validationResult;
        }else{
            return new ValidationResult();
        }
    }
}

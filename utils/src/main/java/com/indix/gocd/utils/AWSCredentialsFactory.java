package com.indix.gocd.utils;

import com.amazonaws.auth.*;
import com.thoughtworks.go.plugin.api.logging.Logger;

import java.util.*;

import static com.indix.gocd.utils.Constants.AWS_ACCESS_KEY_ID;
import static com.indix.gocd.utils.Constants.AWS_SECRET_ACCESS_KEY;
import static com.indix.gocd.utils.Constants.AWS_USE_INSTANCE_PROFILE;

public class AWSCredentialsFactory {
    private static final List<String> validUseInstanceProfileValues = new ArrayList<String>(Arrays.asList("true","false","yes","no","1","0"));
    private static final List<String> affirmativeUseInstanceProfileValues = new ArrayList<String>(Arrays.asList("true","yes","1"));
    private Map<String, String> env = new HashMap<String, String>();
    private Logger log = Logger.getLoggerFor(AWSCredentialsFactory.class);

    public AWSCredentialsFactory(Map<String, String> environment) {
        this.env = environment;
    }

    public AWSCredentialsProvider getCredentialsProvider() {
        List<AWSCredentialsProvider> providers = new ArrayList<AWSCredentialsProvider>();
        if (env.containsKey(AWS_USE_INSTANCE_PROFILE)) {
            String useInstanceProfileCode = env.get(AWS_USE_INSTANCE_PROFILE);
            if (affirmativeUseInstanceProfileValues.contains(useInstanceProfileCode.toLowerCase())) {
                log.debug(String.format(
                        "AWS_USE_INSTANCE_PROFILE=%s;Initializing with InstanceProfileCredentialsProvider",
                        useInstanceProfileCode));
                providers.add(new InstanceProfileCredentialsProvider());
            }
            else if (!validUseInstanceProfileValues.contains(useInstanceProfileCode.toLowerCase())) {
                throwEnvInvalidFormat(AWS_USE_INSTANCE_PROFILE, useInstanceProfileCode,
                        validUseInstanceProfileValues.toString());
            }
        }

        if (providers.size() == 0) {
            if (!env.containsKey(AWS_ACCESS_KEY_ID))
                throwEnvNotFoundIllegalArgumentException(AWS_ACCESS_KEY_ID);
            if (!env.containsKey(AWS_SECRET_ACCESS_KEY))
                throwEnvNotFoundIllegalArgumentException(AWS_SECRET_ACCESS_KEY);

            // See AccessKeyCredentialsProvider as to why use it instead of built-in EnvironmentVariablesCredentialsProvider
            log.debug("AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY are present;Initializing with AccessKeyCredentialsProvider");
            providers.add(new AccessKeyCredentialsProvider(
                    env.get(AWS_ACCESS_KEY_ID),
                    env.get(AWS_SECRET_ACCESS_KEY)
            ));
        }

        return makeProvidersChain(providers);
    }

    public List<String> validationErrors() {
        List<String> result = new ArrayList<String>();

        if (env.containsKey(AWS_USE_INSTANCE_PROFILE) &&
                !validUseInstanceProfileValues.contains(env.get(AWS_USE_INSTANCE_PROFILE).toLowerCase())) {
                    result.add(
                        getEnvInvalidFormatMessage(AWS_USE_INSTANCE_PROFILE, env.get(AWS_USE_INSTANCE_PROFILE),
                        validUseInstanceProfileValues.toString())
                        );
            return result;
        }
        if (!env.containsKey(AWS_USE_INSTANCE_PROFILE) ||
                !affirmativeUseInstanceProfileValues.contains(env.get(AWS_USE_INSTANCE_PROFILE).toLowerCase())) {
                    if (!env.containsKey(AWS_ACCESS_KEY_ID))
                        result.add(getEnvNotFoundIllegalArgumentMessage(AWS_ACCESS_KEY_ID));
                    if (!env.containsKey(AWS_SECRET_ACCESS_KEY))
                        result.add(getEnvNotFoundIllegalArgumentMessage(AWS_SECRET_ACCESS_KEY));
        }

        return result;
    }

    /*
    public just to enable testing
     */
    public AWSCredentialsProvider makeProvidersChain(List<AWSCredentialsProvider> providers) {
        return new AWSCredentialsProviderChain(providers.toArray(new AWSCredentialsProvider[providers.size()]));
    }

    private void throwEnvNotFoundIllegalArgumentException(String environmentVariable) {
        throw new IllegalArgumentException(getEnvNotFoundIllegalArgumentMessage(environmentVariable));
    }

    private String getEnvNotFoundIllegalArgumentMessage(String environmentVariable) {
        return String.format("%s environment variable not present", environmentVariable);
    }
    private void throwEnvInvalidFormat(String environmentVariable, String value, String expected) {
        throw new IllegalArgumentException(getEnvInvalidFormatMessage(environmentVariable, value, expected));
    }

    private String getEnvInvalidFormatMessage(String environmentVariable, String value, String expected) {
        return String.format(
                "Unexpected value in %s environment variable; was %s, but expected one of the following %s",
                environmentVariable, value, expected);
    }}

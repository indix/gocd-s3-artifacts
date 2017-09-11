package com.indix.gocd.s3fetch;

import com.indix.gocd.utils.Constants;
import com.indix.gocd.utils.GoEnvironment;
import com.indix.gocd.utils.store.S3ArtifactStore;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class SelfFetchExecutor extends FetchExecutor {

    @Override
    protected String getArtifactsLocationTemplate(Config config, GoEnvironment env) {

        String prefix = config.getSourcePrefix();
        String source = config.getSource();

        if (StringUtils.isBlank(prefix)) {
            String pipeline = env.get("GO_PIPELINE_NAME");
            String pipelineCounter = env.get("GO_PIPELINE_COUNTER");
            String stage = config.getStage();
            String job = config.getJob();
            String bucket = getBucket(config, env);

            final S3ArtifactStore store = getS3ArtifactStore(env, bucket);
            prefix = store.getLatestPrefix(pipeline, stage, job, pipelineCounter);

            if (StringUtils.isBlank(prefix)) {
                throw new RuntimeException(
                        String.format("Could not determine stage counter on s3 with path: s3://%s/%s/%s/%s/%s.",
                                bucket,
                                pipeline,
                                stage,
                                job,
                                pipelineCounter));
            }
        }

        return prefix + "/" + source;
    }

    @Override
    public Map<String, String> validate(Config config) {
        Map<String, String> errors = new HashMap<>();

        if (StringUtils.isBlank(config.getSourcePrefix())) {

            if (StringUtils.isBlank(config.getStage())) {
                errors.put(Constants.STAGE, Constants.REQUIRED_FIELD_MESSAGE);
            }
            if (StringUtils.isBlank(config.getJob())) {
                errors.put(Constants.JOB, Constants.REQUIRED_FIELD_MESSAGE);
            }
        }

        if (StringUtils.isBlank(config.getSource())) {
            errors.put(Constants.SOURCE, Constants.REQUIRED_FIELD_MESSAGE);
        }

        return errors;
    }
}

package com.indix.gocd.utils;

import org.apache.commons.lang3.BooleanUtils;
import java.lang.StringBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.indix.gocd.utils.Constants.AWS_USE_IAM_ROLE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static com.indix.gocd.utils.Constants.GO_SERVER_DASHBOARD_URL;

/**
 * Wrapper around Go's Environment variables
 */
public class GoEnvironment {
    private Pattern envPat = Pattern.compile("\\$\\{(\\w+)\\}");
    private Map<String, String> environment = new HashMap<String, String>();

    public GoEnvironment() {
        this(System.getenv());
    }

    public GoEnvironment(Map<String, String> defaultEnvironment) {
        this.environment.putAll(defaultEnvironment);
    }

    public GoEnvironment putAll(Map<String, String> existing) {
        environment.putAll(existing);
        return this;
    }

    public Map<String,String> asMap() { return environment; }

    public String get(String name) {
        return environment.get(name);
    }

    public String getOrElse(String name, String defaultValue) {
        if(has(name)) return get(name);
        else return defaultValue;
    }

    public boolean has(String name) {
        return environment.containsKey(name) && isNotEmpty(get(name));
    }

    public boolean isAbsent(String name) {
        return !has(name);
    }

    public String traceBackUrl() {
        String serverUrl = get(GO_SERVER_DASHBOARD_URL);
        String pipelineName = get("GO_PIPELINE_NAME");
        String pipelineCounter = get("GO_PIPELINE_COUNTER");
        String stageName = get("GO_STAGE_NAME");
        String stageCounter = get("GO_STAGE_COUNTER");
        String jobName = get("GO_JOB_NAME");
        return String.format("%s/go/tab/build/detail/%s/%s/%s/%s/%s", serverUrl, pipelineName, pipelineCounter, stageName, stageCounter, jobName);
    }

    public String triggeredUser() {
        return get("GO_TRIGGER_USER");
    }

    public String replaceVariables(String str) {
      Matcher m = envPat.matcher(str);

      StringBuffer sb = new StringBuffer();
      while (m.find()) {
        String replacement = get(m.group(1));
        if(replacement != null) {
          m.appendReplacement(sb, replacement);
        }
      }

      m.appendTail(sb);

      return sb.toString();
    }

    /**
     * Version Format on S3 is <code>pipeline/stage/job/pipeline_counter.stage_counter</code>
     */
    public String artifactsLocationTemplate() {
        String pipeline = get("GO_PIPELINE_NAME");
        String stageName = get("GO_STAGE_NAME");
        String jobName = get("GO_JOB_NAME");

        String pipelineCounter = get("GO_PIPELINE_COUNTER");
        String stageCounter = get("GO_STAGE_COUNTER");
        return artifactsLocationTemplate(pipeline, stageName, jobName, pipelineCounter, stageCounter);
    }

    public String artifactsLocationTemplate(String pipeline, String stageName, String jobName, String pipelineCounter, String stageCounter) {
        return String.format("%s/%s/%s/%s.%s", pipeline, stageName, jobName, pipelineCounter, stageCounter);
    }

    private static final List<String> validUseIamRoleValues = new ArrayList<String>(Arrays.asList("true", "false", "yes", "no", "on", "off"));
    public boolean hasAWSUseIamRole() {
        if (!has(AWS_USE_IAM_ROLE)) {
            return false;
        }

        String useIamRoleValue = get(AWS_USE_IAM_ROLE);
        Boolean result = BooleanUtils.toBooleanObject(useIamRoleValue);
        if (result == null) {
            throw new IllegalArgumentException(getEnvInvalidFormatMessage(AWS_USE_IAM_ROLE,
                    useIamRoleValue, validUseIamRoleValues.toString()));
        }
        else {
            return result.booleanValue();
        }
    }

    private String getEnvInvalidFormatMessage(String environmentVariable, String value, String expected){
        return String.format(
                "Unexpected value in %s environment variable; was %s, but expected one of the following %s",
                environmentVariable, value, expected);
    }
}

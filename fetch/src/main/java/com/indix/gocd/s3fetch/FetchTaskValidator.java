package com.indix.gocd.s3fetch;

import io.jmnarloch.cd.go.plugin.api.validation.AbstractTaskValidator;
import io.jmnarloch.cd.go.plugin.api.validation.ValidationErrors;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class FetchTaskValidator extends AbstractTaskValidator {

    public static final String REPO = "Repo";
    public static final String PACKAGE = "Package";
    public static final String DESTINATION = "Destination";

    @Override
    public void validate(Map<String, Object> properties, ValidationErrors errors) {
        if (StringUtils.isBlank((String) properties.get(REPO))) {
            errors.addError(REPO, "S3 repository must be specified");
        }

        if (StringUtils.isBlank((String) properties.get(PACKAGE))) {
            errors.addError(PACKAGE, "S3 package must be specified");
        }

        if (StringUtils.isBlank((String) properties.get(DESTINATION))) {
            errors.addError(DESTINATION, "Destination directory must be specified");
        }
    }

}

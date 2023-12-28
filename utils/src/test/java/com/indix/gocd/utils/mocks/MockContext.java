package com.indix.gocd.utils.mocks;

import com.indix.gocd.utils.Context;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.Map;

public class MockContext extends Context {

    public MockContext(Map contextMap) {
        super(contextMap);
    }

    @Override
    public void printMessage(String message) {

    }

    @Override
    public void printEnvironment() {

    }

}

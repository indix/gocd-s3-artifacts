package com.indix.gocd.s3material.config;

import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;

public class Config {
    private String name;
    private String displayName;
    private int order;
    private boolean required = true;

    public Config(String name, String displayName, int order, boolean required) {
        this.name = name;
        this.displayName = displayName;
        this.order = order;
        this.required = required;
    }

    public Config(String name, String displayName, int order) {
        this.name = name;
        this.displayName = displayName;
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Config config = (Config) o;

        if (order != config.order) return false;
        if (required != config.required) return false;
        if (displayName != null ? !displayName.equals(config.displayName) : config.displayName != null) return false;
        if (name != null ? !name.equals(config.name) : config.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + order;
        result = 31 * result + (required ? 1 : 0);
        return result;
    }
    
    public Property toPackageProperty() {
        return new PackageMaterialProperty(name)
            .with(Property.DISPLAY_NAME, displayName)
            .with(Property.DISPLAY_ORDER, order)
            .with(Property.REQUIRED, required);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}

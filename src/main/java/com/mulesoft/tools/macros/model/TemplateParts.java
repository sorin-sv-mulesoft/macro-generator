package com.mulesoft.tools.macros.model;

public class TemplateParts {

    private String resources;
    private String file;
    private Object data;

    public Object getData() {
        return data;
    }

    public TemplateParts setData(Object data) {
        this.data = data;
        return this;
    }

    public String getFile() {
        return file;
    }

    public TemplateParts setFile(String file) {
        this.file = file;
        return this;
    }

    public String getResources() {
        return resources;
    }

    public TemplateParts setResources(String resources) {
        this.resources = resources;
        return this;
    }
}

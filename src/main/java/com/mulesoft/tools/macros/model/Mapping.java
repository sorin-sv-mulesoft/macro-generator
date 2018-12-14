package com.mulesoft.tools.macros.model;

import java.util.ArrayList;
import java.util.List;

public class Mapping {

    private static final long serialVersionUID = 8346103843787059468L;
    private String methodName;
    private List<String> attributes = new ArrayList<String>();

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {

        this.methodName = methodName;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

}

package com.mulesoft.tools.macros.builder;

import com.mulesoft.tools.macros.model.Mapping;

import javax.xml.ws.Holder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

public class MacroBuilder {

    private Map<String, String> operationMapping = new HashMap<>();
    private Map<String, Set<String>> parameterPlacementChange = new HashMap<>();
    private Map<String, Map<String, String>> parameterMapping = new HashMap<>();
    private Map<String, Class> operationTransformer = new HashMap<>();
    private String m3NamespacePrefix, m4NamespacePrefix;
    private List<Mapping> m3Mapping = null;
    private StringBuilder macros = new StringBuilder();
    private static String regex = "([a-z])([A-Z]+)";
    private static String replacement = "$1-$2";

    public MacroBuilder(String oldNamespacePrefix, String newNamespacePrefix) {
        this.m3NamespacePrefix = oldNamespacePrefix;
        this.m4NamespacePrefix = newNamespacePrefix;
    }

    ///////////////////////////// BUILDER CUSTOM TRANSFORMATIONS ////////////////////////////////////////
    public MacroBuilder mapOperation(String m3Operation, String m4Operation) {
        operationMapping.put(m4Operation, m3Operation);
        return this;
    }

    public MacroBuilder mapParameter(String operation, String oldParam, String newParam) {
        getOperationParameterMapping(operation).put(oldParam, newParam);
        return this;
    }

    public MacroBuilder addTransformer(String operation, Class clazz) {
        operationTransformer.put(operation, clazz);
        return this;
    }

    public MacroBuilder addPlacementChange(String operation, String parameter) {
        Set<String> params = parameterPlacementChange.get(operation);
        if (params == null) {
            params = new HashSet<>();
            parameterPlacementChange.put(operation, params);
        }
        if (getOperationParameterMapping(operation).get(parameter) == null) {
            getOperationParameterMapping(operation).put(parameter, parameter);
        }
        ;
        params.add(parameter);
        return this;
    }

    ///////////////////////////// BUILDER CUSTOM TRANSFORMATIONS ////////////////////////////////////////

    public String build() {
        return macros.toString();
    }

    public MacroBuilder processMethod(Method m4Method) {
        // search m3 list of methods and see if there's any method matching the m4Method name
        // or if there is any custom mapped operation for the m4Method value
        Mapping m3MethodMapping = getM3MethodMapping(m4Method.getName());
        if (m3MethodMapping != null) {
            List<String> m4Parameters = Arrays.asList(m4Method.getParameters()).stream().map(Parameter::getName).collect(Collectors.toList());
            String m4QualifiedName = getM4NamespacePrefix() + ":" + snakeCase(m4Method.getName()),
                    m3QualifiedName = getM3NamespacePrefix() + ":" + snakeCase(m3MethodMapping.getMethodName());
            String attributesMapping = processMatchingAttributes(m3MethodMapping.getAttributes(), m4Parameters, m3MethodMapping.getMethodName());

            Class mappedClass = getOperationTransformerClass(m3MethodMapping.getMethodName());

            macros
//                        .append("<!-- ").append(m4Method.getName()).append(" -->\n")
                    .append("<#macro \"").append(m3QualifiedName).append("\">\n")
                    .append((mappedClass != null ? getTransformer(mappedClass, m4Method.getName()) : ""))
                    .append("\t<#assign paramsMapping = ").append(attributesMapping).append("/>\n")
                    .append("\t<").append(m4QualifiedName).append(" ${getAttributes(.node, paramsMapping, \"").append(m3NamespacePrefix).append("\")}>\n")
                    .append("\t\t${getChildNodes(.node, \"").append(getM3NamespacePrefix()).append("\", \"").append(getM4NamespacePrefix()).append("\", paramsMapping)}\n")
                    .append("\t</").append(m4QualifiedName).append(">\n")
                    .append("</#macro>\n");
        }
        return this;
    }

    private String getTransformer(Class clazz, String m4MethodName) {
        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        Map<String, String> customParameterMapping = getOperationParameterMapping(m4MethodName);

        StringBuilder sb = new StringBuilder("${namespaces.addNamespace(\"ee\",\"http://www.mulesoft.org/schema/mule/ee/core\")}\n" +
                "${namespaces.addSchemaLocation(\"http://www.mulesoft.org/schema/mule/ee/core http://www.mulesoft.org/schema/mule/ee/core/current/mule-ee.xsd\")}\n").append("<ee:transform>\n" +
                        "\t\t\t<ee:message>\n" +
                        "\t\t\t\t<ee:set-payload ><![CDATA[%dw 2.0\n" +
                        "output application/java\n" +
                        "---\n" +
                        "{\n"
        );
        final Holder<Boolean> first = new Holder<>(true);
        fields.forEach(field -> {
            String fieldName = field.getName();
            sb.append((first.value ? "" : ",\n"))
                    .append("\t\t\t\t" + field.getName() + ": ${searchInAttrAndChildren(.node, \"" + m3NamespacePrefix + "\", \"" + (customParameterMapping != null && customParameterMapping.get(fieldName) != null ? customParameterMapping.get(fieldName) : fieldName) + "\")}");
            first.value = false;
        });

        sb.append("\n} as Object {\n" +
                "\tclass : \"" + clazz.getName() + "\"\n" +
                "}");
        sb.append("]]></ee:set-payload>\n" +
                "\t\t\t</ee:message>\n" +
                "\t\t</ee:transform>\n");
        return sb.toString();
    }

    private String snakeCase(String value) {
        return value.replaceAll(regex, replacement)
                .toLowerCase();
    }

    public String processMatchingAttributes(List<String> m3Attributes, List<String> m4Parameters, String m3MethodName) {
        StringBuilder sb = new StringBuilder("{");
        m3Attributes.forEach(attribute -> {
            Map<String, String> customParameterMapping = getOperationParameterMapping(m3MethodName);
            if (m4Parameters.contains(attribute) || (customParameterMapping != null && customParameterMapping.keySet().contains(attribute))) {
                sb.append(sb.length() > 1 ? "," : "")
                        .append(wrap(attribute, "\""))
                        .append(":")
                        .append(getAttributeMappingValue(attribute, parameterMapping.get(m3MethodName), parameterPlacementChange.get(m3MethodName) != null && parameterPlacementChange.get(m3MethodName).contains(attribute)));
            }
        });
        sb.append("}");
        return sb.toString();
    }

    private String getAttributeMappingValue(String attributeName, Map<String, String> paramsMapping, Boolean placementChange) {
        String name = "\"mappedName\":" + wrap(paramsMapping != null && paramsMapping.get(attributeName) != null ? paramsMapping.get(attributeName) : attributeName, "\"");
        String location = placementChange ? ", \"placementChange\": true" : "";
        return "{" + name + location + "}";
    }

    private String wrap(String value, String chr) {
        return chr + value + chr;
    }

    private boolean checkM4MethodMapping(Mapping m3Mapping, String m4MethodName) {
        return (operationMapping.get(m4MethodName) != null && operationMapping.get(m4MethodName).equals(m3Mapping.getMethodName()))
                || (operationMapping.get(m4MethodName) == null && m3Mapping.getMethodName().equals(m4MethodName));
    }

    private Mapping getM3MethodMapping(String m4MethodName) {
        Mapping result;
        try {
            result = m3Mapping.stream().filter(mapping -> checkM4MethodMapping(mapping, m4MethodName)).findFirst().get();
        } catch (NoSuchElementException ex) {
            return null;
        }
        return result;
    }

    public void setM3Mapping(List<Mapping> m3Mapping) {
        this.m3Mapping = m3Mapping;
    }

    public String getM3NamespacePrefix() {
        return m3NamespacePrefix;
    }

    public String getM4NamespacePrefix() {
        return m4NamespacePrefix;
    }

    public Class getOperationTransformerClass(String operation) {
        return operationTransformer.get(operation);
    }

    private Map<String, String> getOperationParameterMapping(String operation) {
        Map<String, String> parametersMapping = parameterMapping.get(operation);
        if (parametersMapping == null) {
            parametersMapping = new HashMap<>();
            parameterMapping.put(operation, parametersMapping);
        }
        return parametersMapping;
    }

    private <T, K> Map<T, K> newHashMap(T key, K value) {
        return new HashMap<T, K>() {{
            put(key, value);
        }};
    }

}

package com.mulesoft.tools.macros.facade;

import com.mulesoft.tools.macros.builder.MacroBuilder;
import com.mulesoft.tools.macros.model.Mapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MacrosGenerator {

    private ClassLoader classLoader;
    public static MacroBuilder macroBuilder;
    public static ZipFile m3Jar,m4Jar;

    public String generateMacros(String m3JarPath, String m4JarPath) throws IOException, ClassNotFoundException {

        m3Jar = new ZipFile(m3JarPath);
        m4Jar = new ZipFile(m4JarPath);
        classLoader = new URLClassLoader(
                new URL[]{
                        new URL("file:\\" + m3JarPath),
                        new URL("file:\\" + m4JarPath)
                }
                , this.getClass().getClassLoader());

        List<Mapping> m3Mapping = getOperations(m3Jar, "SalesforceConnector.class");
        Class m4Class = getClassFromM4Jar("CoreOperations.class");

        macroBuilder = new MacroBuilder("sfdc", "salesforce")
                .mapParameter("create", "objects", "records")
                .mapParameter("update", "objects", "records")
                .mapParameter("upsert", "objects", "records")
                .mapParameter("delete", "ids", "delete-ids")

                .mapOperation("createBulk", "createMultiple")
                .mapParameter("createBulk", "objects", "records")

                .mapOperation("updateBulk", "updateMultiple")
                .mapParameter("updateBulk", "objects", "records")

                .addTransformer("retrieve", getClassFromM4Jar("RetrieveRequest.class"))

                .mapParameter("findDuplicates", "criterion", "criteria")

                .addTransformer("convertLead", getClassFromM4Jar("LeadConvertRequest.class") );

        macroBuilder.setM3Mapping(m3Mapping);

        List<Method> m4Operations = Arrays.asList(m4Class.getDeclaredMethods());
        m4Operations.forEach(macroBuilder::processMethod);
        return macroBuilder.build();
    }

    private Class getClassFromJar(ZipFile file, String clazz) throws IOException, ClassNotFoundException {
        Enumeration en = file.entries();

        while (en.hasMoreElements()) {
            ZipEntry val = (ZipEntry) en.nextElement();
            String name = val.getName();
            if (name.contains(clazz)) {
                return Class.forName(val.getName().replaceAll("/", ".").replace(".class", ""), false, classLoader);
            }
        }
        return null;
    }

    private Class getClassFromM3Jar(String clazz) throws IOException, ClassNotFoundException {
        return getClassFromJar(m3Jar, clazz);
    }

    private Class getClassFromM4Jar(String clazz) throws IOException, ClassNotFoundException {
        return getClassFromJar(m4Jar,clazz);
    }

    private List<Mapping> getOperations(ZipFile file, String className) throws IOException, ClassNotFoundException {
        List<Mapping> mappings = new ArrayList<>();
        Class classToLoad = getClassFromJar(file, className);

        List<Method> declaredMethods = Arrays.asList(classToLoad.getDeclaredMethods());
        List<Method> methods = Arrays.asList(classToLoad.getMethods());
        declaredMethods.forEach(method -> {
            if (methods.contains(method)) {
                Mapping mapping = new Mapping();
                mapping.setMethodName(method.getName());
                List<String> attributes = Arrays.asList(method.getParameters()).stream().map(Parameter::getName).collect(Collectors.toList());
                mapping.setAttributes(attributes);
                mappings.add(mapping);
            }
        });
        return mappings;
    }

}

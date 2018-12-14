package com.mulesoft.tools.macros;

import com.mulesoft.tools.macros.fx.MacroApp;
import javafx.application.Application;

import java.io.IOException;

public class Main {

    public static void main(String... args) throws IOException, ClassNotFoundException {

        Application.launch(MacroApp.class);

//        MacrosGenerator mg = new MacrosGenerator();
//        mg.generateMacros(
//                "D:\\Work\\Projects\\Mule\\connectors\\salesforce-connector\\target\\mule-module-sfdc-8.7.0-SNAPSHOT.jar",
//                "D:\\Work\\Projects\\Mule\\bkp\\mule-salesforce-connector\\target\\mule-salesforce-connector-9.4.6-SNAPSHOT-mule-plugin.jar"
//        );
    }
}

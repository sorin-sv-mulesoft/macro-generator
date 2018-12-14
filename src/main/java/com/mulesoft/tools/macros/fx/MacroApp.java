package com.mulesoft.tools.macros.fx;

import com.mulesoft.tools.macros.facade.MacrosGenerator;
import com.mulesoft.tools.macros.freemarker.config.Freemarker;
import com.mulesoft.tools.macros.model.TemplateParts;
import com.sun.javafx.webkit.WebConsoleListener;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class MacroApp extends Application {

    private static javafx.stage.Stage primaryStage;
    private MacrosGenerator macroGenerator = new MacrosGenerator();
    private Freemarker freemarker = new Freemarker();
    private static String resources;
    public static CookieManager manager;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        primaryStage.show();
        resources = getClass().getResource("/resources").toExternalForm();
        manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);

        // set up a console logger for the web view component
        WebConsoleListener.setDefaultListener(new WebConsoleListener() {
            @Override
            public void messageAdded(WebView webView, String message, int lineNumber, String sourceId) {
                System.out.println("Console: [" + sourceId + ":" + lineNumber + "] " + message);
            }
        });

    }

    private WebEngine getWebEngine() {
        return ((WebView) primaryStage.getScene().getRoot().getChildrenUnmodifiable().get(0)).getEngine();
    }
    private JSObject getWindow() {
        return  (JSObject) getWebEngine().executeScript("window");
    }
    private void renderTemplate(TemplateParts templateParts) throws IOException, TemplateException {

        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();

        VBox content = new VBox();
        content.getChildren().add(webView);
        Scene scene = new Scene(content, 800, 600);
        primaryStage.setScene(scene);

        Template temp = freemarker.getCfg().getTemplate("template.html");
        Writer out = new StringWriter();
        templateParts.setResources(resources);
        temp.process(templateParts, out);
        webEngine.loadContent(out.toString());

        getWindow().setMember("java", this);

    }


}

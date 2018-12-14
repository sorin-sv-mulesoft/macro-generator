package com.mulesoft.tools.macros.freemarker.config;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

import java.nio.charset.StandardCharsets;

public class Freemarker {
    Configuration cfg;

    public Freemarker() {

        cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_28);
        cfg.setClassForTemplateLoading(this.getClass(), "/resources/template");
        cfg.setDefaultEncoding(StandardCharsets.UTF_8.displayName());
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);

    }

    public Configuration getCfg() {
        return cfg;
    }

    public void setCfg(Configuration cfg) {
        this.cfg = cfg;
    }

}

package com.gitb.reports.extensions;

import freemarker.template.TemplateMethodModelEx;

import java.util.List;

public class PrintResult implements TemplateMethodModelEx {

    @Override
    public Object exec(List arguments) {
        if (arguments != null && !arguments.isEmpty()) {
            var text = String.valueOf(arguments.get(0));
            if ("UNDEFINED".equals(text)) {
                return "INCOMPLETE";
            }
            return text;
        }
        return null;
    }

}

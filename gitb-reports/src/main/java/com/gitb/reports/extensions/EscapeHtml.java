package com.gitb.reports.extensions;

import freemarker.template.TemplateMethodModelEx;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;

public class EscapeHtml implements TemplateMethodModelEx {

    @Override
    public Object exec(List arguments) {
        if (arguments != null && !arguments.isEmpty()) {
            var text = arguments.get(0);
            return StringEscapeUtils.escapeHtml4(String.valueOf(text));
        }
        return null;
    }

}

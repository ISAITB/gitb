package com.gitb.reports.extensions;

import freemarker.template.TemplateMethodModelEx;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;

public class NoWrapPrinter implements TemplateMethodModelEx {

    @Override
    public Object exec(List arguments) {
        if (arguments != null && !arguments.isEmpty()) {
            var text = arguments.get(0);
            Object max = null;
            if (arguments.size() > 1) {
                max = arguments.get(1);
            }
            if (text != null) {
                var textToProcess = String.valueOf(text);
                int maxToProcess = 140;
                if (max instanceof Number maxNumber) {
                    maxToProcess = maxNumber.intValue();
                }
                maxToProcess -= 3;
                if (textToProcess.length() > maxToProcess) {
                    return textToProcess.substring(0, maxToProcess) + "...";
                }
            } else {
                return null;
            }
            return StringEscapeUtils.escapeHtml4(String.valueOf(text));
        }
        return null;
    }

}

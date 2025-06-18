/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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

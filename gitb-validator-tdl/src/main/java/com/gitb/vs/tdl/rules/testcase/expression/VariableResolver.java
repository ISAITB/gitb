package com.gitb.vs.tdl.rules.testcase.expression;

import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.Utils;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;
import java.util.LinkedHashMap;
import java.util.Map;

public class VariableResolver implements XPathVariableResolver {

    private VariableResolverProvider provider;


    public VariableResolver(VariableResolverProvider provider) {
        this.provider = provider;
    }

    @Override
    public Object resolveVariable(QName variableName) {
        checkVariablesInToken("$"+variableName.getLocalPart());
        return "";
    }

    public void checkVariablesInToken(String token) {
        checkVariables(getVariablesInToken(token));
    }

    private Map<String, VariableData> getVariablesInToken(String token) {
        // $a
        // $aa{aa}
        // $aa{$aa{$aa}}
        Map<String, VariableData> variableNames = new LinkedHashMap<>();
        StringBuilder str = new StringBuilder(token);
        while (str.indexOf("$") == 0) {
            str.deleteCharAt(0);
            int containedIndexStart = str.indexOf("{");
            String variableName;
            if (containedIndexStart != -1) {
                variableName = str.substring(0, containedIndexStart);
                str.delete(0, containedIndexStart + 1);
                int containedIndexEnd = str.lastIndexOf("}");
                str.delete(containedIndexEnd, str.length());
            } else {
                variableName = str.toString();
                str.delete(0, str.length());
            }
            variableNames.put(variableName, new VariableData(containedIndexStart != -1, str.toString()));
        }
        return variableNames;
    }

    private void checkVariables(Map<String, VariableData> variableNames) {
        if (variableNames != null) {
            for (Map.Entry<String, VariableData> entry: variableNames.entrySet()) {
                if (provider.getScope().containsKey(entry.getKey())) {
                    if (entry.getValue().container && !provider.getScope().get(entry.getKey())) {
                        // Simple variable referenced as container variable.
                        provider.addReportItem(ErrorCode.SIMPLE_VARIABLE_REFERENCED_AS_CONTAINER, provider.getCurrentTestCase().getId(), Utils.stepNameWithScriptlet(provider.getCurrentStep(), provider.getCurrentScriptlet()), entry.getKey());
                    }
                } else {
                    // Variable not found in scope.
                    provider.addReportItem(ErrorCode.VARIABLE_NOT_IN_SCOPE, provider.getCurrentTestCase().getId(), Utils.stepNameWithScriptlet(provider.getCurrentStep(), provider.getCurrentScriptlet()), entry.getKey());
                }
                if (Utils.DOMAIN_MAP.equals(entry.getKey()) && !Utils.isVariableExpression(entry.getValue().containerExpression)) {
                    if (!provider.getContext().getExternalConfiguration().getExternalParameters().contains(entry.getValue().containerExpression)) {
                        provider.getContext().recordCustomDomainParameter(entry.getValue().containerExpression);
                    }
                }
                if (Utils.ORGANISATION_MAP.equals(entry.getKey()) && !Utils.isVariableExpression(entry.getValue().containerExpression)) {
                    if (!Utils.ORGANISATION_MAP__FULL_NAME.equals(entry.getValue().containerExpression) && !Utils.ORGANISATION_MAP__SHORT_NAME.equals(entry.getValue().containerExpression)) {
                        provider.getContext().recordCustomOrganisationProperty(entry.getValue().containerExpression);
                    }
                }
                if (Utils.SYSTEM_MAP.equals(entry.getKey()) && !Utils.isVariableExpression(entry.getValue().containerExpression)) {
                    if (!Utils.SYSTEM_MAP__FULL_NAME.equals(entry.getValue().containerExpression) && !Utils.SYSTEM_MAP__SHORT_NAME.equals(entry.getValue().containerExpression) && !Utils.SYSTEM_MAP__VERSION.equals(entry.getValue().containerExpression)) {
                        provider.getContext().recordCustomSystemProperty(entry.getValue().containerExpression);
                    }
                }
            }
        }
    }

    static class VariableData {

        Boolean container;
        String containerExpression;

        VariableData(Boolean container, String containerExpression) {
            this.container = container;
            this.containerExpression = containerExpression;
        }

    }

}


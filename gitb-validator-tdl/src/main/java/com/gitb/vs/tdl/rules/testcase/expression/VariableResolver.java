package com.gitb.vs.tdl.rules.testcase.expression;

import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.Utils;
import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VariableResolver implements XPathVariableResolver {

    private static final Pattern BRACKET_DETECTION_PATTERN = Pattern.compile("(?:'[^']*'|(\\$(?:[a-zA-Z][a-zA-Z\\-_0-9]*)(?:\\{(?:[\\$\\{\\}a-zA-Z\\-\\._0-9]*)\\})*))|(?:\"[^\"]*\"|(\\$(?:[a-zA-Z][a-zA-Z\\-_0-9]*)(?:\\{(?:[\\$\\{\\}a-zA-Z\\-\\._0-9]*)\\})*))");
    private static final String CURLY_BRACKET_OPEN_REPLACEMENT = "_com.gitb.OPEN_";
    private static final String CURLY_BRACKET_CLOSE_REPLACEMENT = "_com.gitb.CLOSE_";
    private static final String DOLLAR_REPLACEMENT = "_com.gitb.DOLLAR_";

    private final VariableResolverProvider provider;
    private final Set<String> scriptletMissingVariables = new TreeSet<>();

    public VariableResolver(VariableResolverProvider provider) {
        this.provider = provider;
    }

    @Override
    public Object resolveVariable(QName variableName) {
        String variableExpression = "$"+toTDLExpression(variableName.getLocalPart());
        checkVariablesInToken(variableExpression);
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
                    if (provider.isStandaloneScriptlet()) {
                        // Scriptlet - report once at the end for all such variables.
                        scriptletMissingVariables.add(entry.getKey());
                    } else {
                        // Test case.
                        provider.addReportItem(ErrorCode.VARIABLE_NOT_IN_SCOPE, provider.getCurrentTestCase().getId(), Utils.stepNameWithScriptlet(provider.getCurrentStep(), provider.getCurrentScriptlet()), entry.getKey());
                    }
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

    public void scopeFinished() {
        if (!scriptletMissingVariables.isEmpty()) {
            provider.addReportItem(ErrorCode.POTENTIALLY_INVALID_SCRIPTLET_CONTEXT_VARIABLE, provider.getCurrentTestCase().getId(), StringUtils.joinWith(", ", scriptletMissingVariables.toArray()));
        }
    }

    private static String processMatch(MatchResult match, int group, String expression) {
        var matchedText = match.group(group);
        if (matchedText != null) {
            // Replace all curly brackets and all dollar signs except the first one (which is always there for matches).
            var variableExpression = matchedText.substring(1) // Remove initial dollar.
                    .replace("{", CURLY_BRACKET_OPEN_REPLACEMENT)       // Replace curly brace open.
                    .replace("}", CURLY_BRACKET_CLOSE_REPLACEMENT)      // Replace curly brace close.
                    .replace("$", DOLLAR_REPLACEMENT);                  // Replace dollars.
            variableExpression = "$"+variableExpression;                      // Add initial dollar.
            return new StringBuilder(expression)
                    .replace(match.start(group), match.end(group), variableExpression)
                    .toString();
        } else {
            return expression;
        }
    }

    public static String toLegalXPath(String expression) {
        // GITB TDL expressions contain curly braces for container types which are reserved characters in XPath 2.0+
        var matcher = BRACKET_DETECTION_PATTERN.matcher(expression);
        List<MatchResult> matches = matcher.results().collect(Collectors.toList());
        // Reverse so that the sections to replace don't overlap with the replacements.
        Collections.reverse(matches);
        for (var match: matches) {
            for (int i=1; i <= matcher.groupCount(); i++) {
                expression = processMatch(match, i, expression);
            }
        }
        return expression;
    }

    public static String toTDLExpression(String expression) {
        return expression.replace(CURLY_BRACKET_OPEN_REPLACEMENT, "{")
                .replace(CURLY_BRACKET_CLOSE_REPLACEMENT, "}")
                .replace(DOLLAR_REPLACEMENT, "$");
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


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
    private boolean resolveVariablesAsNumber = false;

    public VariableResolver(VariableResolverProvider provider) {
        this.provider = provider;
    }

    @Override
    public Object resolveVariable(QName variableName) {
        String variableExpression = "$"+toTDLExpression(variableName.getLocalPart());
        checkVariablesInToken(variableExpression);
        if (resolveVariablesAsNumber) {
            return 1;
        } else {
            return "";
        }
    }

    public void setResolveVariablesAsNumber(boolean resolveVariablesAsNumber) {
        this.resolveVariablesAsNumber = resolveVariablesAsNumber;
    }

    public void checkVariablesInToken(String token) {
        checkVariables(getVariablesInToken(token));
    }

    private void detectVariables(String token, Map<String, VariableData> variableNames) {
        char[] tokenChars = token.toCharArray();
        if (tokenChars.length > 0 && tokenChars[0] == '$' && hasNoneOrValidBrackets(tokenChars)) {
            int index = 0;
            StringBuilder variableName = new StringBuilder();
            while (index < tokenChars.length) {
                char nextChar = tokenChars[index];
                if (nextChar == '{') {
                    // Container expression - find matching closing bracket.
                    String containerExpression = getContainerExpression(tokenChars, index);
                    if (containerExpression != null) {
                        if (variableName.length() > 1) {
                            variableNames.put(variableName.substring(1), new VariableData(true, containerExpression));
                            variableName.delete(0, variableName.length());
                        }
                        detectVariables(containerExpression, variableNames);
                        // Skip container expression plus opening and closing brackets.
                        index += containerExpression.length() + 2;
                    } else {
                        // Invalid container expression.
                        return;
                    }
                } else {
                    variableName.append(nextChar);
                    index += 1;
                }
            }
            if (variableName.length() > 1) {
                variableNames.put(variableName.substring(1), new VariableData(false, null));
            }
        }
    }

    private Map<String, VariableData> getVariablesInToken(String token) {
        // $a
        // $aa{aa}
        // $aa{$aa}{$aa}
        // $aa{$aa{$aa}}
        // $aa{$aa{$aa}}{aa}
        // $aa{$aa{$aa}{aa}}{aa}
        Map<String, VariableData> variableNames = new LinkedHashMap<>();
        detectVariables(token, variableNames);
        return variableNames;
    }

    private String getContainerExpression(char[] tokenChars, int openBracketIndex) {
        int innerIndex = openBracketIndex+1;
        int openBrackets = 0;
        StringBuilder containerExpression = new StringBuilder();
        while (innerIndex < tokenChars.length) {
            char nextInnerChar = tokenChars[innerIndex];
            if (nextInnerChar == '{') {
                openBrackets += 1;
                containerExpression.append(nextInnerChar);
            } else if (nextInnerChar == '}') {
                if (openBrackets == 0) {
                    // Found the matching closing bracket.
                    return containerExpression.toString();
                } else {
                    openBrackets -= 1;
                    containerExpression.append(nextInnerChar);
                }
            } else {
                containerExpression.append(nextInnerChar);
            }
            innerIndex += 1;
        }
        return null;
    }

    private boolean hasNoneOrValidBrackets(char[] tokenChars) {
        int unmatchedOpenCount = 0;
        for (var character: tokenChars) {
            if (character == '{') {
                unmatchedOpenCount += 1;
            } else if (character == '}') {
                if (unmatchedOpenCount > 0) {
                    unmatchedOpenCount -= 1;
                } else {
                    return false;
                }
            }
        }
        return unmatchedOpenCount == 0;
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
                    if (!Utils.SYSTEM_MAP__FULL_NAME.equals(entry.getValue().containerExpression) && !Utils.SYSTEM_MAP__SHORT_NAME.equals(entry.getValue().containerExpression) && !Utils.SYSTEM_MAP__VERSION.equals(entry.getValue().containerExpression) && !Utils.SYSTEM_MAP__API_KEY.equals(entry.getValue().containerExpression)) {
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


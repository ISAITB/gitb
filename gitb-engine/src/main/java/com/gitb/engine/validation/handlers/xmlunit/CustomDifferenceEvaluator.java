package com.gitb.engine.validation.handlers.xmlunit;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DifferenceEvaluator;

import java.util.Collections;
import java.util.List;

public class CustomDifferenceEvaluator implements DifferenceEvaluator {

    private static final String ANY_VALUE = "?";
    private final List<String> xpathsToIgnore;

    public CustomDifferenceEvaluator(List<String> xpathsToIgnore) {
        if (xpathsToIgnore == null) {
            this.xpathsToIgnore = Collections.emptyList();
        } else {
            this.xpathsToIgnore = xpathsToIgnore.stream().map(this::prepareXPathForComparison).toList();
        }
    }

    @Override
    public ComparisonResult evaluate(Comparison comparison, ComparisonResult comparisonResult) {
        if (comparisonResult == ComparisonResult.DIFFERENT) {
            final Node controlNode = comparison.getControlDetails().getTarget();
            if ((controlNode != null) && ANY_VALUE.equals(controlNode.getNodeValue())) {
                return ComparisonResult.SIMILAR;
            }
            if (comparison.getControlDetails().getTarget() == null) {
                if (comparison.getControlDetails().getParentXPath() != null) {
                    if (isIgnoredPath(comparison.getControlDetails().getParentXPath())) {
                        return ComparisonResult.SIMILAR;
                    }
                }
            } else {
                if (xpathsToIgnore != null && isIgnored(comparison.getControlDetails().getTarget())) {
                    return ComparisonResult.SIMILAR;
                }
            }
        }
        return comparisonResult;
    }

    private String prepareXPathForComparison(String xpath) {
        StringBuilder str = new StringBuilder();
        if (xpath != null) {
            var pathParts = StringUtils.split(xpath, '/');
            if (pathParts != null) {
                for (var pathPart: pathParts) {
                    str.append('/');
                    // Remove child brackets.
                    int bracketStart = pathPart.indexOf('[');
                    if (bracketStart >= 0) {
                        pathPart = pathPart.substring(0, bracketStart);
                    }
                    // Remove namespace.
                    int namespaceEnd = pathPart.indexOf(':');
                    if (namespaceEnd >= 0) {
                        pathPart = pathPart.substring(namespaceEnd+1);
                    }
                    str.append(pathPart);
                }
            }
        }
        return str.toString();
    }

    private String getNodePath(Node target, StringBuilder path) {
        if (target.getNodeType() == Node.ATTRIBUTE_NODE) {
            getNodePath(((org.w3c.dom.Attr)target).getOwnerElement(), path);
        } else {
            if (target.getParentNode() != null) {
                path.insert(0, "/"+target.getNodeName());
                getNodePath(target.getParentNode(), path);
            }
        }
        return path.toString();
    }

    private boolean isIgnored(Node target) {
        String nodePath = getNodePath(target, new StringBuilder());
        return isIgnoredPath(nodePath);
    }

    private boolean isIgnoredPath(String path) {
        var pathToCheck = prepareXPathForComparison(path);
        for (String pathToIgnore: xpathsToIgnore) {
            if (pathToCheck.startsWith(pathToIgnore)) {
                return true;
            }
        }
        return false;
    }
}

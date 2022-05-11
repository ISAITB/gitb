package com.gitb.engine.validation.handlers.xmlunit;

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
            this.xpathsToIgnore = xpathsToIgnore;
        }
    }

    @Override
    public ComparisonResult evaluate(Comparison comparison, ComparisonResult comparisonResult) {
        if (comparisonResult == ComparisonResult.DIFFERENT) {
            final Node controlNode = comparison.getControlDetails().getTarget();
            if ((controlNode != null) && ANY_VALUE.equals(controlNode.getNodeValue())) {
                return ComparisonResult.SIMILAR;
            }
            if (xpathsToIgnore != null && isIgnored(comparison.getControlDetails().getTarget())) {
                return ComparisonResult.SIMILAR;
            }
        }
        return comparisonResult;
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
        for (String pathToIgnore: xpathsToIgnore) {
            if (nodePath.startsWith(pathToIgnore)) {
                return true;
            }
        }
        return false;
    }
}

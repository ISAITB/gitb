package com.gitb.reports.extensions;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

import java.util.List;

public class TestCoverageBlock implements TemplateMethodModelEx {

    private static final int MIN_WIDTH = 40;
    private static final int PASSED = 0;
    private static final int FAILED = 1;
    private static final int UNDEFINED = 2;

    private void adjustWidths(int[] widths, boolean[] areMinimums, int indexToCheck, int otherIndex1, int otherIndex2) {
        if (areMinimums[indexToCheck]) {
            int previousWidth = widths[indexToCheck];
            int diff = MIN_WIDTH - previousWidth;
            widths[indexToCheck] = MIN_WIDTH;
            if (widths[otherIndex1] > 0 && !areMinimums[otherIndex1] && widths[otherIndex2] > 0 && !areMinimums[otherIndex2]) {
                int split = Math.round((float) diff / 2);
                widths[otherIndex1] = widths[otherIndex1] - split;
                widths[otherIndex2] = widths[otherIndex2] - (diff - split);
            } else if (widths[otherIndex1] > 0 && !areMinimums[otherIndex1]) {
                widths[otherIndex1] = widths[otherIndex1] - diff;
            } else if (widths[otherIndex2] > 0 && !areMinimums[otherIndex2]) {
                widths[otherIndex2] = widths[otherIndex2] - diff;
            }
        }
    }

    private void appendPart(StringBuilder html, int[] counts, float[] ratios, int[] widths, int index, String className, boolean first, boolean last) {
        if (counts[index] > 0) {
            String percentage = String.format("%.1f", ratios[index]*100);
            String positionClass = "";
            if (first) positionClass += " start";
            if (last) positionClass += " end";
            html.append(String.format("<div class='value inline coverage-result coverage-%s%s' style='width: %spx'>%s%%</div>", className, positionClass, widths[index], percentage));
        }
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        // 0: #passed, 1: #failed, 2: #undefined, 3: Container pixels
        var counts = new int[] {
                ((TemplateNumberModel)arguments.get(0)).getAsNumber().intValue(),
                ((TemplateNumberModel)arguments.get(1)).getAsNumber().intValue(),
                ((TemplateNumberModel)arguments.get(2)).getAsNumber().intValue()
        };
        int width = ((TemplateNumberModel)arguments.get(3)).getAsNumber().intValue();
        // Do calculations
        int total = counts[PASSED] + counts[FAILED] + counts[UNDEFINED];
        var ratios = new float[] { (float) counts[PASSED] /total, (float) counts[FAILED] /total, (float) counts[UNDEFINED] /total };
        var widths = new int[] {
                Math.round(width * ratios[PASSED]),
                Math.round(width * ratios[FAILED]),
                Math.round(width * ratios[UNDEFINED])
        };
        // Make sure we didn't match the width due to rounding.
        int roundingDiff = width - widths[PASSED] - widths[FAILED] - widths[UNDEFINED];
        if (widths[PASSED] > widths[FAILED] && widths[PASSED] > widths[UNDEFINED]) {
            widths[PASSED] = widths[PASSED] + roundingDiff;
        } else if (widths[FAILED] > widths[PASSED] && widths[FAILED] > widths[UNDEFINED]) {
            widths[FAILED] = widths[FAILED] + roundingDiff;
        } else {
            widths[UNDEFINED] = widths[UNDEFINED] + roundingDiff;
        }
        // Adjust the widths to ensure we respect the minimum widths.
        var areMinimums = new boolean[] { counts[PASSED] > 0 && (widths[PASSED] < MIN_WIDTH), counts[FAILED] > 0 && (widths[FAILED] < MIN_WIDTH), counts[UNDEFINED] > 0 && (widths[UNDEFINED] < MIN_WIDTH) };
        adjustWidths(widths, areMinimums, PASSED, FAILED, UNDEFINED);
        adjustWidths(widths, areMinimums, FAILED, PASSED, UNDEFINED);
        adjustWidths(widths, areMinimums, UNDEFINED, PASSED, FAILED);
        // Print.
        var container = new StringBuilder();
        container.append(String.format("<div class='coverage-container' style='width: %spx'>", width));
        appendPart(container, counts, ratios, widths, PASSED, "passed", true, counts[FAILED] == 0 && counts[UNDEFINED] == 0);
        appendPart(container, counts, ratios, widths, FAILED, "failed", counts[PASSED] == 0, counts[UNDEFINED] == 0);
        appendPart(container, counts, ratios, widths, UNDEFINED, "undefined", counts[PASSED] == 0 && counts[FAILED] == 0, true);
        container.append("</div>");
        return container.toString();
    }

}

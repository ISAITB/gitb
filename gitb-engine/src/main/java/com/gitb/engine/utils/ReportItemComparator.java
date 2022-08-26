package com.gitb.engine.utils;

import com.gitb.tr.BAR;
import com.gitb.tr.TestAssertionReportType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.xml.bind.JAXBElement;
import java.util.Comparator;
import java.util.function.Function;

public class ReportItemComparator implements Comparator<JAXBElement<TestAssertionReportType>> {

    private final Function<Pair<JAXBElement<TestAssertionReportType>, JAXBElement<TestAssertionReportType>>, Integer> comparator;

    public ReportItemComparator(SortType sortType) {
        if (sortType == SortType.LOCATION_THEN_SEVERITY) {
            comparator = (items) -> {
                var check = compareLocations(items);
                if (check == 0) {
                    check = compareSeverities(items);
                }
                return check;
            };
        } else {
            comparator = (items) -> {
                var check = compareSeverities(items);
                if (check == 0) {
                    check = compareLocations(items);
                }
                return check;
            };
        }
    }

    private int compareSeverities(Pair<JAXBElement<TestAssertionReportType>, JAXBElement<TestAssertionReportType>> items) {
        String name1 = items.getLeft().getName().getLocalPart();
        String name2 = items.getRight().getName().getLocalPart();
        if (name1.equals(name2)) {
            return 0;
        } else if ("error".equals(name1)) {
            return -1;
        } else if ("error".equals(name2)) {
            return 1;
        } else if ("warning".equals(name1)) {
            return -1;
        } else {
            return "warning".equals(name2) ? 1 : 0;
        }
    }

    private int compareLocations(Pair<JAXBElement<TestAssertionReportType>, JAXBElement<TestAssertionReportType>> items) {
        return Integer.compare(
                extractLine(items.getLeft().getValue()),
                extractLine(items.getRight().getValue())
        );
    }

    /**
     * Compare first by location (if defined) and then by severity.
     *
     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return The comparison result.
     */
    public int compare(JAXBElement<TestAssertionReportType> o1, JAXBElement<TestAssertionReportType> o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        } else {
            return comparator.apply(Pair.of(o1, o2));
        }
    }

    private int extractLine(TestAssertionReportType item) {
        var line = -1;
        var locationString = ((BAR)item).getLocation();
        if (locationString != null) {
            var parts = StringUtils.split(locationString, ':');
            if (parts != null && parts.length == 3) {
                try {
                    line = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    // Ignore to return default.
                }
            }
        }
        return line;
    }

    public enum SortType {
        LOCATION_THEN_SEVERITY,
        SEVERITY_THEN_LOCATION
    }

}

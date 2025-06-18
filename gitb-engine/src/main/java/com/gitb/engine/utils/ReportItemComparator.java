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

package com.gitb.engine.utils;

import com.gitb.tr.BAR;
import com.gitb.tr.TestAssertionReportType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import jakarta.xml.bind.JAXBElement;
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

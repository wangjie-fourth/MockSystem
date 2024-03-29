/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.coverage.reporting.lineCoverage;

import org.github.fourth.mocksystem.packages.mockit.coverage.CallPoint;
import org.github.fourth.mocksystem.packages.mockit.coverage.lines.LineCoverageData;
import org.github.fourth.mocksystem.packages.mockit.coverage.lines.PerFileLineCoverage;
import org.github.fourth.mocksystem.packages.mockit.coverage.reporting.ListOfCallPoints;
import org.github.fourth.mocksystem.packages.mockit.coverage.reporting.parsing.LineParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

final class LineCoverageFormatter {
    @Nonnull
    private final StringBuilder formattedLine;
    @Nonnull
    private final LineSegmentsFormatter segmentsFormatter;
    @Nullable
    private final ListOfCallPoints listOfCallPoints;

    LineCoverageFormatter(boolean withCallPoints) {
        formattedLine = new StringBuilder(200);
        segmentsFormatter = new LineSegmentsFormatter(withCallPoints, formattedLine);
        listOfCallPoints = withCallPoints ? new ListOfCallPoints() : null;
    }

    String format(@Nonnull LineParser lineParser, @Nonnull PerFileLineCoverage lineCoverageData) {
        formattedLine.setLength(0);
        formattedLine.append("<pre class='prettyprint");

        int line = lineParser.getNumber();
        LineCoverageData lineData = lineCoverageData.getLineData(line);

        if (lineData.containsBranches()) {
            formatLineWithMultipleSegments(lineParser, lineData);
        } else {
            formatLineWithSingleSegment(lineParser, lineData);
        }

        return formattedLine.toString();
    }

    private void formatLineWithMultipleSegments(@Nonnull LineParser lineParser, @Nonnull LineCoverageData lineData) {
        formattedLine.append(" jmp'>");
        segmentsFormatter.formatSegments(lineParser, lineData);
    }

    private void formatLineWithSingleSegment(@Nonnull LineParser lineParser, @Nonnull LineCoverageData lineData) {
        formattedLine.append(lineData.isCovered() ? " covered" : " uncovered");

        List<CallPoint> callPoints = lineData.getCallPoints();

        if (listOfCallPoints != null && callPoints != null) {
            formattedLine.append(" cp' onclick='showHide(this)");
        }

        formattedLine.append("' id='l").append(lineParser.getNumber()).append("s0'>");
        String content = lineParser.getInitialElement().toString();
        formattedLine.append(content).append("</pre>");

        if (listOfCallPoints != null) {
            listOfCallPoints.insertListOfCallPoints(callPoints);
            formattedLine.append(listOfCallPoints.getContents());
        }
    }
}

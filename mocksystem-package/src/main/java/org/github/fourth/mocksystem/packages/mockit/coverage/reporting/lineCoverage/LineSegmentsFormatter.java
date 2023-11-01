/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.coverage.reporting.lineCoverage;

import org.github.fourth.mocksystem.packages.mockit.coverage.CallPoint;
import org.github.fourth.mocksystem.packages.mockit.coverage.lines.BranchCoverageData;
import org.github.fourth.mocksystem.packages.mockit.coverage.lines.LineCoverageData;
import org.github.fourth.mocksystem.packages.mockit.coverage.lines.LineSegmentData;
import org.github.fourth.mocksystem.packages.mockit.coverage.reporting.ListOfCallPoints;
import org.github.fourth.mocksystem.packages.mockit.coverage.reporting.parsing.LineElement;
import org.github.fourth.mocksystem.packages.mockit.coverage.reporting.parsing.LineParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static java.lang.Character.isWhitespace;

final class LineSegmentsFormatter {
    @Nullable
    private final ListOfCallPoints listOfCallPoints;
    @Nonnull
    private final StringBuilder formattedLine;

    // Helper fields:
    private int lineNumber;
    private int segmentIndex;
    private LineSegmentData segmentData;
    @Nullable
    private LineElement element;

    LineSegmentsFormatter(boolean withCallPoints, @Nonnull StringBuilder formattedLine) {
        listOfCallPoints = withCallPoints ? new ListOfCallPoints() : null;
        this.formattedLine = formattedLine;
    }

    void formatSegments(@Nonnull LineParser lineParser, @Nonnull LineCoverageData lineData) {
        lineNumber = lineParser.getNumber();

        List<BranchCoverageData> branchData = lineData.getBranches();
        int numSegments = lineData.getNumberOfSegments();

        element = lineParser.getInitialElement().appendUntilNextCodeElement(formattedLine);

        segmentIndex = 0;
        segmentData = lineData;
        appendUntilNextBranchingPoint();

        while (element != null && segmentIndex < numSegments) {
            segmentData = segmentIndex == 0 ? lineData : branchData.get(segmentIndex - 1);
            element = element.appendUntilNextCodeElement(formattedLine);
            appendUntilNextBranchingPoint();
        }

        if (element != null) {
            element.appendAllBefore(formattedLine, null);
        }

        formattedLine.append("</pre>");

        if (listOfCallPoints != null) {
            formattedLine.append(listOfCallPoints.getContents());
        }
    }

    private void appendUntilNextBranchingPoint() {
        if (element != null) {
            LineElement firstElement = element;
            element = element.findNextBranchingPoint();

            appendToFormattedLine(firstElement);

            if (element != null && element.isBranchingElement()) {
                formattedLine.append(element.getText());
                element = element.getNext();
            }
        }
    }

    private void appendToFormattedLine(@Nonnull LineElement firstElement) {
        if (firstElement != element) {
            appendStartTag();
            firstElement.appendAllBefore(formattedLine, element);
            appendEndTag();

            segmentIndex++;
        }
    }

    private void appendStartTag() {
        formattedLine.append("<span id='l").append(lineNumber).append('s').append(segmentIndex);
        formattedLine.append("' title='Executions: ").append(segmentData.getExecutionCount()).append("' ");

        if (segmentData.isCovered()) {
            if (segmentData.containsCallPoints()) {
                formattedLine.append("class='covered cp' onclick='showHide(this,").append(segmentIndex).append(")'>");
            } else {
                formattedLine.append("class='covered'>");
            }
        } else {
            formattedLine.append("class='uncovered'>");
        }
    }

    private void appendEndTag() {
        int i = formattedLine.length() - 1;

        while (isWhitespace(formattedLine.charAt(i))) {
            i--;
        }

        formattedLine.insert(i + 1, "</span>");

        if (listOfCallPoints != null) {
            List<CallPoint> callPoints = segmentData.getCallPoints();
            listOfCallPoints.insertListOfCallPoints(callPoints);
        }
    }
}

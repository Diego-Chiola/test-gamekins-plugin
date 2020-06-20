package io.jenkins.plugins.gamekins.challenge;

import io.jenkins.plugins.gamekins.util.JacocoUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public abstract class CoverageChallenge implements Challenge {

    final JacocoUtil.ClassDetails classDetails;
    final int fullyCoveredLines;
    final int partiallyCoveredLines;
    final int notCoveredLines;
    final double coverage;
    double solvedCoverage = 0;
    final String branch;
    final long created = System.currentTimeMillis();
    long solved = 0;

    public CoverageChallenge(JacocoUtil.ClassDetails classDetails, String branch) throws IOException {
        this.classDetails = classDetails;
        this.branch = branch;
        Document document = Jsoup.parse(this.classDetails.getJacocoSourceFile(), "UTF-8");
        this.fullyCoveredLines = JacocoUtil.calculateCoveredLines(document, "fc");
        this.partiallyCoveredLines = JacocoUtil.calculateCoveredLines(document, "pc");
        this.notCoveredLines = JacocoUtil.calculateCoveredLines(document, "nc");
        this.coverage = classDetails.getCoverage();
    }

    public int getFullyCoveredLines() {
        return fullyCoveredLines;
    }

    public int getPartiallyCoveredLines() {
        return partiallyCoveredLines;
    }

    public int getNotCoveredLines() {
        return notCoveredLines;
    }

    @Override
    public long getCreated() {
        return this.created;
    }

    @Override
    public long getSolved() {
        return this.solved;
    }
}

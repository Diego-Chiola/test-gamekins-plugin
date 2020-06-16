package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Result;
import hudson.model.Run;

public class BuildChallenge implements Challenge {
    @Override
    public boolean isSolved(String workspace, Run<?, ?> run) {
        return run.getResult() == Result.SUCCESS;
    }

    @Override
    public int getScore() {
        return 1;
    }

    @Override
    public String toString() {
        return "Let the Build run successfully";
    }
}

package jenkins.plugins.bearychat;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.Cause;
import hudson.scm.ChangeLogSet;
import hudson.util.LogTaskListener;
import org.apache.commons.lang.StringUtils;
import jenkins.plugins.bearychat.Messages;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

public class Helper {

    private static final Logger logger = Logger.getLogger(ActiveNotifier.class.getName());

    private static final String STATUS_STARTING = Messages.JobStatusStarting();
    private static final String STATUS_BACK_TO_NORMAL = Messages.JobStatusBackToNormal();
    private static final String STATUS_STILL_FAILING = Messages.JobStatusStillFailing();
    private static final String STATUS_SUCCESS = Messages.JobStatusSuccess();
    private static final String STATUS_FAILURE = Messages.JobStatusFailure();
    private static final String STATUS_ABORTED = Messages.JobStatusAborted();
    private static final String STATUS_NOT_BUILT = Messages.JobStatusNotBuilt();
    private static final String STATUS_UNSTABLE = Messages.JobStatusUnstable();
    private static final String STATUS_UNKNOWN = Messages.JobStatusUnknown();

    private static final int MAX_COMMIT_MESSAGES = 5;

    public static String COLOR_GREEN = "#008800";
    public static String COLOR_RED = "#FF0000";
    public static String COLOR_YELLOW = "#FFFF00";
    public static String COLOR_BLUE = "#0080FF";
    public static String COLOR_GREY = "#808080";

    public static String escape(String string) {
        string = string.replace("&", "&amp;");
        string = string.replace("<", "&lt;");
        string = string.replace(">", "&gt;");

        return string;
    }


    public static String getStatusByBuild(AbstractBuild build) {
        if (build.isBuilding()) {
            return STATUS_STARTING;
        }
        Result result = build.getResult();
        Result previousResult;
        Run previousBuild = null;

        try {
            previousBuild = build.getProject().getLastBuild().getPreviousBuild();
        } catch (Exception e) {
            logger.info("get previous build failure");
        }

        Run previousSuccessfulBuild = build.getPreviousSuccessfulBuild();
        boolean buildHasSucceededBefore = previousSuccessfulBuild != null;

        /*
         * If the last build was aborted, go back to find the last non-aborted build.
         * This is so that aborted builds do not affect build transitions.
         * I.e. if build 1 was failure, build 2 was aborted and build 3 was a success the transition
         * should be failure -> success (and therefore back to normal) not aborted -> success.
         */
        Run lastNonAbortedBuild = previousBuild;
        while (lastNonAbortedBuild != null && lastNonAbortedBuild.getResult() == Result.ABORTED) {
            lastNonAbortedBuild = lastNonAbortedBuild.getPreviousBuild();
        }

        /* If all previous builds have been aborted, then use
         * SUCCESS as a default status so an aborted message is sent
         */
        if (lastNonAbortedBuild == null) {
            previousResult = Result.SUCCESS;
        } else {
            previousResult = lastNonAbortedBuild.getResult();
        }

        /* Back to normal should only be shown if the build has actually succeeded at some point.
         * Also, if a build was previously unstable and has now succeeded the status should be
         * "Back to normal"
         */
        if (result == Result.SUCCESS
                && (previousResult == Result.FAILURE || previousResult == Result.UNSTABLE)
                && buildHasSucceededBefore) {
            return STATUS_BACK_TO_NORMAL;
        }
        if (result == Result.FAILURE && previousResult == Result.FAILURE) {
            return STATUS_STILL_FAILING;
        }
        if (result == Result.SUCCESS) {
            return STATUS_SUCCESS;
        }
        if (result == Result.FAILURE) {
            return STATUS_FAILURE;
        }
        if (result == Result.ABORTED) {
            return STATUS_ABORTED;
        }
        if (result == Result.NOT_BUILT) {
            return STATUS_NOT_BUILT;
        }
        if (result == Result.UNSTABLE) {
            return STATUS_UNSTABLE;
        }
        return STATUS_UNKNOWN;
    }

    public static String getCommitMessages(AbstractBuild build) {
        ChangeLogSet changeSet = build.getChangeSet();
        List<ChangeLogSet.Entry> entries = new LinkedList<ChangeLogSet.Entry>();
        for (Object o : changeSet.getItems()) {
            ChangeLogSet.Entry entry = (ChangeLogSet.Entry) o;
            logger.info("Entry " + o);
            entries.add(entry);
        }

        if (entries.isEmpty()) {
            logger.info("Empty change...");
            Cause.UpstreamCause c = (Cause.UpstreamCause)build.getCause(Cause.UpstreamCause.class);
            if (c == null) {
                return "No Commit Changes.";
            }
            String upProjectName = c.getUpstreamProject();
            int buildNumber = c.getUpstreamBuild();
            AbstractProject project = null;
            try {
                project = Hudson.getInstance().getItemByFullName(upProjectName, AbstractProject.class);
            } catch (NullPointerException e) {
                logger.info("get project failure");
            }

            if (project == null) {
                return "No Commit Changes.";
            }
            AbstractBuild upBuild = (AbstractBuild)project.getBuildByNumber(buildNumber);
            return getCommitMessages(upBuild);
        }

        StringBuffer commits = new StringBuffer();
        for (int i=0; i < MAX_COMMIT_MESSAGES && i<entries.size(); i++) {
            ChangeLogSet.Entry entry = entries.get(i);
            StringBuffer commit = new StringBuffer();
            commit.append(entry.getMsg());
            commit.append(" [").append(entry.getAuthor().getDisplayName()).append("]");
            commits.append("- ").append(commit.toString()).append("\n");
        }
        if(entries.size() > MAX_COMMIT_MESSAGES) {
            int left = entries.size() - 5;
            commits.append(left).append(" more...");
        }

        return commits.toString();

    }

    public static String getChanges(BearyChatNotifier notifier, AbstractBuild r) {
        StringBuffer result = new StringBuffer();
        if (!r.hasChangeSetComputed()) {
            logger.info("No change set computed...");
            return null;
        }
        ChangeLogSet changeSet = r.getChangeSet();
        List<ChangeLogSet.Entry> entries = new LinkedList<ChangeLogSet.Entry>();
        Set<ChangeLogSet.AffectedFile> files = new HashSet<ChangeLogSet.AffectedFile>();
        for (Object o : changeSet.getItems()) {
            ChangeLogSet.Entry entry = (ChangeLogSet.Entry) o;
            logger.info("Entry " + o);
            entries.add(entry);
            files.addAll(entry.getAffectedFiles());
        }
        if (entries.isEmpty()) {
            logger.info("Empty change...");
            return null;
        }
        Set<String> authors = new HashSet<String>();
        for (ChangeLogSet.Entry entry : entries) {
            authors.add(entry.getAuthor().getDisplayName());
        }

        result.append("Started by changes from ");
        result.append(StringUtils.join(authors, ", "));
        result.append(" (");
        result.append(files.size());
        result.append(" file(s) changed)");

        return result.toString();
    }

    public static String getCustomStartMessage(BearyChatNotifier notifier, AbstractBuild build) {
        String customStartMessage = notifier.getCustomStartMessage();
        EnvVars envVars = null;
        try {
            envVars = build.getEnvironment(new LogTaskListener(logger, INFO));
            customStartMessage = envVars.expand(customStartMessage);
        } catch (IOException e) {
            logger.log(SEVERE, e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.log(SEVERE, e.getMessage(), e);
        }
        return customStartMessage;
    }

    public static String getCustomEndMessage(BearyChatNotifier notifier, AbstractBuild build) {
        String customEndMessage = notifier.getCustomEndMessage();
        EnvVars envVars = null;
        try {
            envVars = build.getEnvironment(new LogTaskListener(logger, INFO));
            customEndMessage = envVars.expand(customEndMessage);
        } catch (IOException e) {
            logger.log(SEVERE, e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.log(SEVERE, e.getMessage(), e);
        }
        return customEndMessage;
    }

    public static String getBuildColor(AbstractBuild build) {
        Result result = build.getResult();
        String color;
        if (result == Result.SUCCESS) {
            color = COLOR_BLUE;
        } else if (result == Result.FAILURE) {
            color = COLOR_RED;
        } else if (result == Result.UNSTABLE) {
            color = COLOR_YELLOW;
        } else {
            color = COLOR_GREY;
        }
        return color;
    }

    public static String createBackToNormalDurationString(AbstractBuild build) {
        long backToNormalDuration = 0;
        try {
            Run previousSuccessfulBuild = build.getPreviousSuccessfulBuild();
            long previousSuccessStartTime = previousSuccessfulBuild.getStartTimeInMillis();
            long previousSuccessDuration = previousSuccessfulBuild.getDuration();
            long previousSuccessEndTime = previousSuccessStartTime + previousSuccessDuration;
            long buildStartTime = build.getStartTimeInMillis();
            long buildDuration = build.getDuration();
            long buildEndTime = buildStartTime + buildDuration;
            backToNormalDuration = buildEndTime - previousSuccessEndTime;
            return Util.getTimeSpanString(backToNormalDuration);
        } catch (NullPointerException e) {
            return Util.getTimeSpanString(backToNormalDuration);
        } catch (Exception e) {
            return Util.getTimeSpanString(backToNormalDuration);
        }

    }

    public static String getBuildStatusMessage(BearyChatNotifier notifier, AbstractBuild build) {
        StringBuffer result = new StringBuffer();
        result.append(Helper.escape(Helper.getStatusByBuild(build)));
        result.append(" " +  Messages.JobDurationAfter() + " ");
        if (result.toString().contains(STATUS_BACK_TO_NORMAL)) {
            result.append(Helper.createBackToNormalDurationString(build));
        } else {
            result.append(build.getDurationString());
        }

        return result.toString();
    }
}


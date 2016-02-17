package jenkins.plugins.bearychat;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BallColor;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import hudson.util.LogTaskListener;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

import jenkins.model.JenkinsLocationConfiguration;

@SuppressWarnings("rawtypes")
public class ActiveNotifier implements FineGrainedNotifier {

    private static final Logger logger = Logger.getLogger(ActiveNotifier.class.getName());

    BearychatNotifier notifier;
    BuildListener listener;

    public ActiveNotifier(BearychatNotifier notifier, BuildListener listener) {
        super();
        this.notifier = notifier;
        this.listener = listener;
    }

    private BearychatService getBearychat(AbstractBuild r) {
        return notifier.newBearychatService(r, listener);
    }

    public Map<String, Object> getData(AbstractBuild build){
        JenkinsLocationConfiguration globalConfig = new JenkinsLocationConfiguration();
        String configUrl = notifier.getBuildServerUrl();

        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put("config_url", configUrl);


        AbstractProject<?, ?> project = build.getProject();
        String projectName = project.getName();
        String projectAbsoluteUrl = project.getAbsoluteUrl();
        String projectFullName = project.getFullName();
        String projectDisplayName = project.getFullDisplayName();

        Map<String, String> projectMap = new HashMap<String, String>();
        projectMap.put("name", projectName);
        projectMap.put("full_name", projectFullName);
        projectMap.put("display_name", projectDisplayName);
        projectMap.put("absolute_url", projectAbsoluteUrl);



        String id = build.getId();
        int number = build.getNumber();
        String jobFullName = build.getFullDisplayName();
        String jobDisplayName = build.getDisplayName();
        String duration = build.getDurationString();
        String status = MessageBuilder.getStatusMessage(build);
        String commitMessage = getCommitMessage(build);

        Map<String, String> jobMap = new HashMap<String, String>();
        jobMap.put("id", id);
        jobMap.put("number", number+"");
        jobMap.put("full_name", jobFullName);
        jobMap.put("display_name", jobDisplayName);
        jobMap.put("status", status);
        jobMap.put("duration", duration);
        jobMap.put("commit_message", commitMessage);

        if(notifier.includeBearychatCustomMessage()){
            String customMessage = getCustomMessage(build);
            jobMap.put("custom_message", customMessage);
        }



        ChangeLogSet changeSet = build.getChangeSet();
        List<Entry> entries = new LinkedList<Entry>();
        Set<AffectedFile> files = new HashSet<AffectedFile>();
        for (Object o : changeSet.getItems()) {
            Entry entry = (Entry) o;
            entries.add(entry);
            files.addAll(entry.getAffectedFiles());
        }


        String nonsense= "remotenonsense";
        Set<String> authors = new HashSet<String>();
        for (Entry entry : entries) {
            String displayName = entry.getAuthor().getDisplayName();
            if(!"".equals(nonsense)){
                authors.add(displayName);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("files", files.size() + "");
        result.put("authors", StringUtils.join(authors, ", "));

        result.put("project", projectMap);
        result.put("job", jobMap);
        result.put("config", configMap);

        return result;
    }

    public String getCustomMessage(AbstractBuild build){
         String customMessage = notifier.getBearychatCustomMessage();
         EnvVars envVars = new EnvVars();
         try {
             envVars = build.getEnvironment(new LogTaskListener(logger, INFO));
             customMessage = envVars.expand(customMessage);
         } catch (IOException e) {
             logger.log(SEVERE, e.getMessage(), e);
         } catch (InterruptedException e) {
             logger.log(SEVERE, e.getMessage(), e);
         }
         return customMessage;
    }

    String getChanges(AbstractBuild r) {
        if (!r.hasChangeSetComputed()) {
            logger.info("No change set computed...");
            return null;
        }
        ChangeLogSet changeSet = r.getChangeSet();
        List<Entry> entries = new LinkedList<Entry>();
        Set<AffectedFile> files = new HashSet<AffectedFile>();
        for (Object o : changeSet.getItems()) {
            Entry entry = (Entry) o;
            logger.info("Entry " + o);
            entries.add(entry);
            files.addAll(entry.getAffectedFiles());
        }
        if (entries.isEmpty()) {
            logger.info("Empty change...");
            return null;
        }
        Set<String> authors = new HashSet<String>();
        for (Entry entry : entries) {
            authors.add(entry.getAuthor().getDisplayName());
        }
        MessageBuilder message = new MessageBuilder(notifier, r);
        message.append("Started by changes from ");
        message.append(StringUtils.join(authors, ", "));
        message.append(" (");
        message.append(files.size());
        message.append(" file(s) changed)");

        return message.toString();
    }

    String getCommitMessage(AbstractBuild r) {
        ChangeLogSet changeSet = r.getChangeSet();
        List<Entry> entries = new LinkedList<Entry>();
        for (Object o : changeSet.getItems()) {
            Entry entry = (Entry) o;
            logger.info("Entry " + o);
            entries.add(entry);
        }

        if (entries.isEmpty()) {
            logger.info("Empty change...");
            Cause.UpstreamCause c = (Cause.UpstreamCause)r.getCause(Cause.UpstreamCause.class);
            if (c == null) {
                return "No Commit Changes.";
            }
            String upProjectName = c.getUpstreamProject();
            int buildNumber = c.getUpstreamBuild();
            AbstractProject project = Hudson.getInstance().getItemByFullName(upProjectName, AbstractProject.class);
            AbstractBuild upBuild = (AbstractBuild)project.getBuildByNumber(buildNumber);
            return getCommitMessage(upBuild);
        }

        StringBuffer commits = new StringBuffer();
        for (int i=0; i < 5 && i<entries.size(); i++) {
            Entry entry = entries.get(i);
            StringBuffer commit = new StringBuffer();
            commit.append(entry.getMsg());
            commit.append(" [").append(entry.getAuthor().getDisplayName()).append("]");
            commits.append("- ").append(commit.toString()).append("\n");
        }
        if(entries.size() > 5){
            int left = entries.size() - 5;
            commits.append(left).append(" more...");
        }

        return commits.toString();
    }

    static String getBuildColor(AbstractBuild r) {
        Result result = r.getResult();
        String color = null;
        if (result == Result.SUCCESS) {
            color = "blue";
        } else if (result == Result.FAILURE) {
            color = "red";
        } else if (result == Result.UNSTABLE) {
            color = "yellow";
        } else{
            color = "grey";
        }
        return color;
    }

    String getBuildStatusMessage(AbstractBuild r) {
        MessageBuilder message = new MessageBuilder(notifier, r);
        message.appendStatusMessage();
        message.appendDuration();
        message.appendOpenLink();

        if(notifier.includeBearychatCustomMessage()){
            message.appendCustomMessage();
        }

        return message.toString();
    }

    // <<<< ========== events to notify ==========
    public void deleted(AbstractBuild r) {

    }

    /**
     * start after failure is still RED color
     */
    public void started(AbstractBuild build) {
        String message = getChanges(build);

        if (message == null || "".equals(message)) {
            message = getBuildStatusMessage(build);
        }

        String action = "start";

        String color = "green";
        Run previousBuild = build.getProject().getLastBuild().getPreviousBuild();
        Result lastResult = previousBuild == null ? null : previousBuild.getResult();
        if(lastResult != null && lastResult == Result.FAILURE){
            color = "red";
        }

        Map<String, Object> dataMap = getData(build);
        dataMap.put("message", message);
        dataMap.put("color", color);
        getBearychat(build).publish(action, dataMap);
    }

    public void finalized(AbstractBuild r) {
    }

    public void completed(AbstractBuild build) {
        AbstractProject<?, ?> project = build.getProject();
        Result result = build.getResult();
        AbstractBuild<?, ?> previousBuild = project.getLastBuild();
        do {
            previousBuild = previousBuild == null ? null : previousBuild.getPreviousCompletedBuild();
        } while (previousBuild != null && previousBuild.getResult() == Result.ABORTED);
        Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;

        if ((result == Result.ABORTED && notifier.getNotifyAborted())
                || (result == Result.FAILURE //notify only on single failed build
                    && previousResult != Result.FAILURE
                    && notifier.getNotifyFailure())
                || (result == Result.FAILURE //notify only on repeated failures
                    && previousResult == Result.FAILURE)
                || (result == Result.NOT_BUILT && notifier.getNotifyNotBuilt())
                || (result == Result.SUCCESS
                    && (previousResult == Result.FAILURE || previousResult == Result.UNSTABLE)
                    && notifier.getNotifyBackToNormal())
                || (result == Result.SUCCESS && notifier.getNotifySuccess())
                || (result == Result.UNSTABLE && notifier.getNotifyUnstable())) {

            String action = "complete";
            String message = getBuildStatusMessage(build);
            String color = getBuildColor(build);
            Map<String, Object> dataMap = getData(build);
            dataMap.put("message", message);
            dataMap.put("color", color);
            getBearychat(build).publish(action, dataMap);
        }
    }
    // ========== events to notify ========== >>>>

    public static class MessageBuilder {

        private static final String STARTING_STATUS_MESSAGE = "Starting",
                                    BACK_TO_NORMAL_STATUS_MESSAGE = "Back to normal",
                                    STILL_FAILING_STATUS_MESSAGE = "Still Failing",
                                    SUCCESS_STATUS_MESSAGE = "Success",
                                    FAILURE_STATUS_MESSAGE = "Failure",
                                    ABORTED_STATUS_MESSAGE = "Aborted",
                                    NOT_BUILT_STATUS_MESSAGE = "NotBuilt",
                                    UNSTABLE_STATUS_MESSAGE = "Unstable",
                                    UNKNOWN_STATUS_MESSAGE = "Unknown";

        private StringBuffer message;
        private BearychatNotifier notifier;
        private AbstractBuild build;

        public MessageBuilder(BearychatNotifier notifier, AbstractBuild build) {
            this.notifier = notifier;
            this.message = new StringBuffer();
            this.build = build;
            startMessage();
        }

        public MessageBuilder appendStatusMessage() {
            message.append(this.escape(getStatusMessage(build)));
            return this;
        }

        static String getStatusMessage(AbstractBuild r) {
            if (r.isBuilding()) {
                return STARTING_STATUS_MESSAGE;
            }
            Result result = r.getResult();
            Result previousResult;
            Run previousBuild = r.getProject().getLastBuild().getPreviousBuild();
            Run previousSuccessfulBuild = r.getPreviousSuccessfulBuild();
            boolean buildHasSucceededBefore = previousSuccessfulBuild != null;

            /*
             * If the last build was aborted, go back to find the last non-aborted build.
             * This is so that aborted builds do not affect build transitions.
             * I.e. if build 1 was failure, build 2 was aborted and build 3 was a success the transition
             * should be failure -> success (and therefore back to normal) not aborted -> success.
             */
            Run lastNonAbortedBuild = previousBuild;
            while(lastNonAbortedBuild != null && lastNonAbortedBuild.getResult() == Result.ABORTED) {
                lastNonAbortedBuild = lastNonAbortedBuild.getPreviousBuild();
            }


            /* If all previous builds have been aborted, then use
             * SUCCESS as a default status so an aborted message is sent
             */
            if(lastNonAbortedBuild == null) {
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
                return BACK_TO_NORMAL_STATUS_MESSAGE;
            }
            if (result == Result.FAILURE && previousResult == Result.FAILURE) {
                return STILL_FAILING_STATUS_MESSAGE;
            }
            if (result == Result.SUCCESS) {
                return SUCCESS_STATUS_MESSAGE;
            }
            if (result == Result.FAILURE) {
                return FAILURE_STATUS_MESSAGE;
            }
            if (result == Result.ABORTED) {
                return ABORTED_STATUS_MESSAGE;
            }
            if (result == Result.NOT_BUILT) {
                return NOT_BUILT_STATUS_MESSAGE;
            }
            if (result == Result.UNSTABLE) {
                return UNSTABLE_STATUS_MESSAGE;
            }
            return UNKNOWN_STATUS_MESSAGE;
        }

        public MessageBuilder append(String string) {
            message.append(this.escape(string));
            return this;
        }

        public MessageBuilder append(Object string) {
            message.append(this.escape(string.toString()));
            return this;
        }

        private MessageBuilder startMessage() {
            message.append(this.escape(build.getProject().getFullDisplayName()));
            message.append(" - ");
            message.append(this.escape(build.getDisplayName()));
            message.append(" ");
            return this;
        }

        public MessageBuilder appendOpenLink() {
            String url = notifier.getBuildServerUrl() + build.getUrl();
            message.append(" ").append(url);
            return this;
        }

        public MessageBuilder appendDuration() {
            message.append(" after ");
            String durationString;
            if(message.toString().contains(BACK_TO_NORMAL_STATUS_MESSAGE)){
                durationString = createBackToNormalDurationString();
            } else {
                durationString = build.getDurationString();
            }
            message.append(durationString);
            return this;
        }

        public MessageBuilder appendCustomMessage() {
            String customMessage = notifier.getBearychatCustomMessage();
            EnvVars envVars = new EnvVars();
            try {
                envVars = build.getEnvironment(new LogTaskListener(logger, INFO));
            } catch (IOException e) {
                logger.log(SEVERE, e.getMessage(), e);
            } catch (InterruptedException e) {
                logger.log(SEVERE, e.getMessage(), e);
            }
            message.append("\n");
            message.append(envVars.expand(customMessage));
            return this;
        }

        private String createBackToNormalDurationString(){
            Run previousSuccessfulBuild = build.getPreviousSuccessfulBuild();
            long previousSuccessStartTime = previousSuccessfulBuild.getStartTimeInMillis();
            long previousSuccessDuration = previousSuccessfulBuild.getDuration();
            long previousSuccessEndTime = previousSuccessStartTime + previousSuccessDuration;
            long buildStartTime = build.getStartTimeInMillis();
            long buildDuration = build.getDuration();
            long buildEndTime = buildStartTime + buildDuration;
            long backToNormalDuration = buildEndTime - previousSuccessEndTime;
            return Util.getTimeSpanString(backToNormalDuration);
        }

        public String escape(String string) {
            string = string.replace("&", "&amp;");
            string = string.replace("<", "&lt;");
            string = string.replace(">", "&gt;");

            return string;
        }

        public String toString() {
            return message.toString();
        }
    }
}

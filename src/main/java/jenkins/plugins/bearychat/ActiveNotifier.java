package jenkins.plugins.bearychat;

import hudson.Util;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jenkins.model.JenkinsLocationConfiguration;

@SuppressWarnings("rawtypes")
public class ActiveNotifier implements FineGrainedNotifier {

    private static final Logger logger = Logger.getLogger(BearychatListener.class.getName());

    BearychatNotifier notifier;

    public ActiveNotifier(BearychatNotifier notifier) {
        super();
        this.notifier = notifier;
    }

    private BearychatService getBearychat(AbstractBuild r) {
        AbstractProject<?, ?> project = r.getProject();
        String projectRoom = Util.fixEmpty(project.getProperty(BearychatNotifier.BearychatJobProperty.class).getRoom());
        return notifier.newBearychatService(projectRoom);
    }

    public void deleted(AbstractBuild r) {
    }

    public Map<String, Object> getData(AbstractBuild build){
        JenkinsLocationConfiguration globalConfig = new JenkinsLocationConfiguration();
        String configUrl = globalConfig.getUrl();
        String configName = globalConfig.getDisplayName();

        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put("config_url", configUrl);
        configMap.put("config_Name", configName);


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

        Map<String, String> jobMap = new HashMap<String, String>();
        jobMap.put("id", id);
        jobMap.put("number", number+"");
        jobMap.put("full_name", jobFullName);
        jobMap.put("display_name", jobDisplayName);
        jobMap.put("status", status);
        jobMap.put("duration", duration);



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

    public void started(AbstractBuild build) {
        String changes = getChanges(build);
        CauseAction cause = build.getAction(CauseAction.class);

        if (changes != null) {
            notifyStart(build, changes);
        } else if (cause != null) {
            MessageBuilder message = new MessageBuilder(notifier, build);
            message.append(cause.getShortDescription());
            notifyStart(build, message.appendOpenLink().toString());
        } else {
            notifyStart(build, getBuildStatusMessage(build));
        }
    }

    private void notifyStart(AbstractBuild build, String message) {
    String action = "start";
    Map<String, Object> dataMap = getData(build);
    dataMap.put("message", message);
    dataMap.put("color", "good");
        getBearychat(build).publish(action, dataMap);
    }

    public void finalized(AbstractBuild r) {
    }

    public void completed(AbstractBuild build) {
        AbstractProject<?, ?> project = build.getProject();
        BearychatNotifier.BearychatJobProperty jobProperty = project.getProperty(BearychatNotifier.BearychatJobProperty.class);
        Result result = build.getResult();
        AbstractBuild<?, ?> previousBuild = project.getLastBuild().getPreviousBuild();
        Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;
        if ((result == Result.ABORTED && jobProperty.getNotifyAborted())
                || (result == Result.FAILURE && jobProperty.getNotifyFailure())
                || (result == Result.NOT_BUILT && jobProperty.getNotifyNotBuilt())
                || (result == Result.SUCCESS && previousResult == Result.FAILURE && jobProperty.getNotifyBackToNormal())
                || (result == Result.SUCCESS && jobProperty.getNotifySuccess())
                || (result == Result.UNSTABLE && jobProperty.getNotifyUnstable())) {

        String action = "complete";
        String message = getBuildStatusMessage(build);
        String color = getBuildColor(build);
        Map<String, Object> dataMap = getData(build);
        dataMap.put("message", message);
        dataMap.put("color", color);
            getBearychat(build).publish(action, dataMap);
        }
    }

    public String getChanges(AbstractBuild r) {
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
        return message.appendOpenLink().toString();
    }

    static String getBuildColor(AbstractBuild r) {
        Result result = r.getResult();
        if (result == Result.SUCCESS) {
            return "good";
        } else if (result == Result.FAILURE) {
            return "danger";
        } else {
            return "warning";
        }
    }

    String getBuildStatusMessage(AbstractBuild r) {
        MessageBuilder message = new MessageBuilder(notifier, r);
        message.appendStatusMessage();
        message.appendDuration();
        return message.appendOpenLink().toString();
    }

    public static class MessageBuilder {
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
                return "Starting...";
            }
            Result result = r.getResult();
            Run previousBuild = r.getProject().getLastBuild().getPreviousBuild();
            Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;
            if (result == Result.SUCCESS && previousResult == Result.FAILURE) return "Back to normal";
            if (result == Result.SUCCESS) return "Success";
            if (result == Result.FAILURE) return "Failure";
            if (result == Result.ABORTED) return "Aborted";
            if (result == Result.NOT_BUILT) return "Not built";
            if (result == Result.UNSTABLE) return "Unstable";
            return "Unknown";
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
            message.append(this.escape(build.getProject().getDisplayName()));
            message.append(" - ");
            message.append(this.escape(build.getDisplayName()));
            message.append(" ");
            return this;
        }

        public MessageBuilder appendOpenLink() {
            String url = notifier.getBuildServerUrl() + build.getUrl();
            message.append(" (<").append(url).append("|Open>)");
            return this;
        }

        public MessageBuilder appendDuration() {
            message.append(" after ");
            message.append(build.getDurationString());
            return this;
        }

        public String escape(String string){
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

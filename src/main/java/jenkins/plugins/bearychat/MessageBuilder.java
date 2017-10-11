package jenkins.plugins.bearychat;

import java.util.*;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.scm.ChangeLogSet;
import org.apache.commons.lang.StringUtils;
import jenkins.plugins.bearychat.Messages;

public class MessageBuilder {

    private AbstractBuild build;
    private BearyChatNotifier notifier;
    private Map<String, String> project;
    private Map<String, String> job;
    private Map<String, String> files;

    public MessageBuilder(BearyChatNotifier notifier, AbstractBuild build) {
        this.build = build;
        this.notifier = notifier;

        this.project = this.getProject(build);
        this.job = this.getJob(build);
        this.files = this.getFiles(build);
    }

    private String getJobURL(AbstractBuild build) {
        AbstractProject<?, ?> project = build.getProject();
        String projectURL = project.getAbsoluteUrl();

        if (projectURL.endsWith("/")) {
            return projectURL + build.getNumber();
        } else {
            return projectURL + "/" + build.getNumber();
        }
    }

    public Map<String, String> getProject(AbstractBuild build) {
        AbstractProject<?, ?> project = build.getProject();

        Map<String, String> projectData = new HashMap<String, String>();
        projectData.put("name", project.getName());
        projectData.put("url", project.getAbsoluteUrl());
        projectData.put("display_name", project.getDisplayName());
        projectData.put("full_name", project.getFullName());

        return projectData;
    }

    public Map<String, String> getJob(AbstractBuild build) {
        Map<String, String> jobData = new HashMap<String, String>();
        String number = Integer.toString(build.getNumber());
        jobData.put("id", build.getId());
        jobData.put("number", number);
        jobData.put("full_name", build.getFullDisplayName());
        jobData.put("display_name", build.getDisplayName());
        jobData.put("duration", build.getDurationString());
        jobData.put("status", Helper.getStatusByBuild(build));
        jobData.put("commits", Helper.getCommitMessages(build));
        jobData.put("url", getJobURL(build));

        if (this.notifier.isIncludeCustomMessage()) {
            jobData.put("custom_start_message", Helper.getCustomStartMessage(this.notifier, build));
            jobData.put("custom_end_message", Helper.getCustomEndMessage(this.notifier, build));
        }

        return jobData;
    }

    public Map<String, String> getFiles(AbstractBuild build) {
        ChangeLogSet changeSet = build.getChangeSet();
        List<ChangeLogSet.Entry> entries = new LinkedList<ChangeLogSet.Entry>();
        Set<ChangeLogSet.AffectedFile> files = new HashSet<ChangeLogSet.AffectedFile>();
        for (Object o : changeSet.getItems()) {
            ChangeLogSet.Entry entry = (ChangeLogSet.Entry) o;
            entries.add(entry);
            files.addAll(entry.getAffectedFiles());
        }

        Set<String> authors = new HashSet<String>();
        for (ChangeLogSet.Entry entry : entries) {
            String displayName = entry.getAuthor().getDisplayName();
            authors.add(displayName);
        }

        Map<String, String> result = new HashMap<String, String>();
        result.put("count", Integer.toString(files.size()));
        result.put("authors", StringUtils.join(authors, ", "));

        return result;
    }

    public String getStartedMessage() {
        String projectName = this.project.get("display_name");
        String projectURL = this.project.get("url");

        String jobName = this.job.get("display_name");
        String jobURL = this.job.get("url");

        return Messages.JobStartedMessage(projectName, projectURL, jobName, jobURL);
    }

    public String getStartedFallback() {
        String projectName = this.project.get("display_name");
        String jobName = this.job.get("display_name");

        String authors = this.files.get("authors");

        if (authors.length() > 0) {
            authors = " by " + authors;
        }

        return Messages.JobStartedFallback(projectName, jobName, authors);
    }

    public String getStartedText() {
        StringBuffer text = new StringBuffer();
        if (this.notifier.isIncludeCustomMessage()) {
            String customStartMessage = this.job.get("custom_start_message");
            text.append(customStartMessage);
            text.append("\n");
        }

        text.append(this.files.get("count"));
        text.append(" file(s) changed");

        String authors = this.files.get("authors");
        if (authors.length() > 0) {
            text.append(" - ");
            text.append(authors);
        }

        return text.toString();
    }


    public String getCompletedMessage() {
        String projectName = this.project.get("display_name");
        String projectURL = this.project.get("url");

        String jobName = this.job.get("display_name");
        String jobURL = this.job.get("url");

        String statusWithDuration = Helper.getBuildStatusMessage(this.notifier, this.build);

        return Messages.JobCompletedMessage(projectName, projectURL, jobName, jobURL, statusWithDuration);
    }

    public String getCompletedFallback() {
        String projectName = this.project.get("display_name");
        String jobName = this.job.get("display_name");
        String statusWithDuration = Helper.getBuildStatusMessage(this.notifier, this.build);

        return Messages.JobCompletedFallback(projectName, jobName, statusWithDuration);
    }

    public String getCompletedText() {
        StringBuffer text = new StringBuffer();
        if (this.notifier.isIncludeCustomMessage()) {
            String customEndMessage = this.job.get("custom_end_message");
            text.append(customEndMessage);
            text.append("\n");
        }

        String commits = this.job.get("commits");

        if (commits != null && commits.length() > 0) {
            text.append(commits);
        } else {
            text.append("No Commits Changes");
        }

        return text.toString();
    }
}

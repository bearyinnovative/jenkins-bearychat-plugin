package jenkins.plugins.bearychat.workflow;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import net.sf.json.JSONArray;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.AbortException;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;

import jenkins.model.Jenkins;
import jenkins.plugins.bearychat.Messages;
import jenkins.plugins.bearychat.BearyChatNotifier;
import jenkins.plugins.bearychat.BearyChatService;
import jenkins.plugins.bearychat.StandardBearyChatService;
import jenkins.plugins.bearychat.Helper;

/**
 * Workflow step to send a BearyChat channel notification.
 */
public class BearyChatSendStep extends AbstractStepImpl {

    private String message;
    private String notification;
    private String title;
    private String url;
    private String attachmentText;
    private String color;
    private String channel;
    private String webhook;
    private boolean failOnError;

    public String getMessage() {
        return message;
    }

    public String getAttachmentText() {
        return attachmentText;
    }

    @DataBoundSetter
    public void setAttachmentText(String text) {
        this.attachmentText = Util.fixEmpty(text);
    }

    public String getTitle() {
        return this.title;
    }

    public String getNotification() {
        return this.notification;
    }

    @DataBoundSetter
    public void setNotification(String notification) {
        this.notification = notification;
    }

    @DataBoundSetter
    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    public String getColor() {
        return color;
    }

    @DataBoundSetter
    public void setColor(String color) {
        this.color = Util.fixEmpty(color);
    }

    public String getWebhook() {
        return webhook;
    }

    @DataBoundSetter
    public void setWebhook(String webhook) {
        this.webhook = Util.fixEmpty(webhook);
    }

    public String getChannel() {
        return channel;
    }

    @DataBoundSetter
    public void setChannel(String channel) {
        this.channel = Util.fixEmpty(channel);
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    @DataBoundSetter
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @DataBoundConstructor
    public BearyChatSendStep(@Nonnull String message) {
        this.message = message;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(BearyChatSendStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "bearychatSend";
        }

        @Override
        public String getDisplayName() {
            return Messages.BearyChatSendStepDisplayName();
        }
    }

    public static class BearyChatSendStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        @Inject
        transient BearyChatSendStep step;

        @StepContextParameter
        transient TaskListener listener;

        protected JSONObject buildData (String message, String title, String attachmenText, String notification, String url, String color) {
            JSONObject data = new JSONObject();
            JSONObject attachment = new JSONObject();
            JSONArray attachments = new JSONArray();

            if (title != null) {
                attachment.put("title", title);
            }

            if (attachmenText != null) {
                attachment.put("text", attachmenText);
            }

            if (color != null) {
                attachment.put("color", color);
            }

            if (url != null) {
                attachment.put("url", url);
            }

            attachments.add(attachment);

            if (message != null) {
                data.put("text", message);
            }

            if (notification != null) {
                data.put("fallback", notification);
            }

            data.put("attachments", attachments);

            return data;
        }

        @Override
        protected Void run() throws Exception {

            // default to global config values if not set in step, but allow step to override all global settings
            Jenkins jenkins;
            // Jenkins.getInstance() may return null, no message sent in that case
            try {
                jenkins = Jenkins.getInstance();
            } catch (NullPointerException ne) {
                listener.error(Messages.NotificationFailedWithException(ne));
                return null;
            }
            if (jenkins == null) {
                return null;
            }

            BearyChatNotifier.DescriptorImpl bearychatDesc = jenkins.getDescriptorByType(BearyChatNotifier.DescriptorImpl.class);
            String webhook = step.getWebhook() != null ? step.getWebhook() : bearychatDesc.getWebhook();
            String channel = step.getChannel() != null ? step.getChannel() : bearychatDesc.getChannel();
            String color = step.getColor() != null ? step.getColor() : Helper.COLOR_GREY;
            String message = step.getMessage();
            String url = step.getUrl();
            String title = step.getTitle();
            String notifition = step.getNotification();
            String attachmentText = step.getAttachmentText();
            Boolean failOnError = step.isFailOnError();

            JSONObject data = buildData(message, title, attachmentText, notifition, url, color);

            listener.getLogger().println(Messages.BearyChatSendStepConfig(webhook, channel));

            BearyChatService bearychatService = getBearyChatService(webhook, channel);
            boolean publishSuccess = bearychatService.publish(data);

            if (!publishSuccess && failOnError) {
                throw new AbortException(Messages.NotificationFailed());
            } else if (!publishSuccess) {
                listener.error(Messages.NotificationFailed());
            }
            return null;
        }
        //streamline unit testing

        BearyChatService getBearyChatService(String webhook, String channel) {
            return new StandardBearyChatService(webhook, channel);
        }
    }
}

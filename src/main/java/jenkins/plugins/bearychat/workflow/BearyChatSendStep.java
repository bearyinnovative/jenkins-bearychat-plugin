package jenkins.plugins.bearychat.workflow;

import javax.annotation.Nonnull;
import javax.inject.Inject;
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

import jenkins.model.Jenkins;
import jenkins.plugins.bearychat.Messages;
import jenkins.plugins.bearychat.BearyChatNotifier;
import jenkins.plugins.bearychat.BearyChatService;
import jenkins.plugins.bearychat.StandardBearyChatService;

/**
 * Workflow step to send a BearyChat channel notification.
 */
public class BearyChatSendStep extends AbstractStepImpl {

    private final @Nonnull String message;
    private String color;
    private boolean botUser;
    private String channel;
    private String webhook;
    private boolean failOnError;

    @Nonnull
    public String getMessage() {
        return message;
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

    public boolean getBotUser() {
        return botUser;
    }

    @DataBoundSetter
    public void setBotUser(boolean botUser) {
        this.botUser = botUser;
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
            listener.getLogger().println("run BearyChatSendStep, step " + step.webhook + " : " + step.botUser + ", desc ");
            String webhook = step.webhook != null ? step.webhook : bearychatDesc.getWebhook();
            String channel = step.channel != null ? step.channel : bearychatDesc.getChannel();
            String color = step.color != null ? step.color : "";

            // placing in console log to simplify testing of retrieving values from global config or from step field; also used for tests
            listener.getLogger().println(Messages.BearyChatSendStepConfig(step.webhook == null, step.channel == null, step.color == null));

            BearyChatService bearychatService = getBearyChatService(webhook, channel);
            boolean publishSuccess = bearychatService.publish("workflow", step.message, color);
            if (!publishSuccess && step.failOnError) {
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

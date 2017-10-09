package jenkins.plugins.bearychat;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class BearyChatNotifier extends Notifier {

    private static final Logger logger = Logger.getLogger(BearyChatNotifier.class.getName());

    private String webhook;
    private String buildServerUrl;
    private String channel;
    private String sendAs;
    private boolean startNotification;
    private boolean notifySuccess;
    private boolean notifyAborted;
    private boolean notifyNotBuilt;
    private boolean notifyUnstable;
    private boolean notifyFailure;
    private boolean notifyBackToNormal;
    private boolean notifyRepeatedFailure;
    private boolean includeBearyChatCustomMessage;
    private String bearychatCustomMessage;
    private String bearychatEndCustomMessage;

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getWebhook() {
        return webhook;
    }

    public String getChannel() {
        return channel;
    }

    public String getBuildServerUrl() {
        if(StringUtils.isEmpty(buildServerUrl)) {
            JenkinsLocationConfiguration jenkinsConfig = new JenkinsLocationConfiguration();
            return jenkinsConfig.getUrl();
        } else {
            return buildServerUrl;
        }
    }

    public String getSendAs() {
        return sendAs;
    }

    public boolean getStartNotification() {
        return startNotification;
    }

    public boolean getNotifySuccess() {
        return notifySuccess;
    }

    public boolean getNotifyAborted() {
        return notifyAborted;
    }

    public boolean getNotifyFailure() {
        return notifyFailure;
    }

    public boolean getNotifyNotBuilt() {
        return notifyNotBuilt;
    }

    public boolean getNotifyUnstable() {
        return notifyUnstable;
    }

    public boolean getNotifyBackToNormal() {
        return notifyBackToNormal;
    }

    public boolean includeBearyChatCustomMessage() {
        return includeBearyChatCustomMessage;
    }

    public String getBearyChatCustomMessage() {
        return bearychatCustomMessage;
    }

    public String getBearyChatEndCustomMessage() {
        return bearychatEndCustomMessage;
    }

    @DataBoundConstructor
    public BearyChatNotifier(final String webhook, final String channel, final String buildServerUrl,
                             final String sendAs, final boolean startNotification, final boolean notifyAborted, final boolean notifyFailure,
                             final boolean notifyNotBuilt, final boolean notifySuccess, final boolean notifyUnstable, final boolean notifyBackToNormal,
                             boolean includeBearyChatCustomMessage, String bearychatCustomMessage, String bearychatEndCustomMessage) {
        super();
        this.webhook = webhook;
        this.buildServerUrl = buildServerUrl;
        this.channel = channel;
        this.sendAs = sendAs;
        this.startNotification = startNotification;
        this.notifyAborted = notifyAborted;
        this.notifyFailure = notifyFailure;
        this.notifyNotBuilt = notifyNotBuilt;
        this.notifySuccess = notifySuccess;
        this.notifyUnstable = notifyUnstable;
        this.notifyBackToNormal = notifyBackToNormal;
        this.includeBearyChatCustomMessage = includeBearyChatCustomMessage;
        this.bearychatCustomMessage = bearychatCustomMessage;
        this.bearychatEndCustomMessage = bearychatEndCustomMessage;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public BearyChatService newBearyChatService(AbstractBuild r, BuildListener listener) {
        String webhook = this.webhook;
        if (StringUtils.isEmpty(webhook)) {
            webhook = getDescriptor().getWebhook();
        }

        String channel = this.channel;
        if (StringUtils.isEmpty(channel)) {
            channel = getDescriptor().getChannel();
        }

        EnvVars env = null;
        try {
            env = r.getEnvironment(listener);
        } catch (Exception e) {
            listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
            env = new EnvVars();
        }
        webhook = env.expand(webhook);
        channel = env.expand(channel);

        return new StandardBearyChatService(webhook, channel);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        if (startNotification) {
            Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
            for (Publisher publisher : map.values()) {
                if (publisher instanceof BearyChatNotifier) {
                    logger.info("Invoking Started...");
                    new ActiveNotifier((BearyChatNotifier) publisher, listener).started(build);
                }
            }
        }
        return super.prebuild(build, listener);
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String webhook;
        private String channel;
        private String buildServerUrl;
        private String sendAs;

        public DescriptorImpl() {
            load();
        }

        public String getWebhook() {
            return webhook;
        }

        public String getChannel() {
            return channel;
        }

        public String getBuildServerUrl() {
            if(StringUtils.isEmpty(buildServerUrl)) {
                JenkinsLocationConfiguration jenkinsConfig = new JenkinsLocationConfiguration();
                return jenkinsConfig.getUrl();
            }
            else {
                return buildServerUrl;
            }
        }

        public String getSendAs() {
            return sendAs;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public BearyChatNotifier newInstance(StaplerRequest sr, JSONObject json) {
            String webhook = sr.getParameter("bearychatWebhook");
            String channel = sr.getParameter("bearychatChannel");
            boolean startNotification = "true".equals(sr.getParameter("bearychatStartNotification"));
            boolean notifySuccess = "true".equals(sr.getParameter("bearychatNotifySuccess"));
            boolean notifyAborted = "true".equals(sr.getParameter("bearychatNotifyAborted"));
            boolean notifyNotBuilt = "true".equals(sr.getParameter("bearychatNotifyNotBuilt"));
            boolean notifyUnstable = "true".equals(sr.getParameter("bearychatNotifyUnstable"));
            boolean notifyFailure = "true".equals(sr.getParameter("bearychatNotifyFailure"));
            boolean notifyBackToNormal = "true".equals(sr.getParameter("bearychatNotifyBackToNormal"));
            boolean includeBearyChatCustomMessage = "on".equals(sr.getParameter("includeBearyChatCustomMessage"));
            String bearychatCustomMessage = sr.getParameter("bearychatCustomMessage");
            String bearychatEndCustomMessage = sr.getParameter("bearychatEndCustomMessage");
            return new BearyChatNotifier(webhook, channel, buildServerUrl, sendAs, startNotification, notifyAborted,
                    notifyFailure, notifyNotBuilt, notifySuccess, notifyUnstable, notifyBackToNormal,
                    includeBearyChatCustomMessage, bearychatCustomMessage, bearychatEndCustomMessage);
        }

        @Override
        public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
            webhook = sr.getParameter("bearychatWebhook");
            channel = sr.getParameter("bearychatChannel");
            buildServerUrl = sr.getParameter("bearychatBuildServerUrl");
            sendAs = sr.getParameter("bearychatSendAs");
            if(StringUtils.isEmpty(buildServerUrl)) {
                JenkinsLocationConfiguration jenkinsConfig = new JenkinsLocationConfiguration();
                buildServerUrl = jenkinsConfig.getUrl();
            }
            if (buildServerUrl != null && !buildServerUrl.endsWith("/")) {
                buildServerUrl = buildServerUrl + "/";
            }
            save();
            return super.configure(sr, formData);
        }

        BearyChatService getBearyChatService(final String webhoo, final String channel) {
            return new StandardBearyChatService(webhook, channel);
        }

        @Override
        public String getDisplayName() {
            return "BearyChat Notifications";
        }

        public FormValidation doTestConnection(@QueryParameter("bearychatWebhook") final String webhook,
                                               @QueryParameter("bearychatChannel") final String channel,
                                               @QueryParameter("bearychatBuildServerUrl") final String buildServerUrl) throws FormException {
            try {
                String targetWebhook = webhook;
                if (StringUtils.isEmpty(webhook)) {
                    targetWebhook = this.webhook;
                }
                String targetChannel = channel;
                if (StringUtils.isEmpty(targetChannel)) {
                    targetChannel = this.channel;
                }
                String targetBuildServerUrl = buildServerUrl;
                if (StringUtils.isEmpty(targetBuildServerUrl)) {
                    targetBuildServerUrl = this.buildServerUrl;
                }
                BearyChatService testBearyChatService = getBearyChatService(targetWebhook, targetChannel);
                String message = "BearyChat Jenkins Plugin has been configured correctly. " + targetBuildServerUrl;
                boolean success = testBearyChatService.publish("ping", message, "green");
                return success ? FormValidation.ok("Success") : FormValidation.error("Failure");
            } catch (Exception e) {
                return FormValidation.error("Client error : " + e.getMessage());
            }
        }
    }
}

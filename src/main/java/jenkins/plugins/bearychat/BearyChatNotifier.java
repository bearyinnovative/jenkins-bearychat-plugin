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
    private String customStartMessage;
    private String customEndMessage;
    private boolean isNotifyOnStarting;
    private boolean isNotifyOnSuccess;
    private boolean isNotifyOnAborted;
    private boolean isNotifyOnNotBuilt;
    private boolean isNotifyOnUnstable;
    private boolean isNotifyOnFailure;
    private boolean isNotifyOnBackToNormal;
    private boolean isNotifyRepeatedFailure;
    private boolean isIncludeCustomMessage;

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

    public boolean isNotifyOnStarting() {
        return this.isNotifyOnStarting;
    }

    public boolean isNotifyOnSuccess() {
        return this.isNotifyOnSuccess;
    }

    public boolean isNotifyOnAborted() {
        return this.isNotifyOnAborted;
    }

    public boolean isNotifyOnFailure() {
        return this.isNotifyOnFailure;
    }

    public boolean isNotifyOnNotBuilt() {
        return this.isNotifyOnNotBuilt;
    }

    public boolean isNotifyOnUnstable() {
        return this.isNotifyOnUnstable;
    }

    public boolean isNotifyOnBackToNormal() {
        return this.isNotifyOnBackToNormal;
    }

    public boolean isIncludeCustomMessage() {
        return this.isIncludeCustomMessage;
    }

    public String getCustomStartMessage() {
        return customStartMessage;
    }

    public String getCustomEndMessage() {
        return customEndMessage;
    }

    @DataBoundConstructor
    public BearyChatNotifier(final String webhook, final String channel, final String buildServerUrl,
                             final boolean isNotifyOnStarting, final boolean isNotifyOnAborted, final boolean isNotifyOnFailure,
                             final boolean isNotifyOnNotBuilt, final boolean isNotifyOnSuccess, final boolean isNotifyOnUnstable,
                             final boolean isNotifyOnBackToNormal, boolean isIncludeCustomMessage,
                             String customStartMessage, String customEndMessage) {
        super();
        this.webhook = webhook;
        this.buildServerUrl = buildServerUrl;
        this.channel = channel;
        this.isNotifyOnStarting = isNotifyOnStarting;
        this.isNotifyOnAborted = isNotifyOnAborted;
        this.isNotifyOnFailure = isNotifyOnFailure;
        this.isNotifyOnNotBuilt = isNotifyOnNotBuilt;
        this.isNotifyOnSuccess = isNotifyOnSuccess;
        this.isNotifyOnUnstable = isNotifyOnUnstable;
        this.isNotifyOnBackToNormal = isNotifyOnBackToNormal;
        this.isIncludeCustomMessage = isIncludeCustomMessage;
        this.customStartMessage = customStartMessage;
        this.customEndMessage = customEndMessage;
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

        logger.info("webhook: " + webhook);
        return new StandardBearyChatService(webhook, channel);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        if (isNotifyOnStarting) {
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

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public BearyChatNotifier newInstance(StaplerRequest sr, JSONObject json) {
            String webhook = sr.getParameter("webhook");
            String channel = sr.getParameter("channel");
            boolean isNotifyOnStarting = "true".equals(sr.getParameter("isNotifyOnStarting"));
            boolean isNotifyOnSuccess = "true".equals(sr.getParameter("isNotifyOnSuccess"));
            boolean isNotifyOnAborted = "true".equals(sr.getParameter("isNotifyOnAborted"));
            boolean isNotifyOnNotBuilt = "true".equals(sr.getParameter("isNotifyOnNotBuilt"));
            boolean isNotifyOnUnstable = "true".equals(sr.getParameter("isNotifyOnUnstable"));
            boolean isNotifyOnFailure = "true".equals(sr.getParameter("isNotifyOnFailure"));
            boolean isNotifyOnBackToNormal = "true".equals(sr.getParameter("isNotifyOnBackToNormal"));
            boolean isIncludeCustomMessage = "on".equals(sr.getParameter("isIncludeCustomMessage"));
            String customStartMessage = sr.getParameter("customStartMessage");
            String customEndMessage = sr.getParameter("customEndMessage");
            return new BearyChatNotifier(webhook, channel, buildServerUrl, isNotifyOnStarting, isNotifyOnAborted,
                    isNotifyOnFailure, isNotifyOnNotBuilt, isNotifyOnSuccess, isNotifyOnUnstable, isNotifyOnBackToNormal,
                    isIncludeCustomMessage, customStartMessage, customEndMessage);
        }

        @Override
        public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
            webhook = sr.getParameter("webhook");
            channel = sr.getParameter("channel");
            buildServerUrl = sr.getParameter("buildServerUrl");
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

        BearyChatService getBearyChatService(final String webhook, final String channel) {
            return new StandardBearyChatService(webhook, channel);
        }

        @Override
        public String getDisplayName() {
            return "BearyChat Notifications";
        }

        public FormValidation doTestConnection(@QueryParameter("webhook") final String webhook,
                                               @QueryParameter("channel") final String channel,
                                               @QueryParameter("buildServerUrl") final String buildServerUrl) throws FormException {
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
                boolean success = testBearyChatService.publish(message);
                return success ? FormValidation.ok("Success") : FormValidation.error("Failure");
            } catch (Exception e) {
                return FormValidation.error("Client error : " + e.getMessage());
            }
        }
    }
}

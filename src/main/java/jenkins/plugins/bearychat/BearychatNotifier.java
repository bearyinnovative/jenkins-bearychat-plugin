package jenkins.plugins.bearychat;

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

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class BearychatNotifier extends Notifier {

    private static final Logger logger = Logger.getLogger(BearychatNotifier.class.getName());

    private String teamDomain;
    private String authToken;
    private String buildServerUrl;
    private String room;
    private String sendAs;
    private boolean startNotification;
    private boolean notifySuccess;
    private boolean notifyAborted;
    private boolean notifyNotBuilt;
    private boolean notifyUnstable;
    private boolean notifyFailure;
    private boolean notifyBackToNormal;
    private boolean notifyRepeatedFailure;
    private boolean includeCustomMessage;
    private String customMessage;

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getTeamDomain() {
        return teamDomain;
    }

    public String getRoom() {
        return room;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getBuildServerUrl() {
        if(buildServerUrl == null || buildServerUrl == "") {
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

    public boolean includeCustomMessage() {
        return includeCustomMessage;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    @DataBoundConstructor
    public BearychatNotifier(final String teamDomain, final String authToken, final String room, final String buildServerUrl,
                             final String sendAs, final boolean startNotification, final boolean notifyAborted, final boolean notifyFailure,
                             final boolean notifyNotBuilt, final boolean notifySuccess, final boolean notifyUnstable, final boolean notifyBackToNormal,
                             boolean includeCustomMessage, String customMessage) {
        super();
        this.teamDomain = teamDomain;
        this.authToken = authToken;
        this.buildServerUrl = buildServerUrl;
        this.room = room;
        this.sendAs = sendAs;
        this.startNotification = startNotification;
        this.notifyAborted = notifyAborted;
        this.notifyFailure = notifyFailure;
        this.notifyNotBuilt = notifyNotBuilt;
        this.notifySuccess = notifySuccess;
        this.notifyUnstable = notifyUnstable;
        this.notifyBackToNormal = notifyBackToNormal;
        this.includeCustomMessage = includeCustomMessage;
        this.customMessage = customMessage;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public BearychatService newBearychatService(AbstractBuild r, BuildListener listener) {
        String teamDomain = this.teamDomain;
        if (StringUtils.isEmpty(teamDomain)) {
            teamDomain = getDescriptor().getTeamDomain();
        }
        String authToken = this.authToken;
        if (StringUtils.isEmpty(authToken)) {
            authToken = getDescriptor().getToken();
        }
        String room = this.room;
        if (StringUtils.isEmpty(room)) {
            room = getDescriptor().getRoom();
        }

        EnvVars env = null;
        try {
            env = r.getEnvironment(listener);
        } catch (Exception e) {
            listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
            env = new EnvVars();
        }
        teamDomain = env.expand(teamDomain);
        authToken = env.expand(authToken);
        room = env.expand(room);

        return new StandardBearychatService(teamDomain, authToken, room);
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
                if (publisher instanceof BearychatNotifier) {
                    logger.info("Invoking Started...");
                    new ActiveNotifier((BearychatNotifier) publisher, listener).started(build);
                }
            }
        }
        return super.prebuild(build, listener);
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String teamDomain;
        private String token;
        private String room;
        private String buildServerUrl;
        private String sendAs;

        public DescriptorImpl() {
            load();
        }

        public String getTeamDomain() {
            return teamDomain;
        }

        public String getToken() {
            return token;
        }

        public String getRoom() {
            return room;
        }

        public String getBuildServerUrl() {
            if(buildServerUrl == null || buildServerUrl == "") {
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
        public BearychatNotifier newInstance(StaplerRequest sr, JSONObject json) {
            String teamDomain = sr.getParameter("bearychatTeamDomain");
            String token = sr.getParameter("bearychatToken");
            String room = sr.getParameter("bearychatRoom");
            boolean startNotification = "true".equals(sr.getParameter("bearychatStartNotification"));
            boolean notifySuccess = "true".equals(sr.getParameter("bearychatNotifySuccess"));
            boolean notifyAborted = "true".equals(sr.getParameter("bearychatNotifyAborted"));
            boolean notifyNotBuilt = "true".equals(sr.getParameter("bearychatNotifyNotBuilt"));
            boolean notifyUnstable = "true".equals(sr.getParameter("bearychatNotifyUnstable"));
            boolean notifyFailure = "true".equals(sr.getParameter("bearychatNotifyFailure"));
            boolean notifyBackToNormal = "true".equals(sr.getParameter("bearychatNotifyBackToNormal"));
            boolean includeCustomMessage = "on".equals(sr.getParameter("includeCustomMessage"));
            String customMessage = sr.getParameter("customMessage");
            return new BearychatNotifier(teamDomain, token, room, buildServerUrl, sendAs, startNotification, notifyAborted,
                    notifyFailure, notifyNotBuilt, notifySuccess, notifyUnstable, notifyBackToNormal, includeCustomMessage, customMessage);
        }

        @Override
        public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
            teamDomain = sr.getParameter("bearychatTeamDomain");
            token = sr.getParameter("bearychatToken");
            room = sr.getParameter("bearychatRoom");
            buildServerUrl = sr.getParameter("bearychatBuildServerUrl");
            sendAs = sr.getParameter("bearychatSendAs");
            if(buildServerUrl == null || buildServerUrl == "") {
                JenkinsLocationConfiguration jenkinsConfig = new JenkinsLocationConfiguration();
                buildServerUrl = jenkinsConfig.getUrl();
            }
            if (buildServerUrl != null && !buildServerUrl.endsWith("/")) {
                buildServerUrl = buildServerUrl + "/";
            }
            save();
            return super.configure(sr, formData);
        }

        BearychatService getBearychatService(final String teamDomain, final String authToken, final String room) {
            return new StandardBearychatService(teamDomain, authToken, room);
        }

        @Override
        public String getDisplayName() {
            return "BearyChat Notifications";
        }

        public FormValidation doTestConnection(@QueryParameter("bearychatTeamDomain") final String teamDomain,
                                               @QueryParameter("bearychatToken") final String authToken,
                                               @QueryParameter("bearychatRoom") final String room,
                                               @QueryParameter("bearychatBuildServerUrl") final String buildServerUrl) throws FormException {
            try {
                String targetDomain = teamDomain;
                if (StringUtils.isEmpty(targetDomain)) {
                    targetDomain = this.teamDomain;
                }
                String targetToken = authToken;
                if (StringUtils.isEmpty(targetToken)) {
                    targetToken = this.token;
                }
                String targetRoom = room;
                if (StringUtils.isEmpty(targetRoom)) {
                    targetRoom = this.room;
                }
                String targetBuildServerUrl = buildServerUrl;
                if (StringUtils.isEmpty(targetBuildServerUrl)) {
                    targetBuildServerUrl = this.buildServerUrl;
                }
                BearychatService testBearychatService = getBearychatService(targetDomain, targetToken, targetRoom);
                String message = "BearyChat Jenkins Plugin has been configured correctly. " + targetBuildServerUrl;
                boolean success = testBearychatService.publish(message);
                return success ? FormValidation.ok("Success") : FormValidation.error("Failure");
            } catch (Exception e) {
                return FormValidation.error("Client error : " + e.getMessage());
            }
        }
    }
}

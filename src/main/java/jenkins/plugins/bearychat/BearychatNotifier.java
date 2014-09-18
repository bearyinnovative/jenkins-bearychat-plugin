package jenkins.plugins.bearychat;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings({"unchecked"})
public class BearychatNotifier extends Notifier {

    private static final Logger logger = Logger.getLogger(BearychatNotifier.class.getName());

    private String teamDomain;
    private String authToken;
    private String buildServerUrl;
    private String room;
    private String sendAs;

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
        return buildServerUrl;
    }

    public String getSendAs() {
        return sendAs;
    }


    @DataBoundConstructor
    public BearychatNotifier(final String teamDomain, final String authToken, final String room, String buildServerUrl, final String sendAs) {
        super();
        this.teamDomain = teamDomain;
        this.authToken = authToken;
        this.buildServerUrl = buildServerUrl;
        this.room = room;
        this.sendAs = sendAs;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    public BearychatService newBearychatService(final String room) {
        return new StandardBearychatService(getTeamDomain(), getAuthToken(), room == null ? getRoom() : room);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
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
            return buildServerUrl;
        }

        public String getSendAs() {
            return sendAs;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public BearychatNotifier newInstance(StaplerRequest sr) {
            if (teamDomain == null) teamDomain = sr.getParameter("bearychatTeamDomain");
            if (token == null) token = sr.getParameter("bearychatToken");
            if (buildServerUrl == null) buildServerUrl = sr.getParameter("bearychatBuildServerUrl");
            if (room == null) room = sr.getParameter("bearychatRoom");
            if (sendAs == null) sendAs = sr.getParameter("bearychatSendAs");
            return new BearychatNotifier(teamDomain, token, room, buildServerUrl, sendAs);
        }

        @Override
        public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
            teamDomain = sr.getParameter("bearychatTeamDomain");
            token = sr.getParameter("bearychatToken");
            room = sr.getParameter("bearychatRoom");
            buildServerUrl = sr.getParameter("bearychatBuildServerUrl");
            sendAs = sr.getParameter("bearychatSendAs");
            if (buildServerUrl != null && !buildServerUrl.endsWith("/")) {
                buildServerUrl = buildServerUrl + "/";
            }
            try {
                new BearychatNotifier(teamDomain, token, room, buildServerUrl, sendAs);
            } catch (Exception e) {
                throw new FormException("Failed to initialize notifier - check your global notifier configuration settings", e, "");
            }
            save();
            return super.configure(sr, formData);
        }

        @Override
        public String getDisplayName() {
            return "Bearychat Notifications";
        }
    }

    public static class BearychatJobProperty extends hudson.model.JobProperty<AbstractProject<?, ?>> {
        private String room;
        private boolean startNotification;
        private boolean notifySuccess;
        private boolean notifyAborted;
        private boolean notifyNotBuilt;
        private boolean notifyUnstable;
        private boolean notifyFailure;
        private boolean notifyBackToNormal;


        @DataBoundConstructor
        public BearychatJobProperty(String room,
                                  boolean startNotification,
                                  boolean notifyAborted,
                                  boolean notifyFailure,
                                  boolean notifyNotBuilt,
                                  boolean notifySuccess,
                                  boolean notifyUnstable,
                                  boolean notifyBackToNormal) {
            this.room = room;
            this.startNotification = startNotification;
            this.notifyAborted = notifyAborted;
            this.notifyFailure = notifyFailure;
            this.notifyNotBuilt = notifyNotBuilt;
            this.notifySuccess = notifySuccess;
            this.notifyUnstable = notifyUnstable;
            this.notifyBackToNormal = notifyBackToNormal;
        }

        @Exported
        public String getRoom() {
            return room;
        }

        @Exported
        public boolean getStartNotification() {
            return startNotification;
        }

        @Exported
        public boolean getNotifySuccess() {
            return notifySuccess;
        }

        @Override
        public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
            if (startNotification) {
                Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
                for (Publisher publisher : map.values()) {
                    if (publisher instanceof BearychatNotifier) {
                        logger.info("Invoking Started...");
                        new ActiveNotifier((BearychatNotifier) publisher).started(build);
                    }
                }
            }
            return super.prebuild(build, listener);
        }

        @Exported
        public boolean getNotifyAborted() {
            return notifyAborted;
        }

        @Exported
        public boolean getNotifyFailure() {
            return notifyFailure;
        }

        @Exported
        public boolean getNotifyNotBuilt() {
            return notifyNotBuilt;
        }

        @Exported
        public boolean getNotifyUnstable() {
            return notifyUnstable;
        }

        @Exported
        public boolean getNotifyBackToNormal() {
            return notifyBackToNormal;
        }

        @Extension
        public static final class DescriptorImpl extends JobPropertyDescriptor {
            public String getDisplayName() {
                return "Bearychat Notifications";
            }

            @Override
            public boolean isApplicable(Class<? extends Job> jobType) {
                return true;
            }

            @Override
            public BearychatJobProperty newInstance(StaplerRequest sr, JSONObject formData) throws hudson.model.Descriptor.FormException {
                return new BearychatJobProperty(sr.getParameter("bearychatProjectRoom"),
                        sr.getParameter("bearychatStartNotification") != null,
                        sr.getParameter("bearychatNotifyAborted") != null,
                        sr.getParameter("bearychatNotifyFailure") != null,
                        sr.getParameter("bearychatNotifyNotBuilt") != null,
                        sr.getParameter("bearychatNotifySuccess") != null,
                        sr.getParameter("bearychatNotifyUnstable") != null,
                        sr.getParameter("bearychatNotifyBackToNormal") != null);
            }
        }
    }
}

package jenkins.plugins.bearychat;

import java.util.logging.Logger;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;

@SuppressWarnings("rawtypes")
public class ActiveNotifier implements FineGrainedNotifier {

    private static final Logger logger = Logger.getLogger(ActiveNotifier.class.getName());

    BuildListener listener;
    BearyChatNotifier notifier;

    public ActiveNotifier(BearyChatNotifier notifier, BuildListener listener) {
        super();
        this.notifier = notifier;
        this.listener = listener;
    }

    private BearyChatService getBearyChat(AbstractBuild build) {
        return notifier.newBearyChatService(build, listener);
    }


    public void deleted(AbstractBuild build) {}

    /**
     * start after failure is still RED color
     */
    public void started(AbstractBuild build) {
        MessageBuilder messageBuilder = new MessageBuilder(this.notifier, build);

        String color = Helper.COLOR_GREEN;
        Run previousBuild = null;
        try {
            previousBuild = build.getProject().getLastBuild().getPreviousBuild();
        } catch (NullPointerException e) {
            logger.info("get previous build failure");
        } catch (Exception e) {
            logger.info("get previous build failure");
            color = Helper.COLOR_GREEN;
        }
        Result lastResult = previousBuild == null ? null : previousBuild.getResult();
        if(lastResult != null && lastResult == Result.FAILURE) {
            color = Helper.COLOR_RED;
        }

        String title = messageBuilder.getStartedMessage();
        String fallback = messageBuilder.getStartedFallback();
        String text = messageBuilder.getStartedText();

        getBearyChat(build).publish(title, text, fallback, color);
    }

    public void finalized(AbstractBuild r) {
    }

    private void notifyCompleted(AbstractBuild build) {
        MessageBuilder messageBuilder = new MessageBuilder(this.notifier, build);
        String color = Helper.getBuildColor(build);
        String title = messageBuilder.getCompletedMessage();
        String fallback = messageBuilder.getCompletedFallback();
        String text = messageBuilder.getCompletedText();

        getBearyChat(build).publish(title, text, fallback, color);
    }

    public void completed(AbstractBuild build) {
        AbstractProject<?, ?> project = build.getProject();
        Result result = build.getResult();
        AbstractBuild<?, ?> previousBuild = project.getLastBuild();
        do {
            previousBuild = previousBuild == null ? null : previousBuild.getPreviousCompletedBuild();
        }
        while (previousBuild != null && previousBuild.getResult() == Result.ABORTED);
        Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;

        // Build aborted
        if (result == Result.ABORTED && this.notifier.isNotifyOnAborted()) {
            this.notifyCompleted(build);
            return ;
        }

        // Build fail on this build
        if (result == Result.FAILURE && previousResult != Result.FAILURE && this.notifier.isNotifyOnFailure()) {
            this.notifyCompleted(build);
            return ;
        }

        // Build fail multi times
        if (result == Result.FAILURE && previousResult == Result.FAILURE) {
            this.notifyCompleted(build);
            return ;
        }

        // Build not built
        if (result == Result.NOT_BUILT && this.notifier.isNotifyOnNotBuilt()) {
            this.notifyCompleted(build);
            return ;
        }

        // Back to normal
        if (result == Result.SUCCESS || (previousResult != Result.FAILURE && previousResult != Result.UNSTABLE) || this.notifier.isNotifyOnBackToNormal()) {
            this.notifyCompleted(build);
            return ;
        }

        // Build successful
        if (result == Result.SUCCESS && this.notifier.isNotifyOnSuccess()) {
            this.notifyCompleted(build);
            return ;
        }

        // Build Unstable
        if (result == Result.UNSTABLE && this.notifier.isNotifyOnUnstable()) {
            this.notifyCompleted(build);
            return ;
        }
    }
}

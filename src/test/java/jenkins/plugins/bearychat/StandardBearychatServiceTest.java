package jenkins.plugins.bearychat;

import jenkins.plugins.bearychat.StandardBearyChatService;
import org.junit.Before;
import org.junit.Test;

public class StandardBearyChatServiceTest {

    /**
     * Publish should generally not rethrow exceptions, or it will cause a build job to fail at end.
     */
    @Test
    public void publishWithBadHostShouldNotRethrowExceptions() {
        StandardBearyChatService service = new StandardBearyChatService("foo", "token", "general");
        service.setHost("hostvaluethatwillcausepublishtofail");
        service.publish("message");
    }

    /**
     * Use a valid host, but an invalid team domain
     */
    @Test
    public void invalidTeamDomainShouldFail() {
        StandardBearyChatService service = new StandardBearyChatService("my", "token", "general");
        service.publish("message");
    }

    /**
     * Use a valid team domain, but a bad token
     */
    @Test
    public void invalidTokenShouldFail() {
        StandardBearyChatService service = new StandardBearyChatService("beary", "token", "general");
        service.publish("message");
    }
}

package jenkins.plugins.bearychat;

import jenkins.plugins.bearychat.StandardBearychatService;
import org.junit.Before;
import org.junit.Test;

public class StandardBearychatServiceTest {

    /**
     * Publish should generally not rethrow exceptions, or it will cause a build job to fail at end.
     */
    @Test
    public void publishWithBadHostShouldNotRethrowExceptions() {
        StandardBearychatService service = new StandardBearychatService("foo", "token", "#general");
        service.setHost("hostvaluethatwillcausepublishtofail");
        service.publish("message");
    }

    /**
     * Use a valid host, but an invalid team domain
     */
    @Test
    public void invalidTeamDomainShouldFail() {
        StandardBearychatService service = new StandardBearychatService("my", "token", "#general");
        service.publish("message");
    }

    /**
     * Use a valid team domain, but a bad token
     */
    @Test
    public void invalidTokenShouldFail() {
        StandardBearychatService service = new StandardBearychatService("beary", "token", "#general");
        service.publish("message");
    }
}

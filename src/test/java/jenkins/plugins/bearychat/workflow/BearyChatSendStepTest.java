package jenkins.plugins.bearychat.workflow;

import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.plugins.bearychat.Messages;
import jenkins.plugins.bearychat.BearyChatNotifier;
import jenkins.plugins.bearychat.BearyChatService;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.PrintStream;
import java.io.PrintWriter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Traditional Unit tests, allows testing null Jenkins,getInstance()
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class,BearyChatSendStep.class})
public class BearyChatSendStepTest {

    /*
    @Mock
    TaskListener taskListenerMock;
    @Mock
    PrintStream printStreamMock;
    @Mock
    PrintWriter printWriterMock;
    @Mock
    StepContext stepContextMock;
    @Mock
    BearyChatService bearychatServiceMock;
    @Mock
    Jenkins jenkins;
    @Mock
    BearyChatNotifier.DescriptorImpl bearychatDescMock;
    */

    @Before
    public void setUp() {
        /*
        PowerMockito.mockStatic(Jenkins.class);
        when(jenkins.getDescriptorByType(BearyChatNotifier.DescriptorImpl.class)).thenReturn(bearychatDescMock);
        */
    }

    @Test
    public void testStepOverrides() throws Exception {
        /*
        BearyChatSendStep.BearyChatSendStepExecution stepExecution = spy(new BearyChatSendStep.BearyChatSendStepExecution());
        BearyChatSendStep bearychatSendStep = new BearyChatSendStep("message");
        bearychatSendStep.setToken("token");
        bearychatSendStep.setTeamDomain("teamDomain");
        bearychatSendStep.setChannel("channel");
        bearychatSendStep.setColor("good");
        stepExecution.step = bearychatSendStep;

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(bearychatDescMock.getToken()).thenReturn("differentToken");


        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(stepExecution.getBearyChatService(anyString(), anyString(), anyString())).thenReturn(bearychatServiceMock);
        when(bearychatServiceMock.publish(anyString(), anyString())).thenReturn(true);

        stepExecution.run();
        verify(stepExecution, times(1)).getBearyChatService("teamDomain", "token", "channel");
        verify(bearychatServiceMock, times(1)).publish("message", "good");
        assertFalse(stepExecution.step.isFailOnError());*/
    }

    @Test
    public void testValuesForGlobalConfig() throws Exception {
/*
        BearyChatSendStep.BearyChatSendStepExecution stepExecution = spy(new BearyChatSendStep.BearyChatSendStepExecution());
        stepExecution.step = new BearyChatSendStep("message");

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(bearychatDescMock.getTeamDomain()).thenReturn("globalTeamDomain");
        when(bearychatDescMock.getToken()).thenReturn("globalToken");
        when(bearychatDescMock.getChannel()).thenReturn("globalChannel");

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(stepExecution.getBearyChatService(anyString(), anyString(), anyString())).thenReturn(bearychatServiceMock);

        stepExecution.run();
        verify(stepExecution, times(1)).getBearyChatService("globalTeamDomain", "globalToken", "globalChannel");
        verify(bearychatServiceMock, times(1)).publish("message", "");
        assertNull(stepExecution.step.getTeamDomain());
        assertNull(stepExecution.step.getToken());
        assertNull(stepExecution.step.getChannel());
        assertNull(stepExecution.step.getColor());
        */
    }

    @Test
    public void testNonNullEmptyColor() throws Exception {
/*
        BearyChatSendStep.BearyChatSendStepExecution stepExecution = spy(new BearyChatSendStep.BearyChatSendStepExecution());
        BearyChatSendStep bearychatSendStep = new BearyChatSendStep("message");
        bearychatSendStep.setColor("");
        stepExecution.step = bearychatSendStep;

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(stepExecution.getBearyChatService(anyString(), anyString(), anyString())).thenReturn(bearychatServiceMock);

        stepExecution.run();
        verify(bearychatServiceMock, times(1)).publish("message", "");
        assertNull(stepExecution.step.getColor());
        */
    }

    @Test
    public void testNullJenkinsInstance() throws Exception {
/*
        BearyChatSendStep.BearyChatSendStepExecution stepExecution = spy(new BearyChatSendStep.BearyChatSendStepExecution());
        stepExecution.step = new BearyChatSendStep("message");

        when(Jenkins.getInstance()).thenThrow(NullPointerException.class);

        stepExecution.listener = taskListenerMock;

        when(taskListenerMock.error(anyString())).thenReturn(printWriterMock);
        doNothing().when(printStreamMock).println();

        stepExecution.run();
        verify(taskListenerMock, times(1)).error(Messages.NotificationFailedWithException(anyString()));
        */
    }
}
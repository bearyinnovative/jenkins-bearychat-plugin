package jenkins.plugins.bearychat.workflow;

import hudson.model.Result;
import jenkins.plugins.bearychat.Messages;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class BearyChatSendStepIntegrationTest {

    // @Rule
    // public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void configRoundTrip() throws Exception {
        /*
        BearyChatSendStep step1 = new BearyChatSendStep("message");
        step1.setColor("good");
        step1.setChannel("#channel");
        step1.setToken("token");
        step1.setTeamDomain("teamDomain");
        step1.setFailOnError(true);

        BearyChatSendStep step2 = new StepConfigTester(jenkinsRule).configRoundTrip(step1);
        jenkinsRule.assertEqualDataBoundBeans(step1, step2);
        */
    }

    @Test
    public void test_global_config_override() throws Exception {
        /*
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "workflow");
        //just define message
        job.setDefinition(new CpsFlowDefinition("bearychatSend(message: 'message', teamDomain: 'teamDomain', token: 'token', channel: '#channel', color: 'good');", true));
        WorkflowRun run = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0).get());
        //everything should come from step configuration
        jenkinsRule.assertLogContains(Messages.BearyChatSendStepConfig(false, false, false, false), run);
        */
    }

    @Test
    public void test_fail_on_error() throws Exception {
        /*
        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "workflow");
        //just define message
        job.setDefinition(new CpsFlowDefinition("bearychatSend(message: 'message', teamDomain: 'teamDomain', token: 'token', channel: '#channel', color: 'good', failOnError: true);", true));
        WorkflowRun run = jenkinsRule.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0).get());
        //everything should come from step configuration
        jenkinsRule.assertLogContains(Messages.NotificationFailed(), run);
        */
    }
}
package jenkins.plugins.bearychat;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.json.JSONObject;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

public class StandardBearyChatService implements BearyChatService {

    private static final Logger logger = Logger.getLogger(StandardBearyChatService.class.getName());

    private String webhook;
    private String channel;

    public StandardBearyChatService(String webhook, String channel) {
        super();

        this.webhook = webhook;
        this.channel = channel;
    }

    public boolean publish(String message) {
        return publish(message, "green");
    }

    public boolean publish(String message, String color) {
        return publish("unknown", message, color);
    }

    public boolean publish(String action, String message, String color) {
        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("message", message);
        dataMap.put("color", color);
        return publish(action, dataMap);
    }

    public boolean publish(String action, Map<String, Object> dataMap) {
        boolean result = true;
        String url = genPostUrl();
        logger.info("Posting to " + channel + " on " + webhook + ": " + dataMap);
        HttpClient client = getHttpClient();
        PostMethod post = new PostMethod(url);
        JSONObject json = new JSONObject();

        try {
            JSONObject dataJson = new JSONObject();

            String message = "", color = "";

            if(dataMap != null && !dataMap.isEmpty()){
                message = (String)dataMap.get("message");
                color = (String)dataMap.get("color");

                dataJson.put("authors", dataMap.get("authors"));
                dataJson.put("files", dataMap.get("files"));

                Map<String,String> configMap = (Map<String,String>)dataMap.get("config");
                if(configMap != null){
                    JSONObject configJson = new JSONObject();
                    for(Map.Entry<String, String> entry : configMap.entrySet()){
                        configJson.put(entry.getKey(), entry.getValue());
                    }
                    dataJson.put("config", configJson);
                }

                Map<String,String> projectMap = (Map<String,String>)dataMap.get("project");
                if(projectMap != null){
                    JSONObject projectJson = new JSONObject();
                    for(Map.Entry<String, String> entry : projectMap.entrySet()){
                        projectJson.put(entry.getKey(), entry.getValue());
                    }
                    dataJson.put("project", projectJson);
                }

                Map<String,String> jobMap = (Map<String,String>)dataMap.get("job");
                if(jobMap != null){
                    JSONObject jobJson = new JSONObject();
                    for(Map.Entry<String, String> entry : jobMap.entrySet()){
                        jobJson.put(entry.getKey(), entry.getValue());
                    }
                    dataJson.put("job", jobJson);
                }
            }
            String data = dataJson.toString();

            json.put("action", action);
            json.put("channel", channel);
            json.put("text", message);
            json.put("color", color);
            json.put("data", data);

            post.addParameter("payload", json.toString());
            post.getParams().setContentCharset("UTF-8");

            int responseCode = client.executeMethod(post);
            String response = post.getResponseBodyAsString();
            if(responseCode != HttpStatus.SC_OK) {
                logger.log(Level.WARNING, "BearyChat post may have failed. Response: " + response);
                result = false;
            } else {
                logger.info("Posting succeeded");
            }

        } catch (NullPointerException e) {
            result = false;
        } catch (Exception e) {
            result = false;
            logger.log(Level.WARNING, "Error posting to BearyChat", e);
        } finally {
            post.releaseConnection();
        }
        return result;
    }

    public String genPostUrl(){
        return webhook;
    }

    protected HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        Jenkins instance = Jenkins.getInstance();
        ProxyConfiguration proxy = instance == null ? null : instance.proxy;
        if (proxy != null) {
            client.getHostConfiguration().setProxy(proxy.name, proxy.port);
            String username = proxy.getUserName();
            String password = proxy.getPassword();
            // Consider it to be passed if username specified. Sufficient?
            if (username != null && !"".equals(username.trim())) {
                logger.info("Using proxy authentication (user=" + username + ")");
                // http://hc.apache.org/httpclient-3.x/authentication.html#Proxy_Authentication
                // and
                // http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/trunk/src/examples/BasicAuthenticationExample.java?view=markup
                client.getState().setProxyCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
            }
        }
        return client;
    }

}

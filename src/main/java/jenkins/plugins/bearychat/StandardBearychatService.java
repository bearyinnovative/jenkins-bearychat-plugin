package jenkins.plugins.bearychat;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONObject;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import hudson.ProxyConfiguration;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

public class StandardBearychatService implements BearychatService {

    private static final Logger logger = Logger.getLogger(StandardBearychatService.class.getName());

    // TODO: rm stage
    private String host = "bearychat.com";
    private String teamDomain;
    private String token;
    private String[] roomIds;

    public StandardBearychatService(String teamDomain, String token, String roomId) {
        super();
        this.teamDomain = teamDomain;
        this.token = token;
        this.roomIds = roomId.split(",");
    }

    public void publish(String message) {
        publish(message, "warning");
    }

    public void publish(String message, String color) {
        publish(message, null, color);
    }

    @Override
        public void publish(String message, Map<String, String> contentMap, String color) {
        for (String roomId : roomIds) {
            String url = "https://" + teamDomain + "." + host + "/api/hooks/jenkins/" + token;
            logger.info("Posting: to " + roomId + " on " + teamDomain + " using " + url +": " + message + " " + color);
            HttpClient client = getHttpClient();
            PostMethod post = new PostMethod(url);
            JSONObject json = new JSONObject();

            try {

            JSONObject contentJson = new JSONObject();
            if(contentMap != null && !contentMap.isEmpty()){
                for(String key : contentMap.keySet()){
                    String val = contentMap.get(key);
                    contentJson.put(key, val);
                }
            }
            String content = contentJson.toString();

                json.put("channel", roomId);
                json.put("text", message);
                json.put("content", content);

                post.addParameter("payload", json.toString());
                post.getParams().setContentCharset("UTF-8");

                int responseCode = client.executeMethod(post);
                String response = post.getResponseBodyAsString();
                if(responseCode != HttpStatus.SC_OK) {
                    logger.log(Level.WARNING, "Bearychat post may have failed. Response: " + response);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error posting to Bearychat", e);
            } finally {
                logger.info("Posting succeeded");
                post.releaseConnection();
            }
        }
    }

    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
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
        }
        return client;
    }

    void setHost(String host) {
        this.host = host;
    }

}

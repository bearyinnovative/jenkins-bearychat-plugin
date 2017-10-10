package jenkins.plugins.bearychat;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

public class StandardBearyChatService implements BearyChatService {

    private static final Logger logger = Logger.getLogger(StandardBearyChatService.class.getName());

    public static String VERSION = "3.0";

    private String webhook;
    private String channel;

    public StandardBearyChatService(String webhook, String channel) {
        super();

        this.webhook = webhook;
        this.channel = channel;
    }

    public JSONObject genAttachment(String title, String text, String color, String url) {
        JSONObject attachment = new JSONObject();
        if (title != null) {
            attachment.put("title", title);
        }
        if (text != null) {
            attachment.put("text", text);
        }
        if (color != null) {
            attachment.put("color", color);
        }
        if (url != null) {
            attachment.put("url", url);
        }
        return attachment;
    }

    public boolean publish(String message) {
        JSONObject data = new JSONObject();
        data.put("text", message);
        return publish(data);
    }

    public boolean publish(String message, String text, String fallback, String color) {
        JSONObject attachment = this.genAttachment(null, text, color, null);
        JSONArray attachments = new JSONArray();
        attachments.add(attachment);
        return publish(message, attachments, fallback);
    }

    public boolean publish(String message, JSONArray attachments, String fallback) {
        JSONObject data = new JSONObject();
        if (fallback != null) {
            data.put("fallback", fallback);
        }
        data.put("text", message);
        data.put("attachments", attachments);
        return publish(data);
    }

    public boolean publish(JSONObject data) {
        boolean result = true;
        String url = getPostUrl();
        logger.info("Post to " + channel + " on " + url + ": " + data);
        HttpClient client = getHttpClient();
        PostMethod post = new PostMethod(url);

        if (this.channel != null) {
            data.put("channel", this.channel);
        }

        try {
            post.addRequestHeader("X-JENKINS-VERSION", Jenkins.VERSION);
            post.addRequestHeader("X-PLUGIN-VERSION", VERSION);
            post.addParameter("payload", data.toString());
            post.getParams().setContentCharset("UTF-8");

            int responseCode = client.executeMethod(post);
            String response = post.getResponseBodyAsString();
            if (responseCode != HttpStatus.SC_OK) {
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

    public String getPostUrl() {
        // Adding version in QueryString for statistic.
        return webhook + "?v=" + VERSION;
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

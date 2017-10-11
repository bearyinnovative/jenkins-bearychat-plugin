package jenkins.plugins.bearychat;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface BearyChatService {
    boolean publish(String message);

    boolean publish(String message, String text, String fallback, String color);

    boolean publish(String message, JSONArray attachments, String fallback);

    boolean publish(JSONObject data);
}

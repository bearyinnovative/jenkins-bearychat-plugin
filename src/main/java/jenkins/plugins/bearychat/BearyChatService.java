package jenkins.plugins.bearychat;

import java.util.Map;

public interface BearyChatService {
    boolean publish(String message);

    boolean publish(String message, String color);

    boolean publish(String action, String message, String color);

    boolean publish(String action, Map<String, Object> dataMap);
}

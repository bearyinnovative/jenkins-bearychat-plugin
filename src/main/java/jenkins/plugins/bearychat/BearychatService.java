package jenkins.plugins.bearychat;

import java.util.Map;

public interface BearychatService {
    void publish(String message);

    void publish(String message, String color);

    void publish(String action, Map<String, Object> dataMap);
}

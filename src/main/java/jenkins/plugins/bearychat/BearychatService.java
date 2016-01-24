package jenkins.plugins.bearychat;

public interface BearychatService {
    boolean publish(String message);

    boolean publish(String message, String color);

    boolean publish(String action, Map<String, Object> dataMap);
}

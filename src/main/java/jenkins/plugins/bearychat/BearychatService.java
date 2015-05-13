package jenkins.plugins.bearychat;

public interface BearychatService {
    void publish(String message);

    void publish(String message, String color);
}

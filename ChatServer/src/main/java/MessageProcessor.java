public interface MessageProcessor {
    void processMessage(int clientID, Message message);
}

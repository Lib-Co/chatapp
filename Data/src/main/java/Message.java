import java.nio.file.Files;
import java.time.Instant;

public class Message {

    enum Type {
        PRIVATE,
        BROADCAST,
        QUIT
    }
    public Message.Type messageType;
    public String username;
    public String data;
    public int id;
    public Instant arrivalTime;

    public Message(Message.Type messageType, String username, String data) {
        this.messageType = messageType;
        this.username = username;
        this.data = data;
    }

    public Message() {
        //Empty constructor for Jackson objects
    }

}

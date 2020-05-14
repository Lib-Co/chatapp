import java.time.Instant;

public class Message {

    String data;
    Instant arrivalTime;

    public Message(String data, Instant arrivalTime) {
        this.data = data;
        this.arrivalTime = arrivalTime;
    }
}


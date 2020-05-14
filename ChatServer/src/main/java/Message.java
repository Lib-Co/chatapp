import java.time.Instant;

public class Message {

    String data;
    Instant arrivalTime;
    int id;

    public Message(String data, Instant arrivalTime, int id) {
        this.data = data;
        this.arrivalTime = arrivalTime;
        this.id = id;
    }
}


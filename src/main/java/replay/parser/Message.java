package replay.parser;

public abstract class Message {
    public String type;
    public String topic;
    public int tick;

    public Message(String type, String topic, int tick) {
        this.type = type;
        this.topic = topic;
        this.tick = tick;
    }

    public abstract String toMessageFormat();
}

package replay.parser;

public abstract class Message {
    public String type;
    public String entity;
    public String topic;
    public int tick;

    public Message(String type, String entity, String topic, int tick) {
        this.type = type;
        this.entity = entity;
        this.topic = topic;
        this.tick = tick;
    }

    public abstract String toMessageFormat();
}

package replay.parser;

public abstract class Message {
    public String type;
    public String entity;
    public int tick;

    public Message(String type, String entity, int tick) {
        this.type = type;
        this.entity = entity;
        this.tick = tick;
    }

    public abstract String toMessageFormat();
}

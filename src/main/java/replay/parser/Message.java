package replay.parser;

public abstract class Message {
    public int game;
    public String type;
    public String topic;
    public int tick;
    public String message;

    public Message(int game, String type, String topic, int tick) {
        this.game = game;
        this.type = type;
        this.topic = topic;
        this.tick = tick;
    }

    public abstract String toMessageFormat();
}

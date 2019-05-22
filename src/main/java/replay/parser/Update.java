package replay.parser;

public class Update {
    public String entity;
    public String property;
    public String value;
    public int tick;

    public Update(String entity, String property, String value, int tick) {
        this.entity = entity;
        this.property = property;
        this.value = value;
        this.tick = tick;
    }
}

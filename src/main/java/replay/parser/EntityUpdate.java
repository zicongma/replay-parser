package replay.parser;

public class EntityUpdate extends Message {
    public String entity;
    public String property;
    public String value;

    public EntityUpdate(String entity, String topic, String property, String value, int tick) {
        super("update", topic, tick);
        this.entity = entity;
        this.property = property;
        this.value = value;
    }

    @Override
    public String toMessageFormat() {
        return "update" + "/" + this.entity + "/" + this.property + "/" + this.value;
    }
}

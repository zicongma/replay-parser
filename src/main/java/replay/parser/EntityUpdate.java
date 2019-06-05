package replay.parser;

public class EntityUpdate extends Message {
    public String entity;
    public String property;
    public String value;

    public EntityUpdate(int game, String entity, String topic, String property, String value, int tick) {
        super(game,"update", topic, tick);
        this.entity = entity;
        this.property = property;
        this.value = value;
    }

    @Override
    public String toMessageFormat() {
        return "update" + "/" + this.game + "/" + this.entity + "/" + this.property + "/" + this.value;
    }
}

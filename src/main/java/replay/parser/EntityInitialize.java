package replay.parser;

public class EntityInitialize extends Message {
    public String entity;
    public String[] properties;
    public String[] values;

    public EntityInitialize(int game, String entity, String topic, String[] properties, String[] values, int tick) {
        super(game, "initialize", topic, tick);
        this.entity = entity;
        this.properties = properties;
        this.values = values;
        this.message = toMessageFormat();
    }

    @Override
    public String toMessageFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("initialize");
        sb.append("/");
        sb.append(game);
        sb.append("/");
        sb.append(entity);
        for (int i = 0; i < properties.length; i++) {
            sb.append("/");
            sb.append(properties[i]);
            sb.append("/");
            sb.append(values[i]);
        }
        return sb.toString();
    }
}

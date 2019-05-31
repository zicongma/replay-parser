package replay.parser;

public class EntityInitialize extends Message {
    public String entity;
    public String[] properties;
    public String[] values;

    public EntityInitialize(String entity, String topic, String[] properties, String[] values, int tick) {
        super("initialize", topic, tick);
        this.entity = entity;
        this.properties = properties;
        this.values = values;
    }

    @Override
    public String toMessageFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("initialize");
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

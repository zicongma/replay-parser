package replay.parser;

public class Initialize extends Message {
    public String[] properties;
    public String[] values;

    public Initialize(String entity, String[] properties, String[] values, int tick) {
        super("initialize", entity, tick);
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

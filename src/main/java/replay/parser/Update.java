package replay.parser;

public class Update extends Message {
    public String property;
    public String value;

    public Update(String entity, String property, String value, int tick) {
        super("update", entity, tick);
        this.property = property;
        this.value = value;
    }

    @Override
    public String toMessageFormat() {
        return "update" + "/" + this.entity + "/" + this.property + "/" + this.value;
    }
}

package replay.parser;

public class CombatLog extends Message {
    public String combatType;
    public String attacker;
    public String target;
    public int value;

    public CombatLog(int game, String combatType, String attacker, String target, int value, int tick) {
        super(game, "combatlog", "combatlog", tick);
        this.combatType = combatType;
        this.attacker = attacker;
        this.target = target;
        this.value = value;
    }

    @Override
    public String toMessageFormat() {
        return this.game + "/" + this.combatType + "/" + this.attacker + "/" + this.target + "/" + this.value;
    }
}

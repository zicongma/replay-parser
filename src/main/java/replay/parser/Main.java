package replay.parser;

import org.joda.time.Instant;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.CombatLogEntry;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.gameevents.OnCombatLogEntry;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

import java.util.*;

@UsesEntities
public class Main {
    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private int totalGame = 16;

    private final PeriodFormatter GAMETIME_FORMATTER = new PeriodFormatterBuilder()
            .minimumPrintedDigits(2)
            .printZeroAlways()
            .appendHours()
            .appendLiteral(":")
            .appendMinutes()
            .appendLiteral(":")
            .appendSeconds()
            .appendLiteral(".")
            .appendMillis3Digit()
            .toFormatter();

    private boolean isPlayer(Entity e) { return e.getDtClass().getDtName().startsWith("CDOTAPlayer"); }
    private List<Message> messages = new ArrayList<>();
    private List<Long> sentTicks = new ArrayList<>();

    private boolean isHero(Entity e) {
        return e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero");
    }
    private boolean isResource(Entity e) { return e.getDtClass().getDtName().startsWith("CDOTA_PlayerResource"); }

    @OnEntityCreated
    public void onCreated(Context ctx, Entity e) {
        String[] properties;
        String[] values;
        String topic;

        if (!(isHero(e)||isResource(e))) {
            return;
        }

        for (int game = 0; game < totalGame; game++) {
            if ((isHero(e))) {
                properties = new String[] {"m_iCurrentLevel", "m_iCurrentXP", "m_iHealth", "m_lifeState"
                        ,"CBodyComponent.m_cellX", "CBodyComponent.m_cellY", "CBodyComponent.m_vecX", "CBodyComponent.m_vecY",
                        "m_iTeamNum", "m_iDamageMin", "m_iDamageMax", "m_flStrength", "m_flAgility", "m_flIntellect", "m_iPlayerID"};
                topic = "hero";
                int numProperties = properties.length;
                values = new String[numProperties];
                for (int i = 0; i < numProperties; i++) {
                    values[i] = e.getProperty(properties[i]).toString();
                }
                EntityInitialize initialize = new EntityInitialize(game, e.getDtClass().getDtName(), topic, properties, values, ctx.getTick());
                messages.add(initialize);
            } else if (isResource(e)) {
//            topic = "resource";
//            for (int i = 0; i < 10; i++) {
//                properties = new String[] {"m_vecPlayerTeamData.000" + i + ".m_iKills",
//                        "m_vecPlayerTeamData.000" + i + ".m_iAssists" , "m_vecPlayerTeamData.000" + i + ".m_iDeaths"};
//                int numProperties = properties.length;
//                values = new String[numProperties];
//                for (int j = 0; j < numProperties; j++) {
//                    values[j] = e.getProperty(properties[j]).toString();
//                }
//                EntityInitialize initialize = new EntityInitialize(game, e.getDtClass().getDtName() + i, topic, properties, values, ctx.getTick());
//                messages.add(initialize);
//            }
            }
        }
    }


    @OnEntityUpdated
    public void onUpdated(Context ctx, Entity e, FieldPath[] updatedPaths, int updateCount) {
        if (!(isHero(e) || isResource(e))) {
            return;
        }
        Entities entities = ctx.getProcessor(Entities.class);

        for (int game = 0; game < totalGame; game++) {
            for (int i = 0; i < updateCount; i++) {
                if (isHero(e)) {
                    EntityUpdate update = new EntityUpdate(game, e.getDtClass().getDtName(),"hero", e.getDtClass().getNameForFieldPath(updatedPaths[i]), e.getPropertyForFieldPath(updatedPaths[i]).toString(), ctx.getTick());
                    messages.add(update);
                } else if (isResource(e)) {
//                String[] split = e.getDtClass().getNameForFieldPath(updatedPaths[i]).split("\\.");
//                if (split.length >= 3) {
//                    EntityUpdate update = new EntityUpdate(game, e.getDtClass().getDtName() + split[1].charAt(3), "resource", e.getDtClass().getNameForFieldPath(updatedPaths[i]), e.getPropertyForFieldPath(updatedPaths[i]).toString(), ctx.getTick());
//                    messages.add(update);
//                }
                }
            }
        }

    }

//    @OnCombatLogEntry
    public void onCombatLogEntry(Context ctx, CombatLogEntry cle) {
        for (int game = 0; game < totalGame; game++) {
            CombatLog combatLog = null;
            switch (cle.getType()) {
                case DOTA_COMBATLOG_DAMAGE:
                    combatLog = new CombatLog(game, "damage",  cle.getAttackerName(), cle.getTargetName(), cle.getValue(), ctx.getTick());
                    break;
                case DOTA_COMBATLOG_HEAL:
                    combatLog = new CombatLog(game, "heal", cle.getAttackerName(), cle.getTargetName(), cle.getValue(), ctx.getTick());
                    break;
                case DOTA_COMBATLOG_DEATH:
                    combatLog = new CombatLog(game, "kill", cle.getAttackerName(), cle.getTargetName(), -1, ctx.getTick());
                case DOTA_COMBATLOG_GOLD:
                    combatLog = new CombatLog(game, "gold",  "", cle.getTargetName(), cle.getValue(), ctx.getTick());
                    break;
                case DOTA_COMBATLOG_XP:
                    combatLog = new CombatLog(game, "xp",  "", cle.getTargetName(), cle.getValue(), ctx.getTick());
                    break;
            }
            if (combatLog != null) {
                messages.add(combatLog);
            }
        }
    }

    public void simulate() {

        MessageProducer producer = new MessageProducer();
        long start = System.nanoTime();
        int updateidx = 0;
        int finalidx = messages.size();
        while (true) {
            long timePassed = System.nanoTime() - start;
            long ticksPassed = timePassed * 30 / 1000000000;
            while (ticksPassed >= messages.get(updateidx).tick) {
                Message message  = messages.get(updateidx);
                System.out.println(message.toMessageFormat());
                //producer.send(message.topic, message.toMessageFormat());
                long sentTick = (System.nanoTime() - start) * 30 / 1000000000;
                //sentTicks.add(sentTick);
                updateidx ++;
                System.out.println(sentTick - message.tick);
            }
        }
    }

    public void statsCollection() {
        int currIdx = 0;
        int finalIdx = messages.size();
        int[] throughput = new int[30 * 60];
        while (currIdx < finalIdx) {
            int tick = messages.get(currIdx).tick;
            int slot = (int) tick / 30;
            throughput[slot] ++;
            currIdx ++;
        }
        System.out.println(Arrays.toString(throughput));
    }

    public void sentStatsCollection() {
        int currIdx = 0;
        int finalIdx = sentTicks.size();
        int[] throughput = new int[30 * 60];
        while (currIdx < finalIdx) {
            long tick = sentTicks.get(currIdx);
            int slot = (int) tick / 30;
            throughput[slot] ++;
            currIdx ++;
        }
        System.out.println(Arrays.toString(throughput));
    }


    public void run(String[] args) throws Exception {
        this.totalGame = Integer.parseInt(args[1]);
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
//        statsCollection();
        System.out.println("starting");
        simulate();
//        sentStatsCollection();
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
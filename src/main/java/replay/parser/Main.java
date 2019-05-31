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

    private boolean isHero(Entity e) {
        return e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero");
    }
    private boolean isResource(Entity e) { return e.getDtClass().getDtName().startsWith("CDOTA_PlayerResource"); }

    private Float getRealGameTimeSeconds(Entities entities) {
        Entity grules = entities.getByDtName("CDOTAGamerulesProxy");
        Float gameTime = null;
        Float startTime = null;
        Float realTime = null;
        Float TIME_EPS = (float) 0.0001;

        if (grules != null) {
            gameTime = grules.getProperty("m_pGameRules.m_fGameTime");
        }

        if (gameTime != null) {
            startTime = grules.getProperty("m_pGameRules.m_flGameStartTime");
            if (startTime > TIME_EPS) {
                realTime = gameTime - startTime;
            } else {
                realTime = (float) 0;
            }
        }
        return realTime;
    }

    @OnEntityCreated
    public void onCreated(Context ctx, Entity e) {
        String[] properties;
        String[] values;
        String topic;

        if (!(isHero(e)||isResource(e))) {
            return;
        }
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
            EntityInitialize initialize = new EntityInitialize(e.getDtClass().getDtName(), topic, properties, values, ctx.getTick());
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
//                EntityInitialize initialize = new EntityInitialize(e.getDtClass().getDtName() + i, topic, properties, values, ctx.getTick());
//                messages.add(initialize);
//            }
        }
    }


    // Add _id in the player name
    @OnEntityUpdated
    public void onUpdated(Context ctx, Entity e, FieldPath[] updatedPaths, int updateCount) {
        if (!(isHero(e) || isResource(e))) {
            return;
        }
        Entities entities = ctx.getProcessor(Entities.class);
        Float realTime = (getRealGameTimeSeconds(entities));

        for (int i = 0; i < updateCount; i++) {
            if (isHero(e)) {
                //System.out.format("Entity: %s, Property: %s, Value: %s, Tick: %s\n", e.getDtClass().getDtName(), e.getDtClass().getNameForFieldPath(updatedPaths[i]), e.getPropertyForFieldPath(updatedPaths[i]), ctx.getTick());
                EntityUpdate update = new EntityUpdate(e.getDtClass().getDtName(),"hero", e.getDtClass().getNameForFieldPath(updatedPaths[i]), e.getPropertyForFieldPath(updatedPaths[i]).toString(), ctx.getTick());
                messages.add(update);
            } else if (isResource(e)) {
//                String[] split = e.getDtClass().getNameForFieldPath(updatedPaths[i]).split("\\.");
//                if (split.length >= 3) {
//                    EntityUpdate update = new EntityUpdate(e.getDtClass().getDtName() + split[1].charAt(3), "resource", e.getDtClass().getNameForFieldPath(updatedPaths[i]), e.getPropertyForFieldPath(updatedPaths[i]).toString(), ctx.getTick());
//                    messages.add(update);
//                }
            }
            //System.out.println(e.getDtClass().getDtName() + " " +  e.getProperty("m_iPlayerID") + " " + ctx.getTick());
        }
    }

    @OnCombatLogEntry
    public void onCombatLogEntry(Context ctx, CombatLogEntry cle) {
        CombatLog combatLog = null;
        switch (cle.getType()) {
            case DOTA_COMBATLOG_DAMAGE:
                combatLog = new CombatLog("damage",  cle.getAttackerName(), cle.getTargetName(), cle.getValue(), ctx.getTick());
                break;
            case DOTA_COMBATLOG_HEAL:
                combatLog = new CombatLog("heal", cle.getAttackerName(), cle.getTargetName(), cle.getValue(), ctx.getTick());
                break;
            case DOTA_COMBATLOG_DEATH:
                combatLog = new CombatLog("kill", cle.getAttackerName(), cle.getTargetName(), -1, ctx.getTick());
            case DOTA_COMBATLOG_GOLD:
                combatLog = new CombatLog("gold",  "", cle.getTargetName(), cle.getValue(), ctx.getTick());
                break;
            case DOTA_COMBATLOG_XP:
                combatLog = new CombatLog("xp",  "", cle.getTargetName(), cle.getValue(), ctx.getTick());
                break;
        }
        if (combatLog != null) {
            messages.add(combatLog);
        }
    }

    public void simulate() {

        MessageProducer producer = new MessageProducer();
        long start = System.nanoTime();
        int updateidx = 0;
        int finalidx = messages.size();
        // To Do: add looping feature
        while (true) {
            long timePassed = System.nanoTime() - start;
            long ticksPassed = timePassed * 30 / 1000000000 + messages.get(0).tick;
            while (ticksPassed >= messages.get(updateidx).tick && updateidx < finalidx) {
                Message message  = messages.get(updateidx);
                producer.send(message.topic, message.toMessageFormat() + "/" + Instant.now());
                updateidx ++;
            }
        }
    }


    public void run(String[] args) throws Exception {
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        simulate();
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
package replay.parser;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

        if (!(isHero(e))) {
            return;
        }

        properties = new String[] {"m_iCurrentLevel", "m_iCurrentXP", "m_iHealth", "m_lifeState"
                ,"CBodyComponent.m_cellX", "CBodyComponent.m_cellY", "CBodyComponent.m_vecX", "CBodyComponent.m_vecY",
                "m_iTeamNum", "m_iDamageMin", "m_iDamageMax", "m_flStrength", "m_flAgility", "m_flIntellect", "m_iPlayerID"};
        topic = "hero";
        int numProperties = properties.length;

        values = new String[numProperties];
        for (int i = 0; i < numProperties; i++) {
            values[i] = e.getProperty(properties[i]).toString();
        }

        int tick = ctx.getTick();

        String name = e.getDtClass().getDtName();



        for (int game = 0; game < totalGame; game++) {
            EntityInitialize initialize = new EntityInitialize(game, name, topic, properties, values,0);
            messages.add(initialize);
        }
    }


    @OnEntityUpdated
    public void onUpdated(Context ctx, Entity e, FieldPath[] updatedPaths, int updateCount) {
        if (!(isHero(e))) {
            return;
        }

        for (int i = 0; i < updateCount; i++) {
            String name = e.getDtClass().getDtName();
            String property = e.getDtClass().getNameForFieldPath(updatedPaths[i]);
            String value = e.getPropertyForFieldPath(updatedPaths[i]).toString();
            int tick = ctx.getTick();
            for (int game = 0; game < totalGame; game++) {
                EntityUpdate update = new EntityUpdate(game, name,"hero", property, value, 0);
                messages.add(update);
            }
        }
    }

    @OnCombatLogEntry
    public void onCombatLogEntry(Context ctx, CombatLogEntry cle) {
        boolean isType = true;
        String combatType = null;
        String attacker = null;
        String target = null;
        int value = -1;
        int tick = -1;
        switch (cle.getType()) {
            case DOTA_COMBATLOG_DAMAGE:
                combatType = "damage";
                attacker = cle.getAttackerName();
                target = cle.getTargetName();
                value = cle.getValue();
                tick = ctx.getTick();
                break;
            case DOTA_COMBATLOG_HEAL:
                combatType = "heal";
                attacker = cle.getAttackerName();
                target = cle.getTargetName();
                value = cle.getValue();
                tick = ctx.getTick();
                break;
            case DOTA_COMBATLOG_DEATH:
                combatType = "kill";
                attacker = cle.getAttackerName();
                target = cle.getTargetName();
                value = -1;
                tick = ctx.getTick();
            case DOTA_COMBATLOG_GOLD:
                combatType = "gold";
                attacker = "";
                target = cle.getTargetName();
                value = cle.getValue();
                tick = ctx.getTick();
                break;
            case DOTA_COMBATLOG_XP:
                combatType = "xp";
                attacker = "";
                target = cle.getTargetName();
                value = cle.getValue();
                tick = ctx.getTick();
                break;
            default:
                isType = false;

        }

        if (isType) {
            for (int game = 0; game < totalGame; game++) {
                CombatLog combatLog = new CombatLog(game, combatType, attacker, target, value, 0);
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
//                String str = message.message;
//                producer.send(message.topic, message.message);
                noop(message.message);
                updateidx ++;
                if (updateidx == finalidx) {
                    return;
                }
            }
        }
    }

    public void noop(String message) {

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


//        System.out.println("took : " + (finish - start) );
//        Runtime rut = Runtime.getRuntime();
//        try {
//            Process process = rut.exec(new String[]{"/bin/sh", "-c", "./run_engine.sh &"});
//            // prints out any message that are usually displayed in the console
//            Scanner scanner = new Scanner(process.getInputStream());
//            while (scanner.hasNext()) {
//                System.out.println(scanner.nextLine());
//            }
//            process.waitFor();
//        }catch(IOException e1) {
//            e1.printStackTrace();
//        }
//
//        TimeUnit.SECONDS.sleep(30);


        long testStart = System.nanoTime();

        simulate();

        long testFinish = System.nanoTime();

        System.out.println(messages.size() + " records processed in " + (testFinish - testStart) + " nanoseconds");

        System.out.println("\nProgram Finished\n");
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}

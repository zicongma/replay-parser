package replay.parser;

import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

import java.sql.Timestamp;
import java.util.*;

@UsesEntities
public class Main {
    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private boolean isPlayer(Entity e) { return e.getDtClass().getDtName().startsWith("CDOTAPlayer"); }
    private List<Update> updates = new ArrayList<>();

    private boolean isHero(Entity e) {
        return e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero");
    }

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
        if (!(isHero(e))) {
            return;
        }
//        System.out.println(e.getDtClass().getDtName() + " " + e.getProperty("m_iPlayerID") + " " + ctx.getTick());
//        System.out.println(e);
    }


    // Add _id in the player name
    @OnEntityUpdated
    public void onUpdated(Context ctx, Entity e, FieldPath[] updatedPaths, int updateCount) {
        if (!(isHero(e))) {
            return;
        }

        Entities entities = ctx.getProcessor(Entities.class);
        Float realTime = (getRealGameTimeSeconds(entities));

        for (int i = 0; i < updateCount; i++) {
            if (e.getDtClass().getNameForFieldPath(updatedPaths[i]).equals("m_iHealth")) {
                //System.out.println(ctx.getTick());
            }
            //System.out.format("Entity: %s, Property: %s, Value: %s, Tick: %s\n", e.getDtClass().getDtName(), e.getDtClass().getNameForFieldPath(updatedPaths[i]), e.getPropertyForFieldPath(updatedPaths[i]), ctx.getTick());
            Update update = new Update(e.getDtClass().getDtName(), e.getDtClass().getNameForFieldPath(updatedPaths[i]), e.getPropertyForFieldPath(updatedPaths[i]).toString(), ctx.getTick());
            updates.add(update);
        }
    }

    public void simulate() {

        UpdateProducer producer = new UpdateProducer();
        long start = System.nanoTime();
        int updateidx = 0;
        int finalidx = updates.size();
        // To Do: add looping feature
        while (true) {
            long timePassed = System.nanoTime() - start;
            long ticksPassed = timePassed * 30 / 1000000000 + updates.get(0).tick;
            while (ticksPassed >= updates.get(updateidx).tick && updateidx < finalidx) {
                Update update  = updates.get(updateidx);
                producer.send(update.entity + "/" + update.property + "/" + update.value + "/" + Instant.now());
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
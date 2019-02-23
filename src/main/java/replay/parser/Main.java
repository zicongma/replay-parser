package replay.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import sun.font.TrueTypeFont;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;

@UsesEntities
public class Main {
    HashMap<String, Integer> count = new HashMap<String, Integer>();

    HashSet<String> names = new HashSet<>();

    boolean initialized = false;

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private boolean isHero(Entity e) {
        return e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero");
    }

    private boolean isPlayer(Entity e) { return e.getDtClass().getDtName().startsWith("CDOTAPlayer"); }

    private boolean isResource(Entity e) { return e.getDtClass().getDtName().startsWith("CDOTA_PlayerResource"); }

    private boolean isDataDire(Entity e) { return e.getDtClass().getDtName().startsWith("CDOTA_DataDire"); }

    private boolean isDataRadiant(Entity e) { return e.getDtClass().getDtName().startsWith("CDOTA_DataRadiant"); }

    private boolean isPlayerCSGO(Entity e) { return e.getDtClass().getDtName().startsWith("DT_CSPlayer"); }

    private boolean isTime(Entity e) { return e.getDtClass().getDtName().equals("CDOTAGamerulesProxy"); }

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

    private void initialize(Entities entities) {

    }

    @OnEntityUpdated
    public void onUpdated(Context ctx, Entity e, FieldPath[] updatedPaths, int updateCount) {
        Float TIME_EPS = (float) 0.0001;

        if (!(isHero(e))) {
            return;
        }

        Entities entities = ctx.getProcessor(Entities.class);
        float real_time = getRealGameTimeSeconds(entities);

        if (real_time < TIME_EPS) {
            return;
        }

        for (int i = 0; i < updateCount; i++) {
            FieldPath path = updatedPaths[i];
            String pathName = e.getDtClass().getNameForFieldPath(path);
//            if (count.keySet().contains(pathName)) {
//                count.put(pathName, count.get(pathName) + 1);
//            } else {
//                count.put(pathName, 1);
//            }
            Entity player = ctx.getProcessor(Entities.class).getByHandle((int) e.getProperty("m_hOwnerEntity"));
            if (player == null) {
                System.out.println(e.getProperty("m_hOwnerEntity") + " " + e.getDtClass().getDtName() + " " + e.getProperty("m_hReplicatingOtherHeroModel"));
            }
//            int id = e.getProperty("m_iPlayerID");
//            if (id > 9 || id < 0) {
//                System.out.println("??????");
//            }
//            System.out.format("Entity: %s, Property: %s, Value: %s, ID: %s, Time: %s\n", e.getDtClass().getDtName(), pathName, e.getPropertyForFieldPath(path), e.getProperty("m_iPlayerID"), real_time);
        }
    }


    public void run(String[] args) throws Exception {
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
//        for (String pathName: count.keySet()) {
//            System.out.println(pathName + ": " + count.get(pathName));
//        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
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

import java.util.*;

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

    private int[] RadiantIndices = new int[5];

    private int[] DireIndices = new int[5];

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

    private void initializeMetaData(Entities entities, Context ctx) {
        Iterator<Entity> teams = entities.getAllByDtName("CDOTATeam");
        String initial = "m_aPlayers.000";

        while (teams.hasNext()) {
            Entity team = teams.next();
            if ((int) team.getProperty("m_iTeamNum") == 2) {
                for (int i = 0; i < 5; i++) {
                    String property = initial + i;
                    int handle = team.getProperty(property);
                    Entity player = ctx.getProcessor(Entities.class).getByHandle(handle);
                    RadiantIndices[i] = player.getProperty("m_iPlayerID");
                }

            } else if ((int) team.getProperty("m_iTeamNum") == 3) {
                for (int i = 0; i < 5; i++) {
                    String property = initial + i;
                    int handle = team.getProperty(property);
                    Entity player = ctx.getProcessor(Entities.class).getByHandle(handle);
                    DireIndices[i] = player.getProperty("m_iPlayerID");
                }
            }
        }
    }

    @OnEntityUpdated
    public void onUpdated(Context ctx, Entity e, FieldPath[] updatedPaths, int updateCount) {


        Float TIME_EPS = (float) 0.0001;
        if (!(isHero(e) || isDataDire(e) || isDataRadiant(e))) {
            return;
        }

        Entities entities = ctx.getProcessor(Entities.class);
        float real_time = getRealGameTimeSeconds(entities);

        if (real_time < TIME_EPS) {
            return;
        }

        if (!initialized) {
            initializeMetaData(entities, ctx);
            initialized = true;
            System.out.println(Arrays.toString(RadiantIndices));
            System.out.println(Arrays.toString(DireIndices));
        }

        for (int i = 0; i < updateCount; i++) {
            FieldPath path = updatedPaths[i];
            String pathName = e.getDtClass().getNameForFieldPath(path);
            if (pathName.equals("m_lifeState")) {
                System.out.format("Player ID: %s, FieldName: %s, UpdatedValue: %s, Time: %s\n", e.getProperty("m_iPlayerID"), pathName, e.getProperty("m_lifeState"), real_time);
            }
            if (pathName.endsWith("m_iTotalEarnedGold") || pathName.endsWith("m_iTotalEarnedXP")) {
                int index = Integer.parseInt(String.valueOf(pathName.charAt(17)));
                int playerID = -1;
                if (isDataRadiant(e)) {
                    playerID = RadiantIndices[index];
                } else if (isDataDire(e)) {
                    playerID = DireIndices[index];
                }
                System.out.format("Player ID: %s, FieldName: %s, UpdatedValue: %s, Time: %s\n", playerID, pathName, e.getProperty(pathName), real_time);
            }


//            if (count.keySet().contains(pathName)) {
//                count.put(pathName, count.get(pathName) + 1);
//            } else {
//                count.put(pathName, 1);
//            }
//            Entity player = ctx.getProcessor(Entities.class).getByHandle((int) e.getProperty("m_hOwnerEntity"));
//            if (player == null) {
//                System.out.println(e.getProperty("m_hOwnerEntity") + " " + e.getDtClass().getDtName() + " " + e.getProperty("m_hReplicatingOtherHeroModel"));
//            }
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
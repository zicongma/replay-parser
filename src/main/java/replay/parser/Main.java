package replay.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

import java.util.HashMap;

@UsesEntities
public class Main {
    HashMap<String, Integer> count = new HashMap<String, Integer>();

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private boolean isHero(Entity e) {
        return e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero");
    }

    @OnEntityUpdated
    public void onUpdated(Context ctx, Entity e, FieldPath[] updatedPaths, int updateCount) {
        if (!isHero(e)) {
            return;
        }

        for (int i = 0; i < updateCount; i++) {
            FieldPath path = updatedPaths[i];
            String pathName = e.getDtClass().getNameForFieldPath(path);
            if (count.keySet().contains(pathName)) {
                count.put(pathName, count.get(pathName) + 1);
            } else {
                count.put(pathName, 1);
            }
            System.out.format("Entity: %s, Property: %s, Value: %s, Tick: %s\n", e.getDtClass().getDtName(), pathName, e.getPropertyForFieldPath(path), ctx.getTick());
        }


    }


    public void run(String[] args) throws Exception {
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        for (String pathName: count.keySet()) {
            System.out.println(pathName + ": " + count.get(pathName));
        }

    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
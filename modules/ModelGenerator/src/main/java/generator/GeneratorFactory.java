package main.java.generator;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Schema;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-8-28.
 * Time: 14:31.
 */
public class GeneratorFactory {

    private Schema schema;

    public static void main(String args[]) throws Exception {
        GeneratorFactory factory = new GeneratorFactory();
        factory.generate();
    }

    public GeneratorFactory() {
        schema = new Schema(1, "com.inovance.elevatorcontrol.entity");
    }

    public void generate() throws Exception {
        DaoGenerator daoGenerator = new DaoGenerator();
        daoGenerator.generateAll(schema, "../ElevatorControl/src");
    }
}

package routeguide;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerConfig {

    public static void setLoggerLevel() {
        Logger logger = Logger.getLogger("io.grpc");
        logger.setLevel(Level.FINEST);
    }
}

package github.mattys1.autoconnect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {
    private final static Logger logger = LogManager.getLogger(Reference.MOD_NAME);

    public static void info(String message, Object... params) {
        logger.info(message, params);
    }
    
    public static void debug(String message, Object... params) {
        logger.debug(message, params);
    }
    
    public static void warn(String message, Object... params) {
        logger.warn(message, params);
    }
    
    public static void error(String message, Object... params) {
        logger.error(message, params);
    }
    
    public static void fatal(String message, Object... params) {
        logger.fatal(message, params);
    }
    
    public static void trace(String message, Object... params) {
        logger.trace(message, params);
    }
}

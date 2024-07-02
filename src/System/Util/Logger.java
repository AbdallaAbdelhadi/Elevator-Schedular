package System.Util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Implements a logger utility class to log
 * events with source name and associated time.
 * @author Yousef Yassin
 */
public class Logger {
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private String NAME;

    /**
     * Creates a new logger with the specified name.
     * @param name String, the logger's source name.
     */
    public Logger(String name) {
        this.NAME = name;
    }

    /**
     * Returns a log prefix consisting of the current
     * date and time along with the logger's name.
     * @return String, the prefix.
     */
    private String getPrefix() {
        Date date = new Date();
        return formatter.format(date) + " - " + "[ " + this.NAME + " ]";
    }

    /**
     * Logs the specified message along with
     * the log prefix (time and source name).
     * @param msg String, the message to log.
     */
    public void log(String msg) {
        System.out.println(getPrefix() + " " + msg);
    }
}
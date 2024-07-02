package System.Util;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Timing Measurement Instrumentation
 * @author Ibrahim Almalki
 */
public class Chrono {
	/**
     * The path of the project
     */
    private static final String ABSOLUTE_PATH = new File("").getAbsolutePath();
    
    /**
     * The name of the file indicating what it contains
     */
    private final String NAME;
    
    /**
     * The name of the thread being measured
     */
    private final String LOG_FILE;
    
    /**
     * Logger object used to log events
     */
    private final Logger logger;

    /**
     * the start time of a process
     */
    private long initialTime;
    
    /**
     * The end time of a process
     */
    private long finalTime;

    /**
     * timer outlier
     */
    private final long CUTOFF_NS = 3000000000L;

    /**
     * Instantiates a new Chrono.
     *
     * @param name    the name
     * @param logFile the log file
     */
    public Chrono(String name, String logFile) {
        this.NAME = name;
        this.LOG_FILE = logFile;
        this.logger = Logger.getLogger(name);
        initLogger();
    }

    /**
     * Creates a file and adds all the measurments asscoiated with a thread to it
     */
    private void initLogger() {
        FileHandler fh;
        SimpleFormatter formatter = new SimpleFormatter();
        File file = new File(ABSOLUTE_PATH + "\\src\\Measurements\\" + LOG_FILE + "-" + NAME + ".txt");
        try {
            file.getParentFile().mkdirs();
            if (!file.exists()) {
                file.createNewFile(); // if file already exists will do nothing
            }
            fh = new FileHandler(ABSOLUTE_PATH + "\\src\\Measurements\\" + LOG_FILE + "-" + NAME + ".txt");
            fh.setFormatter(formatter);

            this.logger.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Measures the start time
     */
    public void start() {
        this.initialTime = System.nanoTime();
    }

    /**
     * Measures the end time
     */
    public void end() {
        this.finalTime = System.nanoTime();
    }

    /**
     * Calculates the time in which a procees was using
     *
     * @param prefix, The name of the the thread being measured
     */
    public void logElapsed(String prefix) {
        long elapsedNanos = (this.finalTime - this.initialTime);

        if (elapsedNanos > CUTOFF_NS) { // Timeout outlier
            return;
        }
        // this.logger.info(prefix + "- Handler time (ns): " + elapsedNanos);
    }
}

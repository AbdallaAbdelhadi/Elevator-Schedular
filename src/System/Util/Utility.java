package System.Util;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Static utility class with helper methods.
 * @author Yousef Yassin
 */
public class Utility {
    public static final int SECONDS_TO_MILLISECONDS = 1000;

    /**
     * Time formatter.
     */
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.S");

    /**
     * Converts the specified string into a time.
     * @param timeString String, the time string.
     * @return Time, the time.
     */
    public static Time stringToTime(String timeString) {
        try {
            Date date = sdf.parse(timeString);
            return new Time(date.getTime());
        } catch (ParseException pe) {
            pe.printStackTrace();
        }

        return null;
    }
}

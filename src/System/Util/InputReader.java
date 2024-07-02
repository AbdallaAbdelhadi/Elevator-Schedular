package System.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Defines Input Reader utility class
 * to help read and extract data from a specified file.
 * @Author Zakariyya Almalki
 */
public class InputReader {
    // The absolute path to the directory where the program is run (used to get absolute path to file).
    private static final String ABSOLUTE_PATH = new File("").getAbsolutePath();
    private ArrayList<ArrayList<String>> fileData = new ArrayList<>();  // Buffer to store read data.

    /**
     * Load data from specified textfile.
     *
     * @param fileName String, the name of the file to read.
     * @throws IOException Exception, filreader IO exception.
     */
    public void loadData(String fileName) throws IOException {
        // File path is passed as parameter, assumed to be in /src.
        System.out.println(ABSOLUTE_PATH + "\\src\\" + fileName);
        File file = new File(ABSOLUTE_PATH + "\\src\\" + fileName);

        // Open file under buffered reader.
        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;
        // Condition holds true until there is no character in the string
        while ((st = br.readLine()) != null) {
            // Data is comma delimeted, split it to seperate columns and
            // add each row as an ArrayList entry.
            fileData.add(new ArrayList(Arrays.asList(st.split(", "))));
        }
    }

    /**
     * Gets file data.
     *
     * @return ArrayList<ArrayList<String>>, the parsed data.
     */
    public ArrayList<ArrayList<String>> getFileData() {
        return fileData;
    }
}

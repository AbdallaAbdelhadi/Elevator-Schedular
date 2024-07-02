package System.Floor;

import System.Util.*;
import System.Config;
import jdk.jshell.execution.Util;
import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Implements Floor type that reads request input from
 * a text file, parses the requests and sends them to the scheduler.
 * @Author Abdalla Abdelhadi, Zakariyya Almalki
 */
public class Floor {
    private ArrayList<ArrayList<String>> inputData;                 // Storage buffer for parsed request data.
    private HashMap<String,Integer> headers = new HashMap<>();      // Defines column headers name to col number from input file.
    private String name;                                              // This floor's id.
    private String inputFilename;                                   // Callback that this elevator.
    private DuplexSocket duplexPacket;                              // Duplex send receive socket utility.
    private Logger logger;                                          // System logger.
    private static final SimpleDateFormat sdf =                     // Time formatter.
            new SimpleDateFormat("HH:mm:ss.S");

    /* DEVELOPMENT - DEBUG */
    private static final boolean DEBUG = true;

    /**
     * Creates a new floor with the specified parameters.
     *
     * @param name String, this floor system's name.
     */
    public Floor(String name, String inputFilename, int receivePortNum, int sendPortNum, InetAddress sendAddress, int timeout) {
        this.name = name;
        this.inputFilename = inputFilename;
        this.logger = new Logger(this.name);
        this.duplexPacket = new DuplexSocket(receivePortNum, sendPortNum, sendAddress, this.logger, timeout);
    }

    /**
     * Gets input data.
     *
     * @return ArrayList<ArrayList<String>>, the parsed input data.
     */
    public ArrayList<ArrayList<String>> getInputData() {
        return this.inputData;
    }

    /**
     * Loads data from the input file.
     *
     * @throws IOException Exception, filereader IO exception.
     */
    public void loadData() throws IOException {
        InputReader r = new InputReader();
        r.loadData(this.inputFilename);                 // Open the specified input file.
        this.inputData = r.getFileData();
        this.initHeaders();                             // Initialized column header names

        if (DEBUG) {
            String listString = this.inputData.stream().map(Object::toString)
                    .collect(Collectors.joining(", "));
            this.logger.log("Loaded data.");
            this.logger.log(listString);
        }
    }

    /**
     * Initializes map from column header names to
     * column number.
     */
    private void initHeaders() {
        // Column header names are in first row (0)
        ArrayList<String> headersList = inputData.get(0);

        // Map each header name to its column number.
        for (int i = 0; i < headersList.size(); i++) {
            this.headers.put(headersList.get(i).toLowerCase(), i);
        }

        if (DEBUG) {
            this.logger.log("Initialized headers.");
            this.logger.log(this.headers.toString());
        }
    }

    /**
     * Creates JSON packet from specified input row data.
     *
     * @param rowIdx int, the index of the row to create the packet from.
     * @return DataPacket, the data packet holding the row's data.
     */
    private JSONObject createPacketFromRow(int rowIdx) {
        ArrayList<String> rowData = inputData.get(rowIdx);
        //System.out.println(rowData.toString());

        String time = rowData.get(headers.get("time"));
        String floor = rowData.get(headers.get("floor"));
        String floorButton = rowData.get(headers.get("floor button"));
        String cartButton = rowData.get(headers.get("car button"));
        String error = rowData.get(headers.get("error"));

        return JSONPacket.createPacket(new HashMap<>(){{
            put(Config.K_TIME, time);
            put(Config.K_FLOOR, floor);
            put(Config.K_FLOOR_BUTTON, floorButton);
            put(Config.K_DESTINATION_FLOOR, cartButton);
            put(Config.K_ERROR, error.toUpperCase());
        }});
    }

    /**
     * Returns the difference in times between timeA and timeB
     * as a list containing [deltaHours, deltaMinutes, deltaSeconds].
     *
     * @param timeA Time, the initial time.
     * @param timeB Time, the final time.
     * @return ArrayList<Integer>, the difference between the times.
     */
    private long deltaTime(Time timeA, Time timeB) {
        return timeB.getTime() - timeA.getTime();
    }


    /**
     * Defines this Floor thread's main task: reads and parses
     * data from the specified input file and sends it to the scheduler. Then
     * waits for the data to be resent back.
     */
    public void run() {
        final int SECONDS_PER_MINUTE = 60;
        int cursorIdx = 1;
        boolean success = false;
        JSONObject sendPacket, dataPacket;
        DatagramPacket receivePacket;
        Time previousTime = null, currentTime = null;


        try {                       // Load input requests
            this.loadData();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(cursorIdx < inputData.size()){
            success = false;
            sendPacket = createPacketFromRow(cursorIdx);
            cursorIdx++;

            currentTime = Utility.stringToTime(sendPacket.getString(Config.K_TIME));

            if (previousTime == null) {
                previousTime = currentTime;
            }

            long delay = this.deltaTime(previousTime, currentTime) / SECONDS_PER_MINUTE;

            previousTime = currentTime;

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while (!success) {
                this.logger.log("Sending request");
                this.logger.log("Request: " + sendPacket);
                duplexPacket.send(JSONPacket.serialize(sendPacket));


                this.logger.log("Receiving Ack");
                try {
                    receivePacket = duplexPacket.receive();
                    dataPacket = JSONPacket.deserialize(receivePacket.getData(), receivePacket.getLength());
                    this.logger.log(dataPacket.toString());
                    success = true;
                } catch (SocketTimeoutException e) {
                    this.logger.log("Timed out.");
                }
            }
        }

        this.logger.log("Sent all simulated requests. Exiting.");
    }

    /**
     * Floor system entry point.
     * @param args String[], command line args (unused).
     */
    public static void main(String[] args) {
        final String NAME = "Floor";
        final String DATA_FILE = "data.txt";

        try {
            Floor floor = new Floor(
                    NAME,
                    DATA_FILE,
                    Config.FLOOR_PORT,
                    Config.SCHEDULER_PORT,
                    InetAddress.getLocalHost(),
                    Config.ELEVATOR_TIMEOUT * Utility.SECONDS_TO_MILLISECONDS
            );

            floor.run();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

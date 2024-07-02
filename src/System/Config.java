package System;

/**
 * Defines the port each subsystem is listening on.
 */
public class Config {
    /* General System Properties */
    public static final int MAX_FLOOR = 22;
    public static final int FLOOR_PORT = 150;
    public static final int SCHEDULER_PORT = 700;
    public static final int ELEVATOR_BASE_PORT = 6000;
    public static final int ELEVATOR_INCREMENT = 20;
    public static final int NUMBER_ELEVATORS = 4;
    public static final int ELEVATOR_TIMEOUT = 20; // seconds;

    /* Elevator Properties */
    /**
     * Time to open/close doors in seconds.
     */
    public static final int DOOR_ACTION_TIME = 3;

    /**
     * Time to unjam doors in seconds.
     */
    public static final int DOOR_UNJAMMING_TIME = 3;

    /**
     * Time to let passengers board.
     */
    public static final int BOARDING_TIME = 5;

    /**
     * Acceleration of the elevator
     */
    public static final double ACCELERATION = 0.3;

    /**
     * Top speed of the elevator
     */
    public static final double TOP_SPEED = 2.33;

    /**
     * The distance between adjacent floors
     */
    public static final long DISTANCE_BETWEEN_FLOOR = 4;

    /**
     * Time in which the elevator is accelerating
     */
    public static final double ACCELERATION_TIME = 7.78;

    /**
     * Distance covered while accelerating
     */
    public static final int ACCELERATION_DISTANCE = 9;


    /* JSON Keys */
    public static final String K_TIME = "time";
    public static final String K_FLOOR = "floor";
    public static final String K_FLOOR_BUTTON = "floorButton";
    public static final String K_DESTINATION_FLOOR = "destinationFloor";
    public static final String K_ACK = "ack";
    public static final String K_ERROR = "error";
    public static final String K_TOPIC = "topic";
    public static final String K_COMMAND = "command";

    public static final String UPDATE_FLOOR_MSG = "update-floor";
    public static final String REQUEST_FLOOR_MSG = "request-floor";
    public static final String OPENING_DOORS_MSG = "open-door";
    public static final String ERROR_TYPE_MSG = "error-type";
    public static final String COMMAND_MSG = "command";

    public static final String UNLOCK_DOOR_COMMAND = "unlock-door";
    public static final String TERMINATE_COMMAND = "terminate";
}
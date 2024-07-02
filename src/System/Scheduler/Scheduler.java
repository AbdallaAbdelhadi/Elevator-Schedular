package System.Scheduler;

import GUI.SystemGUI;
import System.Util.DuplexSocket;
import System.Util.JSONPacket;
import System.Util.Logger;
import System.Config;
import System.Util.Utility;
import Types.ElevatorDirection;
import Types.ElevatorState;
import Types.SchedulerState;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * The system scheduler to accept and queue 
 * requests from the floor and send them scheduled to
 * the elevators.
 * @author Yousef Yassin, Zakariyya Almalki, Abdalla Abdelhadi
 *
 */
public class Scheduler {
    /**
     * The floor duplex socket.
     */
    private DuplexSocket floorSocket;

    /**
     *  Floor subsystem port and elevator base port.
     */
    private final int FLOOR_PORT, BASE_ELEVATOR_PORT;

    /**
     * The number of elevators to schedule.
     */
    private final int NUM_ELEVATORS;

    /**
     * The local host
     */
    private final InetAddress sendAddress;

    /**
     * This scheduler's logger.
     */
    private final Logger logger;

    /**
     * This scheduler's name.
     */
    private final String NAME;

    /**
     * The elevator listener socket timeout.
     */
    private final int ELEVATOR_TIMEOUT;

    /**
     * Listener socket ports for all elevators.
     */
    private final DuplexSocket[] elevatorListenerSockets;
    
    /**
     * Scheduling states for all elevators.
     */
    private final SchedulerState[] schedulerElevatorState;
    
    /**
     * Termination floors for elevator.
     */
    private final int[] elevatorTerminationFloors;

    /**
     * The system's gui (updated by the scheduler).
     */
    private final SystemGUI gui;

    /**
     * The scheduling controller that implements the scheduling
     * algorithm for the system.
     */
    private final SchedulerController schedulerController;

    /**
     * True if this is a dut, false otherwise.
     */
    private final boolean testing;

    /**
     * Creates a new scheduler with the specified parameters.
     * @param name String, the scheduler's name.
     * @param maxFloor int, the highest floor in the system.
     * @param elevatorTimeout int, the time in ms to wait before timing 
     * 		  out on socket receive.
     * @param floorPort int, the floor listener port.
     * @param baseElevatorPort int, the elevator base listening port.
     * @param numElevators int, the number of elevators in the system.
     * @param sendAddress InetAddress, the address to send to.
     * @param gui SystemGUI, the system gui.
     * @param testing boolean, true if this is a dut.
     */
    public Scheduler(String name, int maxFloor, int elevatorTimeout, int floorPort, int baseElevatorPort, int numElevators, InetAddress sendAddress, SystemGUI gui, boolean testing) {
        this.FLOOR_PORT = floorPort;
        this.BASE_ELEVATOR_PORT = baseElevatorPort;
        this.NUM_ELEVATORS = numElevators;
        this.NAME = name;
        this.ELEVATOR_TIMEOUT = elevatorTimeout;
        this.schedulerController = new SchedulerController(maxFloor, numElevators);
        this.testing = testing;

        this.sendAddress = sendAddress;
        this.logger = new Logger(NAME);
        this.gui = gui;

        if (!this.testing) {
        	
        }
        this.elevatorListenerSockets = new DuplexSocket[numElevators];
        this.schedulerElevatorState = new SchedulerState[numElevators];
        this.elevatorTerminationFloors = new int[numElevators];

        // Create the floor duplex socket
        if (!this.testing) {
        	this.floorSocket = new DuplexSocket(Config.SCHEDULER_PORT, Config.FLOOR_PORT, this.sendAddress, this.logger);
        } else {
        	this.floorSocket = null;
        }
        
    }

    /**
     * Displays the system's gui.
     */
    public void display() {
        this.gui.display();
    }

    /**
     * Returns all the elevators' scheduling states.
     * @return
     */
    public SchedulerState[] getSchedulerElevatorState() {
        return this.schedulerElevatorState;
    }

    /**
     * Starts the scheduler's socket threads.
     */
    public void start() {
        this.startFloorListener();
        this.startElevatorListener();
    }

    /* --- FLOOR HANDLER ----------------------------------------------------------------------- */

    /**
     * Starts a floor listener thread.
     */
    private void startFloorListener() {
        Runnable listener = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        floorHandler();
                    } catch (SocketTimeoutException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        };

        this.logger.log("Starting floor listener thread...");
        Thread runnableThread = new Thread(listener);
        runnableThread.start();
    }

    /**
     * Handles floor requests by adding them to the scheduler controller.
     */
    private void floorHandler() throws SocketTimeoutException {
        DatagramPacket receivePacket;
        JSONObject jsonPacket;

        // Block until a packet is received.
        this.logger.log("Listening for Floor Packet...");
        receivePacket = floorSocket.receive();
        jsonPacket = JSONPacket.deserialize(receivePacket.getData(), receivePacket.getLength());
        this.logger.log("Received Floor packet: " + jsonPacket);

        // Send ack
        JSONObject ackPacket = JSONPacket.createPacket(new HashMap<>(){{
            put(Config.K_ACK, true);
        }});
        floorSocket.send(JSONPacket.serialize(ackPacket));

        this.logger.log("Sent ACK Packet to [ Floor ]");

        int pickupFloor = jsonPacket.getInt(Config.K_FLOOR);
        int destFloor = jsonPacket.getInt(Config.K_DESTINATION_FLOOR);
        ElevatorState error = ElevatorState.valueOf(jsonPacket.getString(Config.K_ERROR));
        // Add request to queue
        this.schedulerController.addRequest(pickupFloor, destFloor, error);
    }


    /* --- ELEVATOR HANDLER ----------------------------------------------------------------------- */
    /**
     * Starts elevator listener threads.
     */
    private void startElevatorListener() {
        Thread runnableThread;

        for (int i = 0; i < this.NUM_ELEVATORS; i++) {
            int sendingPortNum = Config.ELEVATOR_BASE_PORT + i * Config.ELEVATOR_INCREMENT; // The port the elevator listens to
            final int elevatorID = i;

            Logger logger = new Logger("Scheduler-elev-port-" + (i));

            // Init sockets
            final DuplexSocket elevatorSocket = new DuplexSocket(
                    Config.SCHEDULER_PORT + i + 1,
                    sendingPortNum,
                    this.sendAddress,
                    logger,
                    ELEVATOR_TIMEOUT
            );
            this.elevatorListenerSockets[elevatorID] = elevatorSocket;

            // Init states
            this.schedulerElevatorState[elevatorID] = SchedulerState.WAIT;

            Runnable listener = new Runnable() {
                @Override
                public void run() {
                    boolean run = true;
                    while (run) {
                        run = elevatorHandler(elevatorID, elevatorSocket, logger);
                    }
                }
            };

            this.logger.log("Starting elevator-" + (i) + " listener thread...");
            runnableThread = new Thread(listener);
            runnableThread.start();
        }
    }

    /**
     * Handles floor requests by adding them to the scheduler controller.
     * @param elevatorID int, id of the elevator to handle.
     * @param elevatorSocket DuplexSocket, the elevator to handle's socket.
     * @param elevatorLogger Logger, the elevator to handle's logger.
     * @return boolean, true if handled successfully, false otherwise.
     */
    public boolean elevatorHandler(int elevatorID, DuplexSocket elevatorSocket, Logger elevatorLogger) {
        return this.elevatorHandler(elevatorID, elevatorSocket, elevatorLogger, "");
    }
    
    /**
     * Handles floor requests by adding them to the scheduler controller with testing param injection.
     * @param elevatorID int, id of the elevator to handle.
     * @param elevatorSocket DuplexSocket, the elevator to handle's socket.
     * @param elevatorLogger Logger, the elevator to handle's logger.
     * @param testParam String, the test injection.
     * @return boolean, true if handled successfully, false otherwise.
     */
    public boolean elevatorHandler(int elevatorID, DuplexSocket elevatorSocket, Logger elevatorLogger, String testParam) {
        SchedulerState state = this.schedulerElevatorState[elevatorID];
        DatagramPacket receivePacket;
        JSONObject jsonPacket;

        if (state != SchedulerState.RECEIVE && state != SchedulerState.WAIT) {
            return this.handleErrors(state, elevatorID, elevatorSocket, elevatorLogger, testParam);
        }
        if (this.testing) return this.controller(state, null, elevatorID, elevatorSocket, elevatorLogger);

        // Block until a packet is received.
        elevatorLogger.log("Listening for Elevator Packet...");

        try {
            receivePacket = elevatorSocket.receive();
        } catch (SocketTimeoutException e) {
            elevatorLogger.log("Timed out. Proceeding to send status request state");
            this.schedulerElevatorState[elevatorID] = SchedulerState.SEND_STATUS_REQ;
            return true;
        }

        jsonPacket = JSONPacket.deserialize(receivePacket.getData(), receivePacket.getLength());
        elevatorLogger.log("Received elevator packet: " + jsonPacket);

        // Handle the elevator packet.
        return this.controller(state, jsonPacket, elevatorID, elevatorSocket, elevatorLogger);
    }

    /**
     * Handle's elevator error.
     * @param state SchedulerState, the elevator to handle's scheduling state.
     * @param elevatorID int, the elevator to handle's id.
     * @param elevatorSocket DuplexSocket, the socket of the elevator to handle.
     * @param elevatorLogger Logger, the elevator to handle's logger.
     * @return boolean, true if handle success, false otherwise.
     */
    private boolean handleErrors(SchedulerState state, int elevatorID, DuplexSocket elevatorSocket, Logger elevatorLogger) {
        return this.handleErrors(state, elevatorID, elevatorSocket, elevatorLogger, "");
    }

    /**
     * Handle's elevator error with test injection.
     * @param state SchedulerState, the elevator to handle's scheduling state.
     * @param elevatorID int, the elevator to handle's id.
     * @param elevatorSocket DuplexSocket, the socket of the elevator to handle.
     * @param elevatorLogger Logger, the elevator to handle's logger.
     * @param testParam String, the test injection.
     * @return boolean, true if handle success, false otherwise.
     */
    public boolean handleErrors(SchedulerState state, int elevatorID, DuplexSocket elevatorSocket, Logger elevatorLogger, String testParam) {
        switch (state) {
            case SEND_STATUS_REQ: {
                this.schedulerElevatorState[elevatorID] = SchedulerState.LISTEN_FOR_STATUS_REPLY;

                if (this.testing) return true;

                JSONObject ackPacket = JSONPacket.createPacket(new HashMap<>(){{
                    put(Config.K_ACK, true);
                }});

                elevatorSocket.send(JSONPacket.serialize(ackPacket));
                elevatorLogger.log("Sent Status request to elevator.");
                elevatorLogger.log(ackPacket.toString());

                return true;
            }

            case LISTEN_FOR_STATUS_REPLY: {
                DatagramPacket receivePacket;
                JSONObject jsonPacket;

                if (this.testing) {
                    switch (testParam) {
                        case "doorJam" : {
                            this.schedulerElevatorState[elevatorID] = SchedulerState.SEND_UNLOCK_DOORS;
                            return true;
                        }

                        case "stuckFloor" : {
                            this.schedulerElevatorState[elevatorID] = SchedulerState.SEND_TERMINATE;
                            return true;
                        }

                        default: {
                            this.schedulerElevatorState[elevatorID] = SchedulerState.RECEIVE;
                            return true;
                        }
                    }
                }

                try {
                    receivePacket = elevatorSocket.receive();
                } catch (SocketTimeoutException e) {
                    elevatorLogger.log("Timed out. Proceeding to send status request state");
                    this.schedulerElevatorState[elevatorID] = SchedulerState.SEND_STATUS_REQ;
                    return true;
                }

                jsonPacket = JSONPacket.deserialize(receivePacket.getData(), receivePacket.getLength());
                elevatorLogger.log("Received elevator status reply: " + jsonPacket);

                ElevatorState error = ElevatorState.valueOf(jsonPacket.getString(Config.K_ERROR));
                int floor = jsonPacket.getInt(Config.K_FLOOR);

                switch (error) {
                    case DOOR_JAM: {
                        this.schedulerElevatorState[elevatorID] = SchedulerState.SEND_UNLOCK_DOORS;
                        this.gui.setElevatorError(elevatorID, floor, ElevatorState.DOOR_JAM);
                        return true;
                    }

                    case STUCK_FLOOR: {
                        this.schedulerElevatorState[elevatorID] = SchedulerState.SEND_TERMINATE;
                        this.elevatorTerminationFloors[elevatorID] = floor;
                        this.gui.setElevatorError(elevatorID, floor, ElevatorState.STUCK_FLOOR);
                        return true;
                    }

                    default: {
                        this.schedulerElevatorState[elevatorID] = SchedulerState.RECEIVE;
                        return true;
                    }
                }
            }

            case SEND_UNLOCK_DOORS: {
                this.schedulerElevatorState[elevatorID] = SchedulerState.RECEIVE;
                if (this.testing) return true;

                JSONObject ackPacket = JSONPacket.createPacket(new HashMap<>(){{
                    put(Config.K_TOPIC, Config.COMMAND_MSG);
                    put(Config.K_COMMAND, Config.UNLOCK_DOOR_COMMAND);
                }});

                elevatorSocket.send(JSONPacket.serialize(ackPacket));
                elevatorLogger.log("Sent unlock door command to elevator.");
                elevatorLogger.log(ackPacket.toString());

                return true;
            }

            case SEND_TERMINATE: {
                this.schedulerElevatorState[elevatorID] = SchedulerState.TERMINATED;
                if (this.testing) return false;

                JSONObject ackPacket = JSONPacket.createPacket(new HashMap<>(){{
                    put(Config.K_TOPIC, Config.COMMAND_MSG);
                    put(Config.K_COMMAND, Config.TERMINATE_COMMAND);
                }});

                elevatorSocket.send(JSONPacket.serialize(ackPacket));
                elevatorLogger.log("Sent terminate command to elevator.");
                elevatorLogger.log(ackPacket.toString());

                this.schedulerController.processError(elevatorID);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                this.gui.setElevatorError(elevatorID, this.elevatorTerminationFloors[elevatorID], ElevatorState.TERMINATED);
                return false;
            }

            default: {
                return true;
            }
        }
    }

    /**
     * Primary elevator handler (non-error).
     * @param state Scheduler, the state of elevator to handle.
     * @param json JSONObject, the message received to handle.
     * @param elevatorID int, the id of elevator to handle.
     * @param elevatorSocket DuplexSocket, the socket of elevator to handle.
     * @param elevatorLogger Logger, the logger of elevator to handle.
     * @return boolean, true if handle success, false otherwise.
     */
    public boolean controller(SchedulerState state, JSONObject json, int elevatorID, DuplexSocket elevatorSocket, Logger elevatorLogger) {
        switch(state) {
            case WAIT: {}   // Cascade
            case RECEIVE: {
                this.schedulerElevatorState[elevatorID] = SchedulerState.RECEIVE;
                if (this.testing) return true;

                boolean success = this.handlePacket(state, json, elevatorID, elevatorSocket, elevatorLogger);
                return success;
            }

            default: {
                return false;
            }
        }
    }

    /**
     * Processes and handles correct response for elevator message.
     * @param state Scheduler, the state of elevator to handle.
     * @param json JSONObject, the message received to handle.
     * @param elevatorID int, the id of elevator to handle.
     * @param elevatorSocket DuplexSocket, the socket of elevator to handle.
     * @param elevatorLogger Logger, the logger of elevator to handle.
     * @return boolean, true if handle success, false otherwise.
     */
    public boolean handlePacket(SchedulerState state, JSONObject json, int elevatorID, DuplexSocket elevatorSocket, Logger elevatorLogger) {
        String topic = json.getString(Config.K_TOPIC);


        switch(topic) {
            case Config.UPDATE_FLOOR_MSG: {
                int floor = json.getInt(Config.K_FLOOR);
                int previousFloor;
                ElevatorDirection direction =
                        ElevatorDirection.valueOf(json.getString(Config.K_FLOOR_BUTTON));

                if (direction == ElevatorDirection.UP) {
                    previousFloor = floor - 1;
                } else {
                    previousFloor = floor + 1;
                }

                this.gui.deactivateElevator(elevatorID, previousFloor);
                this.gui.updateElevatorPosition(elevatorID, floor);

                this.sendElevatorAck(elevatorID, elevatorSocket, elevatorLogger);
                return true;
            }

            case Config.OPENING_DOORS_MSG: {
                int floor = json.getInt(Config.K_FLOOR);
                int previousFloor;
                ElevatorDirection direction =
                        ElevatorDirection.valueOf(json.getString(Config.K_FLOOR_BUTTON));

                if (direction == ElevatorDirection.UP) {
                    previousFloor = floor - 1;
                } else {
                    previousFloor = floor + 1;
                }

                this.gui.deactivateElevator(elevatorID, previousFloor);
                this.gui.activateElevator(elevatorID, floor, direction);

                this.sendElevatorAck(elevatorID, elevatorSocket, elevatorLogger);
                return true;
            }

            case Config.REQUEST_FLOOR_MSG: {
                int floor = json.getInt(Config.K_FLOOR);
                ElevatorDirection direction =
                        ElevatorDirection.valueOf(json.getString(Config.K_FLOOR_BUTTON));
                this.handleElevatorFloorAck(state, elevatorID, elevatorSocket, elevatorLogger, floor, direction);
                return true;
            }

            default: {
                return false;
            }
        }
    }

    /**
     * Handles and elevator send request ack by fetching the next request.
     * @param state Scheduler, the state of elevator to handle.
     * @param elevatorID int, the id of elevator to handle.
     * @param elevatorSocket DuplexSocket, the socket of elevator to handle.
     * @param elevatorLogger Logger, the logger of elevator to handle.
     * @param currentFloor int, the elevator's current floor.
     * @param direction ElevatorDirection, the elevator's direction.
     */
    public void handleElevatorFloorAck(SchedulerState state, int elevatorID, DuplexSocket elevatorSocket, Logger elevatorLogger, int currentFloor, ElevatorDirection direction) {
        boolean success;

        switch (state) {
            case WAIT: {
                success = this.sendNextFloor(elevatorID, elevatorSocket, elevatorLogger, currentFloor, direction);

                if (!success) {
                    this.gui.activateElevator(elevatorID, currentFloor, ElevatorDirection.INACTIVE);
                }

                break;
            }

            case RECEIVE: {
                elevatorLogger.log("ACK Floor:" + currentFloor);

                // Ack floor then send next floor
                this.schedulerController.ackFloor(elevatorID, currentFloor);
                success = this.sendNextFloor(elevatorID, elevatorSocket, elevatorLogger, currentFloor, direction);

                if (!success) {
                    this.gui.activateElevator(elevatorID, currentFloor, ElevatorDirection.INACTIVE);
                }

                break;
            }
        }

    }

    /**
     * Sends the next floor message to elevator.
     * @param elevatorID int, id of elevator to handle.
     * @param elevatorSocket DuplexSocket, socket of elevator to handle.
     * @param elevatorLogger Logger, logger of elevator to handle.
     * @param currentFloor int, the elevator's current floor.
     * @param direction ElevatorDirection, the elevator's direction.
     * @return boolean, true if got next floor, false otherwise.
     */
    public boolean sendNextFloor(int elevatorID, DuplexSocket elevatorSocket, Logger elevatorLogger, int currentFloor, ElevatorDirection direction) {
        Integer nextFloor = this.schedulerController.getNextFloor(elevatorID, currentFloor, direction);

        if (nextFloor == null) {
            return false;
        }

        ElevatorState error = this.schedulerController.getNextError(elevatorID, nextFloor);

        JSONObject ackPacket = JSONPacket.createPacket(new HashMap<>(){{
            put(Config.K_ACK, true);
            put(Config.K_DESTINATION_FLOOR, nextFloor);
            put(Config.K_ERROR, error);
        }});

        elevatorSocket.send(JSONPacket.serialize(ackPacket));
        elevatorLogger.log("Sent ACK Packet to elevator.");
        elevatorLogger.log(ackPacket.toString());
        return true;
    }

    public void sendElevatorAck(int elevatorID, DuplexSocket elevatorSocket, Logger elevatorLogger) {
        JSONObject ackPacket = JSONPacket.createPacket(new HashMap<>(){{
            put(Config.K_ACK, true);
        }});

        elevatorSocket.send(JSONPacket.serialize(ackPacket));
        elevatorLogger.log("Sent ACK Packet to elevator.");
    }

    /**
     * The scheduler's main entrypoint.
     * @param args String[], command line args.
     */
    public static void main(String[] args) {
        final String NAME = "Scheduler";
        InetAddress HOST;

        try {
            HOST = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException("[SCHEDULER] Couldn't acquire the local host.");
        }

        SystemGUI gui = new SystemGUI(Config.MAX_FLOOR, Config.NUMBER_ELEVATORS);
        Scheduler scheduler = new Scheduler(
                NAME,
                Config.MAX_FLOOR,
                Config.ELEVATOR_TIMEOUT * Utility.SECONDS_TO_MILLISECONDS,
                Config.FLOOR_PORT,
                Config.SCHEDULER_PORT,
                Config.NUMBER_ELEVATORS,
                HOST,
                gui,
                false
        );

        scheduler.display();
        scheduler.start();
    }
}

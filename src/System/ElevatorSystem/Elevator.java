package System.ElevatorSystem;

import System.Util.Chrono;
import System.Util.DuplexSocket;
import System.Util.JSONPacket;
import System.Util.Logger;
import Types.ElevatorDirection;
import Types.ElevatorState;
import org.json.JSONObject;
import System.Config;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import static java.lang.Math.abs;

/**
 * Implements an elevator.
 * @author Yousef Yassin
 */
public class Elevator implements Runnable {
    /**
     * This elevator's send receive socket.
     */
    private DuplexSocket socket;

    /**
     * This elevator's state;
     */
    private ElevatorState state;

    /**
     * This elevator's current floor.
     */
    private int currentFloor;

    /**
     * This elevator's destination floor.
     */
    private int destinationFloor;

    /**
     * The elevator's current error type;
     */
    private ElevatorState currentErrorType;

    /**
     * This elevator's id.
     */
    private int id;

    /**
     * Conversion seconds to millisecond.
     */
    private final long SECONDS_TO_MILLI;

    /**
     * This elevator's logger.
     */
    private final Logger logger;

    /**
     * True if this is a dut.
     */
    private boolean testing;
    
    /**
     * This elevator's current direction.
     */
    private ElevatorDirection direction;

    /**
     * Timing instrumentation.
     */
    private Chrono chrono;

    /**
     * Creates a new elevator with the specified parameters.
     * @param receivingPort int, the port the elevator listens to.
     * @param sendPortNum int, the scheduler port.
     * @param sendAddress int, the scheduler address.
     * @param elevatorID int, the elevator's id.
     * @param testing boolean, true if this elevator is being tested.
     * @param timeout int, the amount of ms to timeout after a socket receive.
     */
    public Elevator (int receivingPort, int sendPortNum, InetAddress sendAddress, int elevatorID, boolean testing, int timeout) {
        this.id = elevatorID;
        this.logger = new Logger("Elevator-" + this.id);
        this.chrono = new Chrono("Elevator-" + this.id, "Timing-measurement");
        this.testing = testing;

        
        if (!this.testing) {
        	this.socket = new DuplexSocket(receivingPort, sendPortNum, sendAddress, this.logger, timeout);
        } else {
        	this.socket = null;
        }
        
        this.state = ElevatorState.SEND_ACK;
        this.currentFloor = 1;
        this.destinationFloor = 0;
        this.currentErrorType = ElevatorState.NO_ERROR;
        this.direction = ElevatorDirection.UP;

        if (this.testing) {
            this.SECONDS_TO_MILLI = 0;
        } else {
            this.SECONDS_TO_MILLI = 1000;
        }
    }

    /**
     * Sets the current state;
     */
    public void setState(ElevatorState state) { this.state = state; }

    /**
     * Returns the current state;
     * @return ElevatorState, the elevator state.
     */
    public ElevatorState getState() { return this.state; }

    /**
     * Sends an arrival ack to the scheduler.
     * Blocks for response.
     */
    private void sendAck() {
        JSONObject sendPacket;
        JSONObject dataPacket;
        DatagramPacket receivePacket;

        if (this.testing) {
            this.state = ElevatorState.DOOR_CLOSE;
            return;
        }

        this.logger.log("Sending ack....");
        final int currentFloor = this.currentFloor;
        final ElevatorDirection direction = this.direction;
        sendPacket = JSONPacket.createPacket(new HashMap<>(){{
            put(Config.K_TOPIC, Config.REQUEST_FLOOR_MSG);
            put(Config.K_FLOOR, currentFloor);
            put(Config.K_FLOOR_BUTTON, direction);
            put(Config.K_ERROR, ElevatorState.NO_ERROR);
        }});
        socket.send(JSONPacket.serialize(sendPacket));

        this.logger.log("Waiting for elevator request....");
        try {
            receivePacket = socket.receive();
        } catch (SocketTimeoutException e) {
            // e.printStackTrace();
            this.logger.log("Timed out.");
            this.state = ElevatorState.SEND_ACK;
            return;
        }

        dataPacket = JSONPacket.deserialize(receivePacket.getData(), receivePacket.getLength());
        this.logger.log(dataPacket.toString());

        this.destinationFloor = dataPacket.getInt(Config.K_DESTINATION_FLOOR);
        this.currentErrorType = ElevatorState.valueOf(dataPacket.getString(Config.K_ERROR));

        if (this.currentErrorType == ElevatorState.DOOR_JAM){
            this.state = ElevatorState.DOOR_JAM;
            return;
        }

        this.state = ElevatorState.DOOR_CLOSE;
    }

    /**
     * Closes the doors of this elevator.
     */
    private void doorClose() {
        this.logger.log("Closing Doors");

        try {
            Thread.sleep(Config.DOOR_ACTION_TIME * SECONDS_TO_MILLI);
        } catch (InterruptedException e) {
            this.logger.log("Timed out. Retrying.");
        }

        if (this.currentErrorType == ElevatorState.STUCK_FLOOR){
            this.state = ElevatorState.STUCK_FLOOR;
            return;
        }

        this.state = ElevatorState.MOVE;
    }

    /**
     * Moves this elevator from current to destination floor.
     */
    private void move() {
        final double dt = 0.1;
        double totalTimeout;
        double[] timeouts = new double[3];
        double[] accelerations = new double[]{Config.ACCELERATION, 0, -Config.ACCELERATION};
        double velocity = 0;
        double deltaDistance = 0;
        double timer = 0;

        this.logger.log("Moving to destination floor " + this.destinationFloor);

        if (this.destinationFloor > this.currentFloor) {
            this.direction = ElevatorDirection.UP;
        } else {
            this.direction = ElevatorDirection.DOWN;
        }

        long distanceToDestination = abs(this.destinationFloor - this.currentFloor) * Config.DISTANCE_BETWEEN_FLOOR;

        if(distanceToDestination > Config.ACCELERATION_DISTANCE * 2){
            // t = t_a * 2 + (Î”d - Î”d_a) / v_top
            totalTimeout = Math.round((Config.ACCELERATION_TIME * 2 + abs(distanceToDestination - Config.ACCELERATION_DISTANCE * 2) / Config.TOP_SPEED) * 10.0) / 10.0;
            timeouts[0] = Config.TOP_SPEED / Config.ACCELERATION;
            timeouts[1] = totalTimeout - 2 * timeouts[0];
            timeouts[2] = timeouts[0];
        }
        else{
            // d = Vit + 1/2 at^2
            // Accelerate half and deaccelerate other half
            // t = 2sqrt(0.5 * 2d/a)
            totalTimeout = 2 * (Math.sqrt((distanceToDestination) / Config.ACCELERATION));
            timeouts[0] = totalTimeout / 2;
            timeouts[1] = 0;
            timeouts[2] = timeouts[0];
        }

        for (int timeoutIdx = 0; timeoutIdx < timeouts.length; timeoutIdx++){
            while (timer <= timeouts[timeoutIdx]) {
                try {
                    Thread.sleep(new Double(dt * SECONDS_TO_MILLI).longValue());
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

                timer += dt;
                velocity += accelerations[timeoutIdx] * dt;
                deltaDistance += velocity * dt;

//                this.logger.log("Distance: " + String.format("%.2f", deltaDistance));
//                this.logger.log("Velocity: " + String.format("%.2f", velocity));
//                this.logger.log("Acceleration: " + String.format("%.2f", accelerations[timeoutIdx]));

                if (deltaDistance > Config.DISTANCE_BETWEEN_FLOOR) {
                    deltaDistance = deltaDistance % Config.DISTANCE_BETWEEN_FLOOR;
                    this.chrono.start();
                    this.sendFloorUpdate();
                    this.chrono.end();
                    this.chrono.logElapsed("ELEVATOR_FLOOR_UPDATE");
                }
            }
            timer = 0;
        }

        this.logger.log("\t>>>>>>>>> Arrived to destination floor " + this.destinationFloor);
        this.currentFloor = this.destinationFloor;
        this.state = ElevatorState.DOOR_OPEN;
    }

    /**
     * Sends floor update event to scheduler.
     */
    public void sendFloorUpdate() {
        JSONObject sendPacket;
        JSONObject dataPacket;
        DatagramPacket receivePacket;

        if (this.direction == ElevatorDirection.UP) {
            this.currentFloor ++;
        } else {
            this.currentFloor--;
        }

        if (this.testing) return;

        while (true) {
            this.logger.log("Sending floorUpdate: floor - " + this.currentFloor);
            final int currentFloor = this.currentFloor;
            final ElevatorDirection direction = this.direction;
            sendPacket = JSONPacket.createPacket(new HashMap<>(){{
                put(Config.K_TOPIC, Config.UPDATE_FLOOR_MSG);
                put(Config.K_FLOOR, currentFloor);
                put(Config.K_FLOOR_BUTTON, direction);
                put(Config.K_ERROR, ElevatorState.NO_ERROR);
            }});
            socket.send(JSONPacket.serialize(sendPacket));

            this.logger.log("Waiting for elevator ack....");

            try {
                receivePacket = socket.receive();
                dataPacket = JSONPacket.deserialize(receivePacket.getData(), receivePacket.getLength());
                this.logger.log(dataPacket.toString());
                return;
            } catch (SocketTimeoutException e) {
                // e.printStackTrace();
                this.logger.log("Timed out. Retrying.");
                continue;
            }
        }
    }

    /**
     * Sends door open event to scheduler.
     */
    private void doorOpen() {
        JSONObject sendPacket;
        JSONObject dataPacket;
        DatagramPacket receivePacket;

        this.logger.log("Opening Doors");
        try {
            Thread.sleep(Config.DOOR_ACTION_TIME * SECONDS_TO_MILLI);
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }

        if (this.testing) {
            this.state = ElevatorState.WAIT_BOARDING;
            return;
        }

        while (true) {
            this.logger.log("Sending open doors notification: floor - " + this.currentFloor);
            final int currentFloor = this.currentFloor;
            final ElevatorDirection direction = this.direction;
            sendPacket = JSONPacket.createPacket(new HashMap<>(){{
                put(Config.K_TOPIC, Config.OPENING_DOORS_MSG);
                put(Config.K_FLOOR, currentFloor);
                put(Config.K_FLOOR_BUTTON, direction);
                put(Config.K_ERROR, ElevatorState.NO_ERROR);
            }});
            socket.send(JSONPacket.serialize(sendPacket));

            this.logger.log("Waiting for elevator ack....");

            try {
                receivePacket = socket.receive();
                dataPacket = JSONPacket.deserialize(receivePacket.getData(), receivePacket.getLength());
                this.logger.log(dataPacket.toString());
                this.state = ElevatorState.WAIT_BOARDING;
                return;
            } catch (SocketTimeoutException e) {
                // e.printStackTrace();
                this.logger.log("Timed out. Retrying.");
                continue;
            }
        }
    }

    /**
     * Waits for passengers to board this elevator.
     */
    private void waitBoarding() {
        this.logger.log("Waiting for passengers to board");
        try {
            Thread.sleep(Config.BOARDING_TIME * SECONDS_TO_MILLI);
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }
        this.state = ElevatorState.SEND_ACK;
    }

    /**
     * Simulates jamming of elevator doors.
     */
    private void doorJam() {
        JSONObject sendPacket;
        DatagramPacket receivePacket;
        JSONObject json;

        if (this.testing) {
            this.state = ElevatorState.WAIT_COMMAND;
            return;
        }

        this.logger.log("Doors have jammed! Waiting for scheduler status check...");

        // Get status request
        while (true) {
            try {
                receivePacket = socket.receive();
                break;
            } catch (SocketTimeoutException e) {
                // e.printStackTrace();
                this.logger.log("Timed out.");
            }
        }
        json = JSONPacket.deserialize(receivePacket.getData(), receivePacket.getLength());

        this.logger.log("Got elevator status request: " + json +  ". " +  "Sending door jam notification.");
        final int currentFloor = this.currentFloor;
        sendPacket = JSONPacket.createPacket(new HashMap<>(){{
            put(Config.K_TOPIC, Config.ERROR_TYPE_MSG);
            put(Config.K_ERROR, ElevatorState.DOOR_JAM);
            put(Config.K_FLOOR, currentFloor);
        }});
        this.chrono.start();
        socket.send(JSONPacket.serialize(sendPacket));

        this.state = ElevatorState.WAIT_COMMAND;
    }

    /**
     * Simulates stuck between floors
     * elevator event.
     */
    private void stuckFloor() {
        JSONObject sendPacket;
        DatagramPacket receivePacket;
        JSONObject json;

        if (this.testing) {
            this.state = ElevatorState.WAIT_COMMAND;
            return;
        }

        this.logger.log("Elevator is stuck! Waiting for scheduler status check...");

        // Get status request
        while (true) {
            try {
                receivePacket = socket.receive();
                break;
            } catch (SocketTimeoutException e) {
                // e.printStackTrace();
                this.logger.log("Timed out.");
            }
        }
        json = JSONPacket.deserialize(receivePacket.getData(), receivePacket.getLength());

        this.logger.log("Got elevator status request: " + json +  ". " +  "Sending stuck floor notification.");
        final int currentFloor = this.currentFloor;
        sendPacket = JSONPacket.createPacket(new HashMap<>(){{
            put(Config.K_TOPIC, Config.ERROR_TYPE_MSG);
            put(Config.K_ERROR, ElevatorState.STUCK_FLOOR);
            put(Config.K_FLOOR, currentFloor);
        }});
        this.chrono.start();
        socket.send(JSONPacket.serialize(sendPacket));

        this.state = ElevatorState.WAIT_COMMAND;
    }

    /**
     * Elevator waits for command following door
     * jam event.
     */
    private void waitCommand(String testParam) {
        DatagramPacket receivePacket;
        JSONObject json;

        if (this.testing) {
            if (testParam.equals("unjam")) {
                this.state = ElevatorState.DOOR_CLOSE;
            }
            else if (testParam.equals("terminate")) {
                this.state = ElevatorState.TERMINATED;
            }
            return;
        }

        this.logger.log("Waiting for scheduler command.");

        // Get status request
        while (true) {
            try {
                receivePacket = socket.receive();
                this.chrono.end();
                this.chrono.logElapsed("ERROR_WAIT_COMMAND");
                break;
            } catch (SocketTimeoutException e) {
                // e.printStackTrace();
                this.logger.log("Timed out.");
            }
        }
        json = JSONPacket.deserialize(receivePacket.getData(), receivePacket.getLength());
        this.logger.log("Got elevator command: " + json);

        String command = json.getString(Config.K_COMMAND);

        switch (command) {
            case Config.UNLOCK_DOOR_COMMAND: {
                this.logger.log("Unjamming doors, standby...");
                try {
                    Thread.sleep(Config.DOOR_UNJAMMING_TIME * SECONDS_TO_MILLI);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                this.state = ElevatorState.DOOR_CLOSE;
                break;
            }

            case Config.TERMINATE_COMMAND: {
                this.logger.log("Received termination request. Terminating...");
                this.state = ElevatorState.TERMINATED;
                break;
            }

            default: {
                this.state = ElevatorState.WAIT_COMMAND;
                return;
            }
        }
    }

    /**
     * Executes a transition on this elevator's
     * state machine.
     */
    public void execute() {
        this.execute("");
    }

    /**
     * Executes a transition on this elevator's
     * state machine with testing injection.
     */
    public void execute(String testParam) {
        switch(this.state) {
            case SEND_ACK: {
                this.logger.log("State: SEND_ACK");
                this.chrono.start();
                this.sendAck();
                this.chrono.end();
                this.chrono.logElapsed("SEND_ACK");
                break;
            }

            case DOOR_CLOSE: {
                this.logger.log("State: DOOR_CLOSE");
                this.doorClose();
                break;
            }

            case MOVE: {
                this.logger.log("State: MOVING");
                this.move();
                break;
            }

            case DOOR_OPEN: {
                this.logger.log("State: DOOR_OPEN");
                this.chrono.start();
                this.doorOpen();
                this.chrono.end();
                this.chrono.logElapsed("DOOR_OPEN");
                break;
            }

            case WAIT_BOARDING: {
                this.logger.log("State: WAIT_BOARDING");
                this.waitBoarding();
                break;
            }

            case DOOR_JAM:{
                this.logger.log("State: DOOR_JAM");
                this.doorJam();
                break;
            }

            case WAIT_COMMAND:{
                this.logger.log("State: WAIT_COMMAND");
                this.waitCommand(testParam);
                break;
            }

            case STUCK_FLOOR:{
                this.logger.log("State: STUCK_FLOOR");
                this.stuckFloor();
                break;
            }
        }
    }

    /**
     * Runs this elevator's state machine loop.
     */
    public void run() {
        while(this.state != ElevatorState.TERMINATED) {
            this.execute();
        }

        this.logger.log("Terminated.");
        return;
    }
}

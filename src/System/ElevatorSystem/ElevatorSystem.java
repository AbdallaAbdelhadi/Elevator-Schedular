package System.ElevatorSystem;

import java.net.InetAddress;
import java.net.UnknownHostException;

import System.Config;
import System.Util.Utility;

/**
 * Initiates and manages elevators (threads).
 * @author Ibrahim Almalki
 */
public class ElevatorSystem {
    private int numElevators;

    /**
     * The local host
     */
    private final InetAddress sendAddress;

    /**
     * Base port for elevator sending ports.
     */
    private int sendBasePort;

    /**
     * Base port for elevator receiving ports.
     */
    private int receiveBasePort;

    /**
     * Increment from base port.
     */
    private int receiveBasePortIncrement;

    /**
     * Timeout for elevator receive from socket.
     */
    private int timeout;

    /**
     * Creates a new elevator system.
     * @param receiveBasePort int, base port for elevator receiving ports.
     * @param receiveBasePortIncrement int, increment from base port.
     * @param sendBasePort int, base port for elevator sending ports.
     * @param sendAddress InetAddress, the address to send to.
     * @param numElevators int, the number of elevators.
     */
    public ElevatorSystem(int receiveBasePort, int receiveBasePortIncrement, int sendBasePort, InetAddress sendAddress, int numElevators, int timeout){
        this.numElevators = numElevators;
        this.sendAddress = sendAddress;
        this.sendBasePort = sendBasePort;
        this.receiveBasePort = receiveBasePort;
        this.receiveBasePortIncrement = receiveBasePortIncrement;
        this.timeout = timeout;
    }

    /**
     * Initializes elevator thread.
     * @param receivingPort int, the port the elevator is listening to.
     * @param sendPortNum int, the port the elevator is sending on.
     * @param sendAddress InetAddress, the address the elevator is sending on.
     * @param elevatorID int, the elevator's id.
     */
    public void initElevator(int receivingPort, int sendPortNum, InetAddress sendAddress, int elevatorID){
        Runnable elevator = new Elevator(receivingPort, sendPortNum, sendAddress, elevatorID, false, this.timeout);

        Thread elevatorThread = new Thread(elevator);
        elevatorThread.start();
    }

    /**
     * Starts all elevator threads.
     */
    public void start() {
        int receivingPort;
        int sendingPort;

        for (int i = 0; i < this.numElevators; i++) {
            receivingPort = this.receiveBasePort + i * this.receiveBasePortIncrement;
            sendingPort = this.sendBasePort + 1 + i;
            initElevator(receivingPort, sendingPort, this.sendAddress, i);
        }
    }

    /**
     * Elevator system entrypoint.
     * @param args String[], command line args (unused).
     */
    public static void main(String[] args) {
        InetAddress HOST;

        try {
            HOST = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException("[ELEVATOR SYSTEM] Couldn't acquire the local host.");
        }

        ElevatorSystem elevator = new ElevatorSystem(
                Config.ELEVATOR_BASE_PORT,
                Config.ELEVATOR_INCREMENT,
                Config.SCHEDULER_PORT,
                HOST,
                Config.NUMBER_ELEVATORS,
                Config.ELEVATOR_TIMEOUT * Utility.SECONDS_TO_MILLISECONDS / 2
        );

        elevator.start();
    }
}

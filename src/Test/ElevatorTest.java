package Test;

import System.ElevatorSystem.Elevator;
import Types.ElevatorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests a singular elevator.
 * @author Abdalla Abdelhadi
 */
public class ElevatorTest {
    private static Elevator elevator;

    @BeforeEach
    public void init() {
        InetAddress host;
        try {
            host = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        elevator = new Elevator(
                400,
                401,
                host,
                0,
                true,
                0
        );
    }

    /**
     * Tests elevator state machine.
     */
    @Test
    public void testElevator() {
        // Initially request data
        assertTrue(elevator.getState() == ElevatorState.SEND_ACK);

        elevator.execute();

        // Data acquired, close doors.
        assertTrue(elevator.getState() == ElevatorState.DOOR_CLOSE);

        elevator.execute();

        // Go to destination.
        assertTrue(elevator.getState() == ElevatorState.MOVE);

        elevator.execute();

        // Open doors.
        assertTrue(elevator.getState() == ElevatorState.DOOR_OPEN);

        elevator.execute();

        // Wait for people to board.
        assertTrue(elevator.getState() == ElevatorState.WAIT_BOARDING);

        elevator.execute();

        // Send ack to get next command
        assertTrue(elevator.getState() == ElevatorState.SEND_ACK);
    }

    /**
     * Tests Elevator Handling of Door Jam Error
     */
    @Test
    public void testDoorJamError() {
        // Simulate the door jam error.
        elevator.setState(ElevatorState.DOOR_JAM);

        elevator.execute();

        // The elevator will be idle and should wait for a status request
        // from the scheduler.
        assertTrue(elevator.getState() == ElevatorState.WAIT_COMMAND);

        elevator.execute("unjam");

        // The status request will notify the elevator to unjam and close its doors.
        assertTrue(elevator.getState() == ElevatorState.DOOR_CLOSE);
    }

    /**
     * Tests Scheduler Handling of Stuck Between Floors Error
     */
    @Test
    public void testStuckFloor() {
        // Simulate the stuck between floors error.
        elevator.setState(ElevatorState.STUCK_FLOOR);

        elevator.execute();

        assertTrue(elevator.getState() == ElevatorState.WAIT_COMMAND);

        elevator.execute("terminate");

        // After receiving a reply from the scheduler, the elevator should terminate.
        assertTrue(elevator.getState() == ElevatorState.TERMINATED);
    }
}

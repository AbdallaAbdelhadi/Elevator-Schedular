package Test;

import System.Config;
import System.Scheduler.SchedulerController;
import Types.ElevatorDirection;
import Types.ElevatorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the scheduling algorithm and
 * logic of the scheduler controller.
 * @author Yousef Yassin, Abdalla Abdelhadi
 */
public class SchedulerControllerTest {
    public static SchedulerController schedulerController;

    /**
     * Initializes test harness state
     * before each unit test.
     */
    @BeforeEach
    public void init() {
        schedulerController = new SchedulerController(Config.MAX_FLOOR, Config.NUMBER_ELEVATORS);
    }

    /**
     * Tests that we can add and receive
     * a single request.
     */
    @Test
    public void testAddGetSingleRequest() {
        schedulerController.addRequest(4, 5, ElevatorState.DOOR_JAM);

        int id = 0;
        int nextFloor = schedulerController.getNextFloor(id, 1, ElevatorDirection.DOWN);
        ElevatorState error = schedulerController.getNextError(id, nextFloor);

        assertTrue(nextFloor == 4);
        assertTrue(error.equals(ElevatorState.DOOR_JAM));

        schedulerController.addRequest(20, 5, ElevatorState.STUCK_FLOOR);

        id = 1;
        nextFloor = schedulerController.getNextFloor(id, 5, ElevatorDirection.UP);
        error = schedulerController.getNextError(id, nextFloor);

        assertTrue(nextFloor == 20);
        assertTrue(error.equals(ElevatorState.STUCK_FLOOR));
    }

    /**
     * Tests that the closest request for an elevator
     * is the one dispatched.
     */
    @Test
    public void testGetProximityRequest() {
        schedulerController.addRequest(4, 5, ElevatorState.NO_ERROR);
        schedulerController.addRequest(8, 9, ElevatorState.DOOR_JAM);

        int id = 0;
        int nextFloor = schedulerController.getNextFloor(id, 1, ElevatorDirection.UP);
        ElevatorState error = schedulerController.getNextError(id, nextFloor);

        assertTrue(nextFloor == 4);
        assertTrue(error.equals(ElevatorState.NO_ERROR));
    }

    /**
     * Tests that elevator prioritizes requests in the 
     * direction it's headed over proximity (ensure no starvation).
     */
    @Test
    public void testDirectionRequest() {
        schedulerController.addRequest(4, 5, ElevatorState.NO_ERROR);
        schedulerController.addRequest(8,9, ElevatorState.DOOR_JAM);
        schedulerController.addRequest(1,9, ElevatorState.STUCK_FLOOR);


        int id = 0;
        int nextFloor = schedulerController.getNextFloor(id, 1, ElevatorDirection.DOWN);
        ElevatorState error = schedulerController.getNextError(id, nextFloor);

        assertTrue(nextFloor == 1);
        assertTrue(error.equals(ElevatorState.STUCK_FLOOR));
    }


    /**
     * Ensures an elevator can claim a request 
     * dispatch floor.
     */
    @Test
    public void testElevatorClaimRequest() {
        schedulerController.addRequest(4, 9, ElevatorState.NO_ERROR);
        schedulerController.addRequest(4, 15, ElevatorState.NO_ERROR);

        int id = 0;
        Integer nextFloor = schedulerController.getNextFloor(id, 1, ElevatorDirection.DOWN);
        ElevatorState error = schedulerController.getNextError(id, nextFloor);

        assertTrue(nextFloor == 4);
        assertTrue(error.equals(ElevatorState.NO_ERROR));
        assertTrue(schedulerController.getValid()[nextFloor - 1] == id);

        nextFloor = schedulerController.getNextFloor(id, 1, ElevatorDirection.DOWN);
        assertTrue(nextFloor == null);
    }

    /**
     * Tests that elevator acknowledgments
     * are processed correctly.
     */
    @Test
    public void testProcessAck() {
        schedulerController.addRequest(4, 9, ElevatorState.DOOR_JAM);

        int id = 0;

        Integer nextFloor = schedulerController.getNextFloor(id, 1, ElevatorDirection.DOWN);
        ElevatorState error = schedulerController.getNextError(id, nextFloor);

        assertTrue(nextFloor == 4);
        assertTrue(error.equals(ElevatorState.DOOR_JAM));
        assertTrue(schedulerController.getValid()[nextFloor - 1] == id);
        assertTrue(!schedulerController.getRequestQueue().get(nextFloor - 1).isEmpty());
        assertTrue(!schedulerController.getElevatorRequests()[id][nextFloor - 1]);

        schedulerController.ackFloor(id, nextFloor);
        assertTrue(schedulerController.getRequestQueue().get(nextFloor - 1).isEmpty());
        assertTrue(schedulerController.getElevatorRequests()[id][9 - 1]);
        assertTrue(schedulerController.getValid()[nextFloor - 1] == -1);

        nextFloor = schedulerController.getNextFloor(id, nextFloor, ElevatorDirection.UP);
        error = schedulerController.getNextError(id, nextFloor);
        assertTrue(nextFloor == 9);
        assertTrue(error.equals(ElevatorState.NO_ERROR));
    }

    /**
     * Tests that elevator (Stuck floor) errors
     * are processed correctly.
     */
    @Test
    public void testProcessError() {
        schedulerController.addRequest(4, 9, ElevatorState.STUCK_FLOOR);
        schedulerController.addRequest(4, 15, ElevatorState.DOOR_JAM);

        int id = 0;
        Integer nextFloor = schedulerController.getNextFloor(id, 1, ElevatorDirection.DOWN);
        ElevatorState error = schedulerController.getNextError(id, nextFloor);

        assertTrue(nextFloor == 4);
        assertTrue(error.equals(ElevatorState.STUCK_FLOOR));
        assertTrue(schedulerController.getValid()[nextFloor - 1] == id);

        nextFloor = schedulerController.getNextFloor(id, 1, ElevatorDirection.DOWN);
        assertTrue(nextFloor == null);

        schedulerController.processError(0);

        nextFloor = schedulerController.getNextFloor(id, 1, ElevatorDirection.DOWN);
        error = schedulerController.getNextError(id, nextFloor);
        assertTrue(nextFloor == 4);
        assertTrue(error.equals(ElevatorState.NO_ERROR));
    }

    /**
     * Tests that the correct sequence of 
     * expected scheduling order is dispatched.
     */
    @Test
    public void testSequenceRequest() {
        schedulerController.addRequest(4, 5, ElevatorState.NO_ERROR);
        schedulerController.addRequest(8,9, ElevatorState.NO_ERROR);
        schedulerController.addRequest(1,9, ElevatorState.NO_ERROR);

        int id = 0;
        Integer nextFloor = schedulerController.getNextFloor(id, 1, ElevatorDirection.DOWN);
        ElevatorState error = schedulerController.getNextError(id, nextFloor);

        Integer[] sequence = new Integer[]{4, 5, 8, 9, null};

        assertTrue(nextFloor == 1);
        assertTrue(error.equals(ElevatorState.NO_ERROR));

        for (Integer floor : sequence) {
            schedulerController.ackFloor(id, nextFloor);
            nextFloor = schedulerController.getNextFloor(id, nextFloor, ElevatorDirection.UP);
            if (nextFloor != null) error = schedulerController.getNextError(id, nextFloor);

            assertEquals(nextFloor, floor);
            assertTrue(error.equals(ElevatorState.NO_ERROR));
        }
    }
}

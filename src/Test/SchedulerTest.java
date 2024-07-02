package Test;

import GUI.SystemGUI;
import System.Config;
import System.Scheduler.Scheduler;
import System.Util.Logger;
import System.Util.Utility;
import Types.SchedulerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Tests the scheduler subsystem.
 * @author Abdalla Abdelhadi
 */
public class SchedulerTest {
    private static Scheduler scheduler;
    private static Logger elevatorLogger;
    private final static int ELEVATOR_ID = 1;

    /**
     * Initializes test harness state
     * before each unit test.
     */
    @BeforeEach
    public void init() {
        InetAddress host;

        try {
            host = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException("[SCHEDULER] Couldn't acquire the local host.");
        }

        elevatorLogger = new Logger("Scheduler-elev-port-" + ELEVATOR_ID);
        SystemGUI gui = new SystemGUI(Config.MAX_FLOOR, Config.NUMBER_ELEVATORS);
        scheduler = new Scheduler(
                "Scheduler",
                Config.MAX_FLOOR,
                Config.ELEVATOR_TIMEOUT * Utility.SECONDS_TO_MILLISECONDS,
                Config.FLOOR_PORT,
                Config.SCHEDULER_PORT,
                Config.NUMBER_ELEVATORS,
                host,
                gui,
                true
        );

        setElevatorState(SchedulerState.WAIT);
    }

    /**
     * Sets elevator id's state.
     * @param state SchedulerState, the state to set.
     */
    private void setElevatorState(SchedulerState state) {
        scheduler.getSchedulerElevatorState()[ELEVATOR_ID] = state;
    }

    /**
     * Gets elevator id's state.
     * @return SchedulerState, the state.
     */
    private SchedulerState getElevatorState() {
        return scheduler.getSchedulerElevatorState()[ELEVATOR_ID];
    }

    /**
     * Progresses the scheduler's state machine.
     * @return boolean, true if success, false otherwise.
     */
    private boolean execute() {
        return scheduler.elevatorHandler(ELEVATOR_ID, null, elevatorLogger);
    }

    /**
     * Progresses the scheduler's state machine with a test injection.
     * @param testParam String, the test injection.
     * @return boolean, true if success, false otherwise.
     */
    private boolean execute(String testParam) {
        return scheduler.elevatorHandler(ELEVATOR_ID, null, elevatorLogger, testParam);
    }

    /**
     * Tests the primary scheduler state machine.
     */
    @Test
    public void testScheduler() {
        assertTrue(getElevatorState() == SchedulerState.WAIT);

        this.execute();

        assertTrue(getElevatorState() == SchedulerState.RECEIVE);

        this.execute();

        assertTrue(getElevatorState() == SchedulerState.RECEIVE);
    }

    /**
     * Tests scheduler handling of a 
     * door jam elevator error.
     */
    @Test
    public void testDoorJamError() {
        // Simulate a timeout
        this.setElevatorState(SchedulerState.SEND_STATUS_REQ);

        assertTrue(getElevatorState() == SchedulerState.SEND_STATUS_REQ);

        this.execute();

        assertTrue(getElevatorState() == SchedulerState.LISTEN_FOR_STATUS_REPLY);

        this.execute("doorJam");

        assertTrue(getElevatorState() == SchedulerState.SEND_UNLOCK_DOORS);

        this.execute();

        assertTrue(getElevatorState() == SchedulerState.RECEIVE);

        this.execute();

        assertTrue(getElevatorState() == SchedulerState.RECEIVE);
    }

    /**
     * Tests scheduler handling of a stuck
     * floor error.
     */
    @Test
    public void testStuckFloorError() {
        // Simulate a timeout
        this.setElevatorState(SchedulerState.SEND_STATUS_REQ);

        this.execute();

        assertTrue(getElevatorState() == SchedulerState.LISTEN_FOR_STATUS_REPLY);

        this.execute("stuckFloor");

        assertTrue(getElevatorState() == SchedulerState.SEND_TERMINATE);

        this.execute();

        assertTrue(getElevatorState() == SchedulerState.TERMINATED);
    }
}

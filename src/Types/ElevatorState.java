package Types;

/**
 * Enum for elevator state in state machine.
 */
public enum ElevatorState {
    SEND_ACK, MOVE, DOOR_OPEN,
    WAIT_BOARDING, DOOR_CLOSE, NO_ERROR,
    DOOR_JAM, WAIT_COMMAND, STUCK_FLOOR,
    TERMINATED
}
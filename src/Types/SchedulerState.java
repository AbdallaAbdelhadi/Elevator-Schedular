package Types;

/**
 * Enum for elevator state machine communication with elevator.
 */
public enum SchedulerState {
    WAIT, RECEIVE, SEND_STATUS_REQ,
    SEND_UNLOCK_DOORS, SEND_TERMINATE,
    TERMINATED, LISTEN_FOR_STATUS_REPLY
}

package System.Scheduler;

import Types.ElevatorState;

/**
 * A scheduler stored/queued request.
 * @author Yousef Yassin
 */
public class Request {
    private int pickupFloor;
    private int destFloor;
    private ElevatorState error;

    /**
     * Creates a new request with the specified parameters.
     * @param pickupFloor int, the floor to pick up from.
     * @param destFloor int, the floor to drop off at.
     * @param error ElevatorState, the associated error with the request.
     */
    public Request (int pickupFloor, int destFloor, ElevatorState error) {
        this.pickupFloor = pickupFloor;
        this.destFloor = destFloor;
        this.error = error;
    }

    /**
     * Returns the pickup floor.
     * @return int, the pickup floor.
     */
    public int getPickupFloor() {
        return pickupFloor;
    }

    /**
     * Sets the pickup floor to the specified floor.
     * @param pickupFloor int, the floor to set.
     */
    public void setPickupFloor(int pickupFloor) {
        this.pickupFloor = pickupFloor;
    }

    /**
     * Returns the destination floor.
     * @return int, the destination floor.
     */
    public int getDestFloor() {
        return destFloor;
    }

    /**
     * Sets the destination floor.
     * @param destFloor int, the destination floor to set.
     */
    public void setDestFloor(int destFloor) {
        this.destFloor = destFloor;
    }

    /**
     * Returns the error associated with the request.
     * @return Elevatorstate, the error.
     */
    public ElevatorState getError() {
        return error;
    }

    /**
     * Sets this request's error.
     * @param error ElevatorState, the error to set.
     */
    public void setError(ElevatorState error) {
        this.error = error;
    }
}

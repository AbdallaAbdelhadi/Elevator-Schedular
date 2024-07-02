package System.Scheduler;

import Types.ElevatorDirection;
import Types.ElevatorState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Implements scheduling algorithm and
 * controls scheduler sent requests.
 * @author Yousef Yassin
 */
public class SchedulerController {
    private ArrayList<HashSet<Request>> requestQueue;
    private boolean[][] elevatorRequests;
    private int[] valid;

    /**
     * Creates a new scheduler controller with the specified parameters.
     * @param numFloors int, the number of floors in system.
     * @param numElevators int, the number of elevators in system.
     */
    public SchedulerController(int numFloors, int numElevators) {
        this.requestQueue = new ArrayList<>();
        this.elevatorRequests = new boolean[numElevators][numFloors];
        this.valid = new int[numFloors];
        initRequestQueue(numFloors);
        initValid();
    }

    /**
     * Returns this scheduler's unclaimed floors.
     * @return int[], the unclaimed floors bitmap.
     */
    public int[] getValid() {
        return this.valid;
    }

    /**
     * Return's this controller's requests.
     * @return boolean[][], the requests.
     */
    public boolean[][] getElevatorRequests() {
        return this.elevatorRequests;
    }

    /**
     * Return's this elevator's request queue.
     * @return ArrayList<HashSet<Request>>, the request queue.
     */
    public ArrayList<HashSet<Request>> getRequestQueue() {
        return this.requestQueue;
    }

    /**
     * Initializes this controller's request queue.
     * @param numFloors int, number of floors in system.
     */
    private void initRequestQueue(int numFloors) {
        for (int i = 0; i < numFloors; i++) {
            requestQueue.add(new HashSet<>());
        }
    }

    /**
     * Initializes floor valid bitmap.
     */
    private void initValid() {
        for (int i = 0; i < this.valid.length; i++) {
            this.valid[i] = -1;
        }
    }

    /**
     * Adds a request to this controller.
     * @param pickupFloor int, the floor to pickup elevator form.
     * @param destFloor int, the floor to drop off elevator.
     * @param error ElevatorState, the associated error with request.
     */
    public synchronized void addRequest(int pickupFloor, int destFloor, ElevatorState error) {
        Request newRequest = new Request(pickupFloor, destFloor, error);
        this.requestQueue.get(pickupFloor - 1).add(newRequest);
    }

    /**
     * Processes an error for elevator.
     * @param elevatorID int, the elevator with the error.
     */
    public synchronized void processError(int elevatorID) {
        for (int i = 0; i < this.valid.length; i++) {
            if (this.valid[i] == elevatorID) {
                this.valid[i] = -1;
                HashSet<Request> requests = this.requestQueue.get(i);

                Iterator<Request> iter = requests.iterator();
                while(iter.hasNext()) {
                    iter.next().setError(ElevatorState.NO_ERROR);
                }
            }
        }
    }

    /**
     * Acks a floor for the specified elevator.
     * @param elevatorID int, the elevator to ack for.
     * @param floor int, the floor to ack for.
     */
    public synchronized void ackFloor(int elevatorID, int floor) {
        this.elevatorRequests[elevatorID][floor - 1] = false;

        // 1 - Pickup all request on the floor you arrive to
        HashSet<Request> requests = this.requestQueue.get(floor - 1);

        for (Request request : requests) {
            this.elevatorRequests[elevatorID][request.getDestFloor() - 1] = true;
        }

        this.requestQueue.get(floor - 1).clear();
        if (this.valid[floor - 1] == elevatorID) {
            this.valid[floor - 1] = -1;
        }
    }

    /**
     * Returns the next scheduled error for the specified elevator and request floor.
     * @param elevatorID int, the elevator to fetch error for.
     * @param nextFloor int, the next request for the elevator.
     * @return ElevatorState, the error.
     */
    public synchronized  ElevatorState getNextError(int elevatorID, int nextFloor) {
        ElevatorState error = ElevatorState.NO_ERROR;

        if (this.valid[nextFloor - 1] == elevatorID) {
            HashSet<Request> requests = this.requestQueue.get(nextFloor - 1);
            for (Request request : requests) {
                switch (request.getError()) {
                    case DOOR_JAM: {
                        error = ElevatorState.DOOR_JAM;
                        break;
                    }

                    case STUCK_FLOOR: {
                        error = ElevatorState.STUCK_FLOOR;
                        return error;
                    }

                    default:  { break; }
                }
            }
        }

        return error;
    }

    /**
     * Fetches the next scheduled floor for the specified elevator.
     * @param elevatorID int, the elevator to get floor for.
     * @param currentFloor int, the current floor of elevator.
     * @param direction ElevatorDirection, the direction the elevator is travelling in.
     * @return Integer, the next floor or null if no new requests.
     */
    public synchronized Integer getNextFloor(int elevatorID, int currentFloor, ElevatorDirection direction) {
        // 2 - Get the closest next floor in the direction you are going.

        Integer nextElevatorDestinationFloor = this.getNextElevatorDestinationFloor(elevatorID, currentFloor, direction);
        Integer nextRequestFloor = this.getNextRequestFloor(elevatorID, currentFloor, direction);

        if (nextRequestFloor == null && nextElevatorDestinationFloor == null) {
            direction = this.getOppositeDirection(direction);
            nextElevatorDestinationFloor = this.getNextElevatorDestinationFloor(elevatorID, currentFloor, direction);
            nextRequestFloor = this.getNextRequestFloor(elevatorID, currentFloor, direction);

            if (nextRequestFloor == null && nextElevatorDestinationFloor == null) { return null; }
        }

        if (nextElevatorDestinationFloor == null) {
            this.valid[nextRequestFloor - 1] = elevatorID;
            return nextRequestFloor;
        }
        if (nextRequestFloor == null) { return nextElevatorDestinationFloor; }

        if (nextRequestFloor < nextElevatorDestinationFloor) {
            this.valid[nextRequestFloor - 1] = elevatorID;
            return nextRequestFloor;
        } else {
            return nextElevatorDestinationFloor;
        }
    }

    /**
     * Fetches the closest destination floor for an elevator.
     * @param elevatorID int, the elevator to get floor for.
     * @param currentFloor int, the current floor of elevator.
     * @param direction ElevatorDirection, the direction the elevator is travelling in.
     * @return Integer, the closest destination floor or null if no are left.
     */
    private Integer getNextElevatorDestinationFloor(int elevatorID, int currentFloor, ElevatorDirection direction) {
        // Optimization (add counter)
        int increment;
        boolean[] elevatorDestinations = this.elevatorRequests[elevatorID];

        if (direction == ElevatorDirection.UP) {
            increment = 1;
        } else {
            increment = -1;
        }

        for (int i = currentFloor - 1; i < elevatorDestinations.length && i >= 0; i+=increment) {
            if (elevatorDestinations[i]) {
                return (i + 1);
            }
        }
        return null;
    }

    /**
     * Fetches the closest request floor for an elevator.
     * @param elevatorID int, the elevator to get floor for.
     * @param currentFloor int, the current floor of elevator.
     * @param direction ElevatorDirection, the direction the elevator is travelling in.
     * @return Integer, the next floor or null if no new requests.
     */
    private Integer getNextRequestFloor(int elevatorID, int currentFloor, ElevatorDirection direction) {
        // Optimization (add counter)
        int increment;
        ElevatorState error;

        if (direction == ElevatorDirection.UP) {
            increment = 1;
        } else {
            increment = -1;
        }

        for (int i = currentFloor - 1; i < this.requestQueue.size() && i >= 0; i+=increment) {
            if (!this.requestQueue.get(i).isEmpty() && this.valid[i] == -1) {
                return (i + 1);
            }
        }

        return null;
    }

    /**
     * Returns the opposite to the direction provided.
     * @param direction ElevatorDirection, direction to invert.
     * @return ElevatorDirection, the opposite direction.
     */
    private ElevatorDirection getOppositeDirection(ElevatorDirection direction) {
        if (direction == ElevatorDirection.UP) {
            return ElevatorDirection.DOWN;
        } else {
            return  ElevatorDirection.UP;
        }
    }
}

# ElevatorScheduler

## Iteration 4: Adding Error detection and correction.

### Authors:
- Abdalla Abdelhadi
- Ibrahim Almalki
- Yousef Yassin
- Zakariyya Almalki


### Breakdown of Responsibilities
- **Abdalla Abdelhadi**: Tests Classes, DuplexSocket, ElevatorPacket/Status, Scheduler Logic (Scheduler, SchedulerController), UML Class diagram
- **Ibrahim Almalki**: Tests Classes,  FloorPacket, ElevatorSystem, Sequence diagram, UML Class diagram, State machine diagram
- **Yousef Yassin**: Tests Classes, DuplexSocket, FloorPacket, Scheduler Logic (Scheduler, SchedulerController), ElevatorSystem, Sequence diagram
- **Zakariyya Almalki**: Tests Classes, ElevatorPacket/Status, UML Class diagram, Sequence diagram, State Machine diagram

### Explanation of File Names
- **InputReader**: Gets and imports data from textfile.
- **Floor**: Retrieves information from InputerReader, then puts information into data packet which is then sent to Elevator.
- **FloorPacket**: Creates a JSON serializable packet to be sent to the scheduler.
- **Elevator**: Receives data from the Floor class and displays it. Then it sends the data back to floor.
- **ElevatorStatus**: An container to encapsulate state context information
- **ElevatorPacket**: Creates a JSON serializable packet to be sent to the scheduler.
- **ElevatorSystem**: Initializes and starts threads for elevators.
- **Request**: Implements an elevator floor request
- **RequestQueue**: Stores the data used by scheduler for the order of floors the elevator visits.
- **Scheduler**: Ensures that the floor and elevator are receiving and sending data.
- **SchedulerController**: Contains scheduler logic for elevators.
- **Main**: Initializes and runs program.
- **ElevatorPacketTest**: JUnit5 test for ElevatorPacket class.
- **ElevatorTest**: JUnit5 test for Elevator class.
- **FloorPacketTest**: JUnit5 test for FloorPacket class.
- **FloorTest**: JUnit5 test for Floor class.
- **RequestQueueTest**: JUnit5 test for RequestQueue class.
- **SchedulerControllerTest**: JUnit5 test for SchedulerController class.
- **Logger**: Implements a logger utility class to log events with source name and associated time.
- **SchedulerTest**: JUnit5 test for Scheduler class.
- **SYSC3303_ElevatorScheduler_M4_UML**: UML class diagram of the system.
- **SYSC3303_ElevatorScheduler_M4_Sequence**: UML sequence diagram of the system.
- **SYSC3303_ElevatorScheduler_M4_StateMachines**: UML state machine diagram of the scheduler and elevator subsystems.
- **SYSC3303_ElevatorScheduler_M4_Timing**: Timing diagram of the error scenarios.


### Set-up Instructions
- Download project and run the entry points of the core processes in the following order:
	- Window > Preferences > General > Workspace > Text File Encoding > UTF-8
    - Run the main thread in the Scheduler class.
    - Then run the main thread in the floor class to begin sending commands.
    - Finally, run the main thread in the elevator class to execute commands.
- Print statements will be displayed on console describing the action that took place.

### Test Instructions
**Test Files Used**: SystemTest, LambdaInterface <br>
Test cases are standard JUnit 5 Tests. Right click on the any test file in the Test/ package then Run As -> Junit Test to test all the classes.

### UML Diagram
This can also be found as a png file in the project zip file. <br>
![UML Class Diagram](SYSC3303_ElevatorScheduler_M4_UML.png)

### Sequence Diagram
This can also be found as a png file in the project zip file. <br>
![Sequence Diagram](SYSC3303_ElevatorScheduler_M4_Sequence.png)

### State Machine Diagram of Elevator and Scheduler Subsystems
This can also be found as a png file in the project zip file. <br>
![State Machine Diagram](SYSC3303_ElevatorScheduler_M4_StateMachines.png)

### Timing Diagram
This can also be found as a png file in the project zip file. <br>
![Timing Diagram](SYSC3303_ElevatorScheduler_M4_Timing.png)

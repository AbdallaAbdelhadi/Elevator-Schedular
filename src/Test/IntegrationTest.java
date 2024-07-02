package Test;

import GUI.SystemGUI;
import System.Config;
import System.ElevatorSystem.ElevatorSystem;
import System.Floor.Floor;
import System.Scheduler.Scheduler;

import System.Util.Utility;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests integration of all system subsystem nodes.
 * @author Yousef Yassin
 */
public class IntegrationTest {
    public static Thread floorThread, schedulerThread, elevatorSystemThread;

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
            return;
        }

        floorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                    Floor floor = new Floor(
                            "Floor", "\\Test\\testData.txt",
                            Config.FLOOR_PORT, Config.SCHEDULER_PORT,
                            host, 10000
                    );
                    floor.run();
            }
        });

        schedulerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                SystemGUI gui = new SystemGUI(Config.MAX_FLOOR, Config.NUMBER_ELEVATORS);
                Scheduler scheduler = new Scheduler(
                        "Scheduler",
                        Config.MAX_FLOOR,
                        Config.ELEVATOR_TIMEOUT * Utility.SECONDS_TO_MILLISECONDS,
                        Config.FLOOR_PORT,
                        Config.SCHEDULER_PORT,
                        Config.NUMBER_ELEVATORS,
                        host,
                        gui,
                        false
                );
                scheduler.display();
                scheduler.start();
            }
        });

        elevatorSystemThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ElevatorSystem elevator = new ElevatorSystem(
                        Config.ELEVATOR_BASE_PORT,
                        Config.ELEVATOR_INCREMENT,
                        Config.SCHEDULER_PORT,
                        host,
                        Config.NUMBER_ELEVATORS,
                        Config.ELEVATOR_TIMEOUT * Utility.SECONDS_TO_MILLISECONDS / 2
                );

                elevator.start();
            }
        });
    }

    /**
     * Start each system and ensure proper
     * end-to-end communication and handling.
     */
    @Test
    public void testIntegration() {
        try {
            schedulerThread.start();
            Thread.sleep(1000);
            floorThread.start();
            Thread.sleep(4000);
            elevatorSystemThread.start();
            Thread.sleep(80000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        schedulerThread.stop();
        floorThread.stop();
        elevatorSystemThread.stop();
    }

}

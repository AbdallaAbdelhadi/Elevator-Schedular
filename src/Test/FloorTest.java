package Test;

import System.Floor.Floor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the floor subsystem.
 * @author Zakariyya Almalki
 */
public class FloorTest {
    private static Floor floor;

    /**
     * Initializes test harness state
     * before each unit test.
     */
    @BeforeEach
    public void init() throws UnknownHostException {
        floor = new Floor(
                "Floor",
            "\\Test\\testData.txt",
                900,
                200,
                InetAddress.getLocalHost(),
                10000
        );
    }

    /**
     * Tests input reader file reading.
     *
     * @throws IOException the io exception
     */
    @Test
    public void testGetData() throws IOException {
        ArrayList<ArrayList<String>> actual = new ArrayList<ArrayList<String>>();
        ArrayList<ArrayList<String>> expected = new ArrayList<ArrayList<String>>(
                Arrays.asList(
                        new ArrayList<>(Arrays.asList("Time", "Floor", "Floor Button", "Car Button", "Error")),
                        new ArrayList<>(Arrays.asList("14:05:15.0", "2", "Up", "4", "no_error")),
                        new ArrayList<>(Arrays.asList("14:05:15.0", "4", "Down", "1", "no_error")),
                        new ArrayList<>(Arrays.asList("14:05:15.0", "3", "Up", "4", "door_jam")),
                        new ArrayList<>(Arrays.asList("14:10:15.0", "5", "Down", "3", "stuck_floor"))
                )
        );

        floor.loadData();
        actual = floor.getInputData();
        assertTrue(actual.equals(expected));
    }
}

package Test;

import System.Util.JSONPacket;
import Types.ElevatorDirection;
import Types.ElevatorState;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import System.Config;
import java.util.HashMap;

/**
 * Tests JSONPacket utility.
 * @author Yousef Yassin
 */
public class JSONPacketTest {
    private static JSONObject json;
    private static HashMap<String, Object> items = new HashMap<>(){{
        put(Config.K_FLOOR, 1);
        put(Config.K_DESTINATION_FLOOR, 2);
        put(Config.K_TIME, "14:05:15.0");
        put(Config.K_FLOOR_BUTTON, ElevatorDirection.UP);
        put(Config.K_ACK, true);
        put(Config.K_ERROR, ElevatorState.DOOR_JAM);
    }};

    /**
     * Initializes test harness state
     * before each unit test.
     */
    @BeforeEach
    public void init() {
        json = JSONPacket.createPacket(items);
    }

    /**
     * Tests creation of a packet.
     */
    @Test
    public void testCreatePacket() {
        ElevatorDirection ed;
        ElevatorState es;

        assertTrue(json.getInt(Config.K_FLOOR) == (int) items.get(Config.K_FLOOR));
        assertTrue(json.getInt(Config.K_DESTINATION_FLOOR) == (int) items.get(Config.K_DESTINATION_FLOOR));
        assertTrue(json.getString(Config.K_TIME).equals(items.get(Config.K_TIME)));
        assertTrue(json.getBoolean(Config.K_ACK) == (boolean) items.get(Config.K_ACK));

        es = (ElevatorState) json.get(Config.K_ERROR);
        ed = (ElevatorDirection) json.get(Config.K_FLOOR_BUTTON);
        assertTrue(es.equals(items.get(Config.K_ERROR)));
        assertTrue(ed.equals(items.get(Config.K_FLOOR_BUTTON)));
    }

    /**
     * Tests proper serialization and 
     * deserialization of a packet.
     */
    @Test
    public void testSerialization() {
        ElevatorDirection ed;
        ElevatorState es;

        byte[] serialized = JSONPacket.serialize(json);
        JSONObject deserialized = JSONPacket.deserialize(serialized, serialized.length);

        assertTrue(deserialized.getInt(Config.K_FLOOR) == (int) items.get(Config.K_FLOOR));
        assertTrue(deserialized.getInt(Config.K_DESTINATION_FLOOR) == (int) items.get(Config.K_DESTINATION_FLOOR));
        assertTrue(deserialized.getString(Config.K_TIME).equals(items.get(Config.K_TIME)));
        assertTrue(deserialized.getBoolean(Config.K_ACK) == (boolean) items.get(Config.K_ACK));

        es = ElevatorState.valueOf(deserialized.getString(Config.K_ERROR));
        ed = ElevatorDirection.valueOf(deserialized.getString(Config.K_FLOOR_BUTTON));
        assertTrue(es.equals(items.get(Config.K_ERROR)));
        assertTrue(ed.equals(items.get(Config.K_FLOOR_BUTTON)));
    }
}

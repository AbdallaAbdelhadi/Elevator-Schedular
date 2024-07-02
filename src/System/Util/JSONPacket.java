package System.Util;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a packet that can be easily send using UDP.
 * @author Zakariyya Almalki, Yousef Yassin
 */
public class JSONPacket {
	/**
     * Create json packet.
     *
     * @param map, A hasmap of all the elemnts that are required to be sent
     * @return the json object, the jason packet representation of the hashmap
     */
    public static JSONObject createPacket(HashMap<String, Object> map) {
        JSONObject json = new JSONObject();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            json.put(key, value);
        }

        return json;
    }
    
    /**
     * Converts a Jason packet into an array of bytes for it be sent using UDP
     *
     * @param json, the json packet
     * @return the byte [ ], the array byte representation of the jason packet
     */
    public static byte[] serialize(JSONObject json) {
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Converts a byte array back to a jason packet for interperatation
     *
     * @param serialized, the byte array that need to be deserialized
     * @param length,     the length of the byte array
     * @return the json object
     */
    public static JSONObject deserialize(byte[] serialized, int length) {
        String serializedString = new String(serialized, 0, length);
        return new JSONObject(serializedString);
    }
}

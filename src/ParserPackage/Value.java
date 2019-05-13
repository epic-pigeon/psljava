package ParserPackage;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Value //extends JSONToString
    implements Serializable
{
    public boolean isSettable() {
        return false;
    }
    protected Object value;
    protected HashMap<String, Value> properties = new HashMap<>();

    public Value(Object value) {
        this.value = value;
    }

    public static final Value NULL = new Value(null);

    public Value(Object value, HashMap<String, Value> properties) {
        this.value = value;
        this.properties = properties;
    }

    public HashMap<String, Value> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<String, Value> properties) {
        this.properties = properties;
    }

    public Value get(String key) throws Exception {
        return properties.get(key);
    }

    public Value put(String key, Value value) throws Exception {
        return properties.put(key, value);
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String stringifyProps() {
        //return properties.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\n");
        for (Map.Entry<String, Value> entry : properties.entrySet()) {
            stringBuilder.append(String.join("", Collections.nCopies(4, " "))).append(entry.getKey()).append(": ");
            int offset = 4 + entry.getKey().length() + 2;
            String val = entry.getValue().toString();
            Collection<String> collection = new Collection<>(val.split("\\n"));
            val = collection.get(0) + "\n" + new Collection<>(collection.subList(1, collection.size()).toArray())
                    .map(s -> String.join("", Collections.nCopies(offset, " ")) + s).join("\n");
            stringBuilder.append(val);//.append("\n");
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public String toString() {
        if (value == null) {
            if (properties.isEmpty()) {
                return "null";
            } else {
                return stringifyProps();
            }
        } else {
            if (properties.isEmpty()) {
                return value.toString();
            } else {
                return value.toString() + " " + stringifyProps();
            }
        }
    }

    public void putAll(Map<? extends String, ? extends Value> m) {
        properties.putAll(m);
    }
}

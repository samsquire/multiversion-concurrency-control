package main;

import java.util.HashMap;
import java.util.Map;

public class LValue {
    private final Object data;
    private final String key;
    public String type;
    public LValue(String type, Object data, String key) {
        this.type = type;
        this.data = data;
        this.key = key;
    }

    public void add(Integer remove) {
        if (data instanceof HashMap) {
            Map<String, Object> data2 = (HashMap<String, Object>)data;
            data2.put(key, ((Integer) data2.get(key)) + remove);
            System.out.println("ADDING NUMBER");
        } else {
            System.out.println("Some other type");
        }
    }
}

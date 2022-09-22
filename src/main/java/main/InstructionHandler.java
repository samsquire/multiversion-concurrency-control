package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface InstructionHandler {
    public Map<String, String> run(HashMap<String, Integer> variables, List<String> args);
}

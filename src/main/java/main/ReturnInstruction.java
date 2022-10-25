package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReturnInstruction implements InstructionHandler {
    @Override
    public Map<String, String> run(HashMap<String, Integer> variables, List<String> args) {
        HashMap<String, String> parsed = new HashMap<>();
        return parsed;
    }
}

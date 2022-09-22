package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReceiveInstruction implements InstructionHandler {
    @Override
    public Map<String, String> run(HashMap<String, Integer> variables, List<String> args) {
        Map<String, String> parsed = new HashMap<>();
        parsed.put("variableName", args.get(1));
        parsed.put("failJump", args.get(2).substring(1, args.get(2).length()));
        return parsed;
    }
}

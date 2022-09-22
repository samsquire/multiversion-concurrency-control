package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndWhileInstruction implements InstructionHandler {
    @Override
    public Map<String, String> run(HashMap<String, Integer> variables, List<String> args) {
        Map<String, String> parsed = new HashMap<>();
        parsed.put("jump", args.get(1).substring(1, args.get(1).length()));
        return parsed;
    }
}

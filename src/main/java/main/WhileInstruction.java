package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhileInstruction implements InstructionHandler {
    @Override
    public Map<String, String> run(HashMap<String, Integer> variables, List<String> args) {
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("variableName", args.get(1));
        parsed.put("jump", args.get(2).substring(1, args.get(2).length()));
        return parsed;
    }
}

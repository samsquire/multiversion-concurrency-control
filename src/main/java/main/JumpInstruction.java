package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JumpInstruction implements InstructionHandler {
    @Override
    public Map<String, String> run(HashMap<String, Integer> variables, List<String> args) {
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("jumpDestination", args.get(1).substring(1, args.get(1).length()));
        return parsed;
    }
}

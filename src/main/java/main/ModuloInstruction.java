package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuloInstruction implements InstructionHandler {
    @Override
    public Map<String, String> run(HashMap<String, Integer> variables, List<String> args) {
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("variableName", args.get(1));
        parsed.put("amount", args.get(2));
        return parsed;
    }
}

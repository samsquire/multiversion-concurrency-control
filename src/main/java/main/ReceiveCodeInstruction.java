package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReceiveCodeInstruction implements InstructionHandler {
    @Override
    public Map<String, String> run(HashMap<String, Integer> variables, List<String> args) {
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("mailboxName", args.get(1));
        parsed.put("returnLabel", args.get(2));
        return parsed;
    }
}

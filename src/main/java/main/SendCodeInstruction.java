package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendCodeInstruction implements InstructionHandler {
    @Override
    public Map<String, String> run(HashMap<String, Integer> variables, List<String> args) {
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("mailboxName", args.get(1));
        parsed.put("destination", args.get(2));
        parsed.put("sendLabel", args.get(3).substring(1, args.get(3).length()));
        return parsed;
    }
}

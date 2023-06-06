package main;

import java.util.List;
import java.util.Map;

public interface API {
    public void fire(String variable, Map<String, String> values);

    MultiplexedAST getAst();

    Map<String, String> createValueMap(List<MultiplexingProgramParser.Fact> values);

    void wait(String send);

    void submit(String identifier, String fact, String value);

}

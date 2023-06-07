package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiplexingProgramParser {

    private final Pattern pattern;
    private Matcher matcher;
    private String programString;
    private int pos = 0;
    private boolean end = false;
    private char last_char = Character.MIN_VALUE;
    private String type;
    private String lastToken;
    private int precedence;
    private int lastPrecedence;
    private int SQUARE_BRACKET = 0;
    private int BRACKETS = 1;
    private int CURLY_BRACKETS = 2;
    private int[] depth;

    public Map<String, String> tokenHandling;
    private String lastType;
    private boolean depthChange;
    private int depthWhich;
    private int depthAmount;
    private boolean closeToken;
    private boolean previousDepthChange;

    public MultiplexingProgramParser(String programString) {
        pattern = Pattern.compile("[a-zA-Z0-9_]+", Pattern.CASE_INSENSITIVE);
        this.programString = programString;
        this.pos = 0;
    }

    public char getChar(int pos, int amount) {
        if (pos + amount >= this.programString.length()) {
            this.end = true;
            return this.programString.charAt(pos);
        } else {
            this.pos = pos + amount;
            return this.programString.charAt(pos);
        }
    }
    public String getToken(boolean peek, int peekAmount) {
        String token = getTokenReal(peek, peekAmount);
        System.out.println(String.format("TOKEN GET: %s (%b) %s", token, peek, type));

        return token;
    }

    public String getTokenReal(boolean peek, int peekAmount) {
        int remembered = this.pos;

        char remembered_char = this.last_char;

        String rememberedType = this.type;
        String rememberedlastToken = this.lastToken;
        this.lastPrecedence = this.precedence;
        this.type = "token";
        this.lastType = "token";
        while (this.end == false && (this.last_char == ' ' || this.last_char == '\n' || this.last_char == Character.MIN_VALUE)) {
            this.last_char = getChar(this.pos, 1);
            this.type = "space";
            lastToken = "space";
        }
        if (this.type == "space") {
            // return "space";
        }

        if (this.last_char == '(') {
            this.last_char = getChar(this.pos, 1);
            this.type = "openbracket";
            lastToken = "openbracket";
            this.depthChange = true;
            this.depthWhich = BRACKETS;
            this.depthAmount = 1;
            this.precedence = 100;
            String tokenToReturn = lastToken;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return tokenToReturn;
        }

        if (this.last_char == ')') {
            this.last_char = this.getChar(this.pos, peekAmount);
            lastToken = "closebracket";
            this.precedence = 100;
            this.closeToken = true;
            this.depthChange = true;
            this.depthWhich = BRACKETS;
            this.depthAmount = -1;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }

        if (this.last_char == '*') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.type = "operator";
            this.lastType = "operator";
            lastToken = "wildcard";
            // BODMAS
            this.precedence = 4;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }

        String identifier = "";
        if (this.last_char == '\'') {
            this.last_char = this.getChar(this.pos, peekAmount);
            identifier = "";
            while (!this.end && this.last_char != '\'') {
                if (this.last_char == '\\') {
                    this.last_char = this.getChar(this.pos, peekAmount);
                }
                identifier = identifier + this.last_char;

                this.last_char = this.getChar(this.pos, peekAmount);

                if (this.end && this.last_char != ')' && this.last_char != '\''){
                    identifier += this.last_char;
                }
            }

            this.last_char = this.getChar(this.pos, peekAmount);
            this.type = "string";
            this.lastType = "string";
            this.lastToken = identifier;
            this.depthChange = false;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            this.precedence = 7;
            return identifier;

        }

        matcher = pattern.matcher(Character.toString(last_char));
        Pattern pattern2 = Pattern.compile("[a-zA-Z0-9_]+");
        if (matcher.find()) {
            identifier = "";

            while (!this.end && pattern2.matcher(Character.toString(this.last_char)).find()) {

                identifier = identifier + this.last_char;
                this.last_char = this.getChar(this.pos, peekAmount);
            }

            if (this.end && this.last_char != ')') {
                identifier += this.last_char;
            }
            this.type = "identifier";
            this.depthChange = false;
            this.lastType = "identifier";
            this.lastToken = identifier;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            this.precedence = 1;
            return identifier;
        }

        if (this.last_char == '=') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.type = "operator";
            this.lastType = "operator";
            this.lastToken = "eq";
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            this.precedence = 100;
            return lastToken;
        }

        if (this.last_char == '+') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.type = "operator";
            this.lastType = "operator";
            this.lastToken = "plus";
            // BODMAS
            this.precedence = 8;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }

        if (this.last_char == '-') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.type = "operator";
            this.lastType = "operator";
            this.lastToken = "minus";
            // BODMAS
            this.precedence = 6;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }

        if (this.last_char == '~') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.type = "operator";
            this.lastType = "operator";
            this.lastToken = "tilde";
            this.precedence = 7;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return this.lastToken;
        }

        if (this.last_char == ',') {
            this.last_char = this.getChar(this.pos, peekAmount);
            lastToken = "comma";
            this.precedence = 1;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }

        if (this.last_char == '<') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.type = "operator";
            this.lastType = "operator";
            this.lastToken = "lessthan";
            this.precedence = 7;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }
        if (this.last_char == '[') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.lastToken = "opensquare";
            this.type = "arraybegin";
            this.depthChange = true;

            this.depthWhich = SQUARE_BRACKET;
            this.depthAmount = 1;
            this.depth[depthWhich] += this.depthAmount;
            this.precedence = 0;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }
        if (this.last_char == ']') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.lastToken = "closesquare";

            this.type = "arrayend";
            this.closeToken = true;
            this.depthChange = true;
            this.depthWhich = SQUARE_BRACKET;
            this.depthAmount = -1;
            this.depth[depthWhich] += this.depthAmount;
            this.precedence = 0;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return this.lastToken;
        }
        if (this.last_char == '>') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.type = "operator";
            this.lastType = "operator";
            this.lastToken = "greaterthan";
            this.precedence = 1;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return this.lastToken;
        }
        if (this.last_char == '.') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.type = "property";
            this.lastType = "property";
            this.lastToken = "property";
            this.precedence = 0;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }
        if (this.last_char == '{') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.lastToken = "opencurly";
            this.depthChange = true;
            this.type = "hash";
            this.depthWhich = CURLY_BRACKETS;
            this.depthAmount = 1;
            this.depth[depthWhich] += this.depthAmount;
            this.precedence = 7;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return this.lastToken;
        }
        if (this.last_char == '}') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.lastToken = "closecurly";
            this.precedence = 7;
            this.closeToken = true;
            this.depthChange = true;
            this.depthWhich = CURLY_BRACKETS;
            this.depthAmount = -1;
            this.depth[depthWhich] += this.depthAmount;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return this.lastToken;
        }
        if (this.last_char == ';') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.lastToken = "semicolon";
            this.depthChange = false;
            this.precedence = 7;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return this.lastToken;
        }
        if (this.last_char == '|') {
            this.last_char = this.getChar(this.pos, peekAmount);
            this.type = "pipe";
            this.lastType = "pipe";
            this.lastToken = "pipe";
            this.precedence = 0;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }

        if (this.end) {
            this.lastToken = null;
            return null;
        }
        this.lastToken = null;
        return null;
    }

    public MultiplexedAST parse() {
        String token = "";

        Stateline stateline = new Stateline();
        List<Stateline> statelines = new ArrayList<>();
        statelines.add(stateline);
        while (!this.end) {
            System.out.println(token);
            token = getToken(false, 1);

            System.out.println(token);
            if (type.equals("identifier")) {
                System.out.println("Found identifier");
                if ( parseFact(token, stateline)) {
                    stateline = new Stateline();
                    statelines.add(stateline);
                }
            }
        }
        System.out.println(statelines);
        MultiplexedAST trie = createTrie(statelines);

        return trie;
    }

    private MultiplexedAST createTrie(List<Stateline> statelines) {
        MultiplexedAST ast = new MultiplexedAST(statelines);
        for (Stateline stateline : statelines) {
            for (Identifier identifier : stateline.identifiers) {
                for (Fact fact : identifier.arguments) {
                    ast.register(fact, identifier, stateline);

                }
            }
        }
        return ast;
    }

    /**
     * thread(x) = a(optional, two, three) m(hello) | b(four, five) | c | d
     * identifier openbracket identifier closebracket eq identifier openbracket identifier comma identifier comma identifier closebracket pipe
     *
     * @param token
     * @param stateline
     * @return
     */
    private boolean parseFact(String token2, Stateline stateline) {

        System.out.println("peeked " + type);

        System.out.println("looping in facts");
        Identifier identifier = new Identifier(token2);
        stateline.add(identifier);
        while (!end && (!token2.equals("pipe")) && (!token2.equals("eq") && !token2.equals("semicolon"))) {

            String token1 = getToken(false, 1);
            if (token1.equals("openbracket")) {
                token1 = getToken(false, 1);
            }
            if (token1.equals("pipe")) {
                break;
            }
            if (token1.equals("eq")) {
                break;
            }
            if (token1.equals("semicolon")) {
                return true;
            }
            System.out.println(String.format("token is %s", token1));
            Fact fact = new Fact(token1);
            identifier.add(fact);
            token2 = getToken(false, 1);

                while (!end && (!token2.equals("pipe"))) {

                    if (token2.equals("openbracket") || token2.equals("comma")) {
                        System.out.println("Found openbracket");
                        token2 = getToken(false, 1);

                        continue;
                    }
                    if (token2.equals("closebracket")) {
                        System.out.println("Breaking");
                    }

                    if (!token2.equals("openbracket") && !token2.equals("closebracket")) {

                    }
                    if (token2.equals("closebracket")) {
                        break;
                    }
                    if (token2.equals("pipe")) {
                        break;
                    }
                    if (type.equals("identifier")) {
                        fact.add(token2);
                    }

                    token2 = getToken(false, 1);

            }

            // token2 = getToken(false, 1);

            System.out.println(token2);
        }
        System.out.println("Cancelled");
        return false;
    }

    public class Stateline {
        public List<Identifier> identifiers;
        public volatile boolean runnable = false;

        public Stateline() {
            this.identifiers = new ArrayList<>();
        }
        public void add(Identifier identifier) {
            this.identifiers.add(identifier);

        }
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            for (Identifier identifier : identifiers) {
                stringBuilder.append("[");
                stringBuilder.append(identifier.toString());
                stringBuilder.append("]");
                stringBuilder.append("|");
            }
            return stringBuilder.toString();
        }
    }

    public class Identifier {
        public int submitted;
        String identifier;
        List<Fact> arguments;
        public Identifier(String identifier) {
            this.identifier = identifier;
            this.arguments = new ArrayList<>();
        }

        public void add(Fact token) {
            this.arguments.add(token);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(identifier);
            stringBuilder.append(" ");
            for (Fact identifier : arguments) {
                stringBuilder.append("<");
                stringBuilder.append(identifier.toString());
                stringBuilder.append(">");
                stringBuilder.append(",");
            }
            return stringBuilder.toString();
        }

        public boolean pending() {
            for (Fact fact : arguments) {
                if (fact.submitted < fact.pending) {
                    return true;
                }
            }
            return false;
        }
    }

    public class Fact {
        public String name;
        public List<String> arguments;
        public int submitted;
        public int pending;
        public List<String> values;

        public Fact(String name) {
            this.name = name;
            this.arguments = new ArrayList<>();
            this.values = new ArrayList<>();
        }

        public void add(String token2) {
            this.arguments.add(token2);
        }
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" ");
            stringBuilder.append(name);
            stringBuilder.append(" ");
            for (String identifier : arguments) {
                stringBuilder.append(identifier.toString());
                stringBuilder.append(",");
            }
            return stringBuilder.toString();
        }

        public void submit(String value) {
            System.out.println(String.format("Added value %s", value));
            this.values.add(value);
        }
    }
}

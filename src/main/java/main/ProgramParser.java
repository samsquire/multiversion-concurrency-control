package main;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProgramParser {
    private static final EndOfExpressionAST END_OF_EXPRESSION = new EndOfExpressionAST(true);;
    private static final EndOfBlockAST END_OF_BLOCK = new EndOfBlockAST();
    private static final EndOfFileAST END_OF_FILE = new EndOfFileAST();
    private static final AST END_OF_ARRAY = new EndOfArrayAST();
    private static final AST END_OF_PARAMETERS = new EndOfParameterListAST();
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
    private int[] lastDepth;

    public Map<String, String> tokenHandling;
    private String lastType;
    private boolean depthChange;
    private int depthWhich;
    private int depthAmount;
    private boolean closeToken;
    private boolean previousDepthChange;

    public ProgramParser(String programString) {
        pattern = Pattern.compile("[a-zA-Z0-9_]+", Pattern.CASE_INSENSITIVE);
        matcher = null;
        this.programString = programString;
        this.tokenHandling = new HashMap<>();
        this.tokenHandling.put("type operator", "append");
        this.tokenHandling.put("type openparameter", "promoteleft");
        this.tokenHandling.put("posttype operator", "append");

        this.tokenHandling.put("type string", "append");
        this.tokenHandling.put("type identifier", "append");
        this.tokenHandling.put("type property", "promoteleft");
        this.tokenHandling.put("type arraybegin", "append");
        this.tokenHandling.put("type arrayend", "ignore");
        this.tokenHandling.put("type token", "append");
        this.depth = new int[3];
        Arrays.fill(depth, 0);
        this.lastDepth = new int[3];
        Arrays.fill(lastDepth, 0);
        this.depthChange = false;

    }

    public char getChar(int pos, int amount) {

        char letter = programString.charAt(pos);
        if (pos + amount >= this.programString.length()) {
            this.end = true;
            return letter;
        } else {
            this.end = false;
        }
        this.pos = pos + 1;

        return letter;
    }

    public String getToken(boolean peek, int peekAmount) {
        String token = getTokenReal(peek, peekAmount);
        System.out.println(String.format("TOKEN GET: %s (%b)", token, peek));
        for (int i = 0; i < depth.length; i++) {
            System.out.println(depth[i]);
        }
        return token;
    }

    public void resetDepthChange() {
        this.depthChange = false;
    }

    public String getTokenReal(boolean peek, int peekAmount) {
        int remembered = this.pos;

        this.previousDepthChange = this.depthChange;
        this.depthWhich = -1;
        this.depthAmount = 0;
        for (int i = 0 ; i < depth.length; i++) {
            depth[i] = lastDepth[i];
        }

        char remembered_char = this.last_char;

        String rememberedType = this.type;
        String rememberedlastToken = this.lastToken;
        this.lastPrecedence = this.precedence;
        this.type = "token";
        this.lastType = "token";
        while (this.end == false && (this.last_char == ' ' || this.last_char == '\n' || this.last_char == Character.MIN_VALUE)) {
            this.last_char = getChar(this.pos, 1);
        }

        if (this.last_char == '(') {
            this.last_char = getChar(this.pos, 1);
            this.type = "openparameter";
            lastToken = "openbracket";
            this.depthChange = true;
            this.depthWhich = BRACKETS;
            this.depthAmount = 1;
            for (int i = 0 ; i < depth.length; i++) {
                lastDepth[i] = depth[i];
            }
            this.depth[depthWhich] += this.depthAmount;
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
            for (int i = 0 ; i < depth.length; i++) {
                lastDepth[i] = depth[i];
            }
            this.depth[depthWhich] += this.depthAmount;
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
            for (int i = 0 ; i < depth.length; i++) {
                lastDepth[i] = depth[i];
            }
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
            for (int i = 0 ; i < depth.length; i++) {
                lastDepth[i] = depth[i];
            }
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
            for (int i = 0 ; i < depth.length; i++) {
                lastDepth[i] = depth[i];
            }
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
            for (int i = 0 ; i < depth.length; i++) {
                lastDepth[i] = depth[i];
            }
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


        if (this.end) {
            this.lastToken = null;
            return null;
        }
        this.lastToken = null;
        return null;
    }

    public AST parse() {
        String token = getToken(false, 1);
        AST programAST = null;

        switch (token) {
            case "threads":
                programAST = new ProgramAST(getToken(false, 1));
        }
        System.out.println(programAST);
        AST astNode = nextAST();
        while (astNode != null) {
            programAST.add(astNode);
            System.out.println("Parsing main method node");
            astNode = nextAST();

        }
        System.out.println("Finished parsing");
        System.out.println(programAST);
        return programAST;
    }

    private AST nextAST() {
        int[] depthExpect = new int[3];
        for (int i = 0; i < depth.length; i++) {
            depthExpect[i] = depth[i];
            System.out.println(String.format("Expected: %d", depth[i]));
        }
        System.out.println("nextast");
        String token = getToken(false, 1);
        System.out.println(String.format("nextAst Token: %s", token));
        if (token == null) {
            return null;
        }
        System.out.println(token);
        switch (token) {
            case "lessthan":

                String start = getToken(false, 1);
                if (!start.equals("start")) {
                    throw new IllegalArgumentException("Parse error, expected <start>");
                }
                String end = getToken(false, 1);
                assert end.equals("greaterthan");
                System.out.println("Encountered start");
                return new StartProgramAST();
            case "function":
                return parseFunctionDeclaration(depthExpect);
            case "set":
                return parseVariableDeclaration(depthExpect);
            case "for":
                return parseForLoopExpression(depthExpect);
            case "semicolon":
                return null;
            default:
                System.out.println(String.format("Token %s type is %s", token, type));
                switch (this.type) {
                    case "identifier":
                        String token1 = getToken(true, 1);
                        System.out.println(String.format("Identifier match: %s", token1));
                        if ( token1.equals("openbracket")) {
                            System.out.println("Found method call");
                            return new MethodCallAST(token,
                                    parseExpression("toplevel", 0,
                                    List.of(), depthExpect));
                        }
                        break;
                }
                return null;


        }
    }

    private ForLoopAST parseForLoopExpression(int[] depthExpect) {
        String token;
        System.out.println("Parsing loop");

        token = getToken(false, 1);
        for (int i = 0 ; i < depth.length; i++) {
            System.out.println(String.format("Loop declaration Expected depth: %d", depth[i]));
            depthExpect[i] = depth[i];
        }


        if (!token.equals("openbracket")) {
            throw new IllegalArgumentException("Expected open bracket for loop beginning");
        }
        List<String> tokens = new ArrayList<>();
        List<String> types = new ArrayList<>();
        token = getToken(false, 1);
        while (token != null && !token.equals("semicolon")) {
            tokens.add(token);
            types.add(this.type);
            token = getToken(false, 1);


        }
        System.out.println("Parsing assignment of loop");
        AST assignment = parseAssignmentStatement(tokens, types);
        System.out.println(String.format("loop assignment %s", assignment));
        if (!token.equals("semicolon")) {
            throw new IllegalArgumentException("Expected semicolon after loop initializer");
        }
        System.out.println("Beginning parsing expression");
        AST expression = parseExpression("label", 0, null, depthExpect);
        System.out.println(String.format("Parsing loop expression %s", expression));
        int position = pos;
        char letter = programString.charAt(position);
        System.out.println();
        while (letter != '\n' && position + 1 < programString.length()) {
            System.out.print(letter);
            letter = programString.charAt(++position);
        }
        System.out.println();
        AST postexpression = parseExpression("looppostexpression", 0, List.of("closebracket"), depthExpect);

        String curly = getToken(false, 1);
        System.out.println(String.format("Loop body start %s %s", curly, postexpression));
        int position2 = pos;
        char letter2 = programString.charAt(position);
        System.out.println();
        while (letter2 != '\n' && position2 + 1 < programString.length()) {
            System.out.print(letter2);
            letter2 = programString.charAt(++position2);
        }
        System.out.println();
        if (!curly.equals("opencurly")) {
            throw new IllegalArgumentException(String.format("Expected curly bracket open loop body, was %s", curly));
        }

        ForLoopAST forLoopAST = new ForLoopAST(assignment, expression, postexpression);
        AST loopBodyExpression = parseFunctionBodyItem("forloopbody", depthExpect);

        while (!isExpressionStop("forparse", loopBodyExpression, depth, depthExpect)) {
            forLoopAST.add(loopBodyExpression);
            loopBodyExpression = parseFunctionBodyItem("forloopbody", depthExpect);
            System.out.println("For loop body");
        }
        String peeked = getToken(true, 1);
        System.out.println(String.format("For loop peeked %s", peeked));
        return forLoopAST;
    }

    private AST parseVariableDeclaration(int[] depthExpect) {
        String variableType = getToken(false, 1);
        String variableName = getToken(false, 1);
        System.out.println(String.format("Parsing variable declaration %s %s", variableType, variableName));
        String equals = getToken(false, 1);
        if (!equals.equals("eq")) {
            throw new IllegalArgumentException("Expected variable declaration to have equals");
        }
        AST expression = parseExpression("label", 0, null, depthExpect);
        System.out.println(String.format("Variable declaration expression %s %s %s", expression, lastToken, getToken(true, 1)));
        String semicolon = getToken(false, 1);
        System.out.println(String.format("End of variable declaration statement %s", semicolon));
        if (!semicolon.equals("semicolon")) {
            throw new IllegalArgumentException("Variable setting doesn't end with semicolon");
        }
        return new VariableDeclarationAST(variableType, variableName, expression);
    }

    private AST parseExpression(String label, int precedence, List<String> customStop, int[] depthExpect) {
        System.out.println("Printing stack trace:");
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < elements.length; i++) {
            StackTraceElement s = elements[i];
            System.out.println("\tat " + s.getClassName() + "." + s.getMethodName()
                    + "(" + s.getFileName() + ":" + s.getLineNumber() + ")");
        }

        int start = this.pos;
        System.out.println(String.format("%s PARSE EXPRESSION %d", label, this.pos));
        int[] depthToSeek = new int[3];
        for (int i = 0 ; i < depth.length; i++) {
            depthToSeek[i] = depth[i];
        }
        String token = getToken(false, 1);
        String originalType = type;
        if (token != null && token.equals("comma")) {
            token = getToken(false, 1);
        }
        System.out.println(String.format("%s Expression token %s", label, token));
        System.out.println(programString.substring(this.pos, programString.length()));
        AST tokenStop = isTokenStop(token, customStop);
        if (tokenStop != null) {
            System.out.println(String.format("%s First Token stop parse expression %s", label, tokenStop));
            // rewind(1);
            tokenStop.setStopped();
            return tokenStop;
        }
        System.out.println(token);
        if (token.equals(null)) {
            return null;
        }

        // expression code

        AST innerAst = parseToken(token, depthExpect, customStop);

        if (innerAst != null && innerAst.getClass() == StructureAST.class) {
            return innerAst;
        }

        AST left = new ExpressionAST(innerAst);
        if (innerAst != null) {
            innerAst.parent = left;
            left.setType(originalType);
        }
        if (innerAst != null && innerAst.getClass() == ArrayAccessAST.class) {
            left = innerAst;
        }


        System.out.println(String.format("%s left: %s %s", label, token, left));
        int previousPos = this.pos;
        int[] depthToUse = new int[3];
        for (int i = 0 ; i < depth.length; i++) {
            depthToUse[i] = depth[i];
        }
        String peeked = getToken(true, 1);
        String peekedType = this.lastType;
        tokenStop = isTokenStop(peeked, customStop);
        if (tokenStop != null) {
//            rewind(1);
            System.out.println(String.format("%s %s FOUND END OF EXPRESSION", label, tokenStop));
            return left;
        }
        int nextPos = this.pos;
        String peeked2 = getToken(true, 1);
        // assert peeked.equals(peeked2);
        int lastPos = this.pos;
        assert (previousPos == nextPos) && previousPos == lastPos;
        System.out.println(String.format("%s peeked: %s %d", label, peeked, this.precedence, precedence));

        List<AST> stack = new ArrayList<>();
        stack.add(left);
        resetDepthChange();
        AST returnValue = null;
        while (this.precedence > precedence) {
            resetDepthChange();
            System.out.println(String.format("%s %s.%d > %s.%d", label, peeked, this.precedence, token, precedence));
            AST newLeft = parseExpression(label + "nestedexpression", 0, customStop, depthExpect);
            System.out.println(String.format("%s NewLeft is %s", left, newLeft));
            newLeft.parent = left;

            boolean expressionStop = isExpressionStop("mainparse", newLeft, depthToUse, depthExpect);

            boolean stopped = false;
            if (newLeft.equals(END_OF_EXPRESSION) || newLeft.equals(END_OF_PARAMETERS) || newLeft.equals(END_OF_BLOCK)) {
                System.out.println(String.format("%s REACHED END OF EXPRESSION %s", label, left));
                newLeft.setStopped();
                return left;
            }

            if (expressionStop || left.isStopped() || newLeft.isStopped() || innerAst.isStopped()) {
                System.out.println(String.format("%s Expression stopped on %s", label, newLeft));
                // rewind(1);



                if (newLeft.isStopped()) {
                    stopped = true;
                    left.setStopped();
                    newLeft.setStopped();

                } else if (left.isStopped()) {

                    newLeft.setStopped();
                    stopped = true;
                }
            }
//            if (newLeft == END_OF_EXPRESSION || newLeft == END_OF_FILE || newLeft == END_OF_BLOCK || newLeft == END_OF_ARRAY) {
//                System.out.println("new left was null");
//                left.setStopped();
//                return left;
//            }
            System.out.println(String.format("%s new left %s %s", type, label, newLeft));
            String typeToUse = originalType;
            String postTypeCheck = String.format("posttype %s", newLeft.type);
            String typeCheck;
            typeCheck = String.format("type %s", typeToUse);
            if (tokenHandling.containsKey(postTypeCheck)) {
                typeCheck = postTypeCheck;
                System.out.println(String.format("type was %s using %s instead", originalType, newLeft.type));
            }
            System.out.println(String.format("%s %s type %s %s %s", originalType, peekedType, left, newLeft, typeCheck));
            String behaviour = "";

            System.out.println(typeCheck);
                if (tokenHandling.containsKey(typeCheck)) {
                    behaviour = tokenHandling.get(typeCheck);
                    System.out.println(String.format("%s %s behaviour is %s %s", peekedType, label, behaviour, newLeft));

                } else {
                    System.out.println(String.format("Not found %s ", typeCheck));
                }
                switch (behaviour) {

                    case "append":
                        System.out.println(String.format("Appending %s", newLeft));

                        left.add(newLeft);
                        peeked = getToken(true, 1);
                        System.out.println(String.format("peeked is %s", peeked));
                        tokenStop = isTokenStop(peeked, customStop);
                        if (tokenStop != null) {
//                        rewind(1);
                            left.setStopped();
                            newLeft.setStopped();
                            return left;
                        }
                        peekedType = this.lastType;
                        originalType = this.lastType;
                        break;
                    case "appendparent":
                        System.out.println(String.format("Appending %s", newLeft));

                        left.parent.add(newLeft);
                        peeked = getToken(true, 1);
                        System.out.println(String.format("peeked is %s", peeked));
                        tokenStop = isTokenStop(peeked, customStop);
                        if (tokenStop != null) {
//                        rewind(1);
                            left.setStopped();
                            newLeft.setStopped();
                            return left;
                        }
                        peekedType = this.lastType;
                        originalType = this.lastType;
                        break;
                    case "promoteleft":
                        System.out.println(String.format("promoting %s, subsuming %s", token, newLeft));
                        if (newLeft != null) {
                            List<AST> children = ((ExpressionAST) newLeft).children;
                            // ((ExpressionAST)left).children.get(0).add(children.get(0));
                            // ((ExpressionAST)left).children.addAll(children.subList(0, children.size()));
                            // left = newLeft;
                            left.add(newLeft);
                            peeked = getToken(true, 1);
                            System.out.println(String.format("peeked is %s", peeked));

                            tokenStop = isTokenStop(peeked, customStop);
                            if (tokenStop != null) {
//                            rewind(1);
                                left.setStopped();
                                newLeft.setStopped();
                                return left;
                            }
                            peekedType = this.lastType;
                            originalType = this.lastType;
                        } else {
                            left.setStopped();
                            return left;
                        }
                        break;
                    case "promote":

                        System.out.println(String.format("promote, promoting %s %s, subsuming %s", left, token, newLeft));
                        if (newLeft != null) {
                            left.add(newLeft);
                            AST oldLeft = newLeft;
                            // left = newLeft;
                            //newLeft = oldLeft;
                            peeked = getToken(true, 1);
                            System.out.println(String.format("peeked is %s", peeked));
                            System.out.println(left);
                            tokenStop = isTokenStop(peeked, customStop);
                            if (tokenStop != null) {
//                            rewind(1);
                                System.out.println(String.format("TOKEN2STOP %s %s", left, newLeft));
                                left.setStopped();
                                newLeft.setStopped();
                                return left;
                            }
                            peekedType = this.lastType;
                            originalType = this.lastType;
                        } else {
                            left.setStopped();
                            return left;
                        }

//                        break;
                    default:
                        originalType = type;
                        break;


            }
            if (stopped) {
                System.out.println(String.format("Stopped, returning %s", left));
                returnValue = left;
                return returnValue;
            }
        }
        System.out.println(String.format("left is %s", left));
        return left;
    }

    private boolean isExpressionStop(String label, AST ast, int[] depthToUse, int[] depthExpect) {
        if (ast == END_OF_FILE) {
            return true;
        }

        if (ast == END_OF_EXPRESSION) {
            System.out.println(String.format("%s end of expression", label));
            return true;
        }
        if (ast == END_OF_BLOCK) {
            System.out.println(String.format("%s end of block", label));
            return true;
        }
        for (int i = 0 ; i < depthToUse.length; i++) {
//            System.out.println(String.format("%s Comparing depth %d %d", label, depthToUse[i], depthExpect[i]));
        }
        if (this.previousDepthChange && Arrays.equals(depthToUse, depthExpect) && !Arrays.equals(depthToUse, lastDepth)) {
            System.out.println(String.format("DEPTH CHANGE %s", label, ast));
            return true;
        }


        if (ast == null) {
            return true;
        }


        System.out.println(String.format("%s expression stop", ast));
        return false;
    }

    private void rewind(int rewind) {
        this.pos = this.pos - rewind;
    }

    private AST isTokenStop(String token, List<String> customStop) {
        if (token == null) {
            return END_OF_FILE;
        }
        if (customStop != null && customStop.contains(token)) {
            System.out.println(String.format("Stopping due to custom stop %s %s", token, customStop));
            return END_OF_EXPRESSION;
        }
        if (token.equals("closesquare")) {
            return END_OF_ARRAY;
        }

        if (token.equals("semicolon")) {
            return END_OF_EXPRESSION;
        }
        if (token.equals("closecurly")) {
            return END_OF_BLOCK;
        }
//        if (token.equals("closebracket")) {
//            System.out.println("WAS END OF PARAMETERS");
//            return END_OF_PARAMETERS;
//        }
        return null;
    }

    private AST parseToken(String token, int[] depthExpect, List<String> customStop) {
        System.out.println("PARSE TOKEN");
        AST ast = null;
        int[] currentDepthExpect = new int[3];
        if (this.type.equals("string")) {
            ast = new LiteralStringAST(token);
            System.out.println("literal string " + token);
        } else
        if (isNumber(token)) {
            System.out.println("FOUND A NUMBER");
            ast = new LiteralNumberAST(token);
            return ast;
        }
        if (this.type.equals("operator")) {
            String nextToken = getToken(true, 2);
            System.out.println(String.format("peeked token is %s %s", token, nextToken));
            if (nextToken.equals("plus")) {
                // we are a compound operator
                getToken(false, 1); // use up the eq
                ast = new OperatorAST(String.format("%seq", token));
            } else {
                ast = new OperatorAST(token);
            }

        }
        if (this.type.equals("identifier")) {
            if (isNumber(token)) {
                ast = new IdentifierAST(token, "int");
            } else {
                ast = new IdentifierAST(token, "string");

            }
        }
        if (this.type.equals("arraybegin")) {
            System.out.println("Array access");
            ast = new ArrayAccessAST();
        }
        if (token.equals("openbracket")) {
            System.out.println("Method call");
            ast = new ParameterListAST();
        }
        if (token.equals("closebracket")) {
            ast = END_OF_PARAMETERS;
        }
        if (token.equals("closesquare")) {
            return new EndOfArrayAST();
        }
        if (token.equals("property")) {
            System.out.println("encountered property");
            ast = new PropertyAccessAST();
        }
        if (token.equals("opencurly")) {
            System.out.println("Parsing structure");
            int position = pos;
            char letter = programString.charAt(position);
            System.out.println();
            while (position < programString.length() -1) {
                System.out.print(letter);
                letter = programString.charAt(++position);
            }
            System.out.println();
            for (int i = 0 ; i < depth.length; i++) {
                currentDepthExpect[i] = depth[i];
            }
            Map<AST, AST> data = new HashMap<>();
            ast = new StructureAST(data);
            AST current = ast;

            String seek = getToken(true, 1);
            System.out.println(String.format("Structure parsing seek %s %s", token, seek));
            if (seek.equals("closecurly")) {
                // literal
                System.out.println("Immediately closing struct");
                getToken(false, 1);
                return ast;
            } else {
                // rewind(1);
            }

            while (!isExpressionStop("structureparse", current, depth, depthExpect)) {
                System.out.println("STRUCTURE PARSING X");
                AST fieldKey = parseExpression("label", 0, List.of("eq"), depthExpect);
                if (fieldKey.equals(END_OF_BLOCK)) {
                    break;
                }
//                if (isExpressionStop("structurekeyparse", fieldKey, depth, depthExpect)) {
//                    System.out.println("STRUCTURE EXPRESSION END");
//                    return ast;
//                }


                System.out.println(String.format("Structure parsing: %s", fieldKey));
                AST fieldValue = parseExpression("label", 0, List.of("semicolon"), depthExpect);
                if (fieldValue.equals(END_OF_BLOCK)) {
                    break;
                }
//                if (isExpressionStop("structurevalueparse", fieldValue, depth, currentDepthExpect)) {
//                    System.out.println("STRUCTURE EXPRESSION END 2");
//                    return ast;
//                }
                int position2 = pos;
                char letter2 = programString.charAt(position);
                System.out.println();
                while (letter2 != '\n' && position2 + 1 < programString.length()) {
                    System.out.print(letter2);
                    letter2 = programString.charAt(++position2);
                }
                System.out.println();
                String comma = getToken(true, 1);
                if (comma != null && comma.equals("comma")) {
                    getToken(false, 1);
                }
//
                System.out.println(String.format("Structure Field value: %s", fieldValue));

                for (int i = 0 ; i < depth.length; i++) {
                    System.out.println(String.format("Structure end processing depth %d", lastDepth[i]));
                }
                data.put(fieldKey, fieldValue);
                String token1 = getToken(true, 1);

                System.out.println(String.format("Structure lookahead %s", token1));
                if ("semicolon".equals(token1)) {
                    System.out.println("Reached end of structure");
                    token1 = getToken(false, 1);
                    System.out.println(String.format("After semicolon, end of structure %s", token1));
                    if (token1.equals("closecurly")) {
                        break;
                    }
                }

                current = fieldValue;
            }

            System.out.println(String.format("Exit structure token parsing %s", ast));
            ast.stopped = true;
            return ast;
        }

        if (ast == null) {
            System.out.println(String.format("Token is null %s", token));
        }
        if (token.equals("for")) {
            return parseForLoopExpression(depthExpect);
        }

        return ast;
    }

    private boolean isNumber(String token) {
        Pattern pattern = Pattern.compile("[0-9.]+", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(token).find();
    }

    private AST parseList(String token) {
        List<AST> ast = new ArrayList<>();
        while (!token.equals("closesquare")) {
            AST fieldValue = parseExpression("label", 0, null, new int[3]);
            ast.add(fieldValue);
            token = getToken(false, 1);
            if (token == null) {
                break;
            }
            if (token.equals("comma")) {
                token = getToken(false, 1);
            }
        }
        return new ListAST(ast);
    }

    private StructureAST parseStructure(String token) {
        if (token == null) {
            return null;
        }
        Map<AST, AST> ast = new HashMap<>();
        while (token != null && !token.equals("closecurly")) {
            String fieldName = getToken(false, 1);
            AST fieldValue = parseExpression("label", 0, null, new int[3]);
            ast.put(fieldValue, fieldValue);
            token = getToken(false, 1);
        }
        return new StructureAST(ast);
    }

    private AST parseMain(String token, int[] depthExpect) {

        System.out.println(String.format("parseMain Token: %s", token));
        while (token != null && !token.equals("closecurly")) {
            switch (token) {


            }
            token = getToken(false, 1);
            System.out.println(String.format("parseMain Token: %s", token));

        }
        return null;
    }

    private boolean isForLoopExpressionStop(AST ast) {
        if (ast == END_OF_BLOCK) {
            return true;
        }
        return false;
    }

    private AST parseFunctionDeclaration(int[] depthExpect) {
        String functionName = getToken(false, 1);
        String bracket = getToken(false, 1);
        if (!bracket.equals("openbracket")) {
            throw new IllegalArgumentException("Parse error, expected bracket in function declaration beginning");
        }
        List<Argument> arguments = new ArrayList<>();

        String currentToken = getToken(false, 1);
        while (!currentToken.equals("closebracket")) {
            System.out.println(String.format("Parsing function declaration: Token: %s", currentToken));
            String argumentType = currentToken;
            String argumentName = getToken(false, 1);
            Argument e = new Argument(argumentName, argumentType);
            System.out.println(e);
            arguments.add(e);
            currentToken = getToken(false, 1);
            if (currentToken.equals("comma")) {
                currentToken = getToken(false, 1);
            }

        }

        depthExpect = new int[3];
        for (int i = 0 ; i < depth.length; i++) {
            System.out.println(String.format("Function declaration Expected depth: %d", depth[i]));
            depthExpect[i] = depth[i];
        }
        String curly = getToken(false, 1);

        System.out.println(String.format("CurlyToken: %s", curly));
        System.out.println("Beginning of function body parsing");
        if (!curly.equals("opencurly")) {
            throw new IllegalArgumentException("Expected curly open for function declaration");
        }
        FunctionAST functionAST = new FunctionAST(functionName, arguments);
        AST expression = parseFunctionBodyItem("functionast", depthExpect);
        System.out.println(String.format("parseFunctionDeclaration Expression: %s", expression));
        while (!isExpressionStop("funcbodyparse", expression, depth, depthExpect)) {
            System.out.println("before parseFunctionDeclaration parseFunctionBodyItem");
            if (expression.getClass() != EndOfArrayAST.class) {
                functionAST.add(expression);
            }
            AST bodyItem = parseFunctionBodyItem("functionast", depthExpect);
            System.out.println(String.format("Function body item %s", bodyItem));
            expression = bodyItem;

        }
        System.out.println(String.format("FUNAST2", functionAST));
        return functionAST;
    }

    private AST parseFunctionBodyItem(String label, int[] depthExpect) {
        AST expression = parseExpression(label, 0, List.of("semicolon"), depthExpect);
        return expression;
    }

    private AST parseFunctionBodyItem2(String token) {
        List<String> tokens = new ArrayList<>();
        List<String> types = new ArrayList<>();
        tokens.add(token);
        types.add(this.type);
        System.out.println(String.format("FunctionBodyItem Token %s", token));
        token = getToken(false, 1);
        System.out.println(String.format("FunctionBodyItem Token %s", token));

        String whatis = null;
        if (token == null) {
            return null;
        }
        while (token != null && !token.equals("semicolon")) {
            System.out.println(String.format("parseFunctionBodyItem Token: %s", token));
            switch (token) {
                case "eq":
                    whatis = "assignment";
                    break;
                case "openbracket":
                    whatis = "methodcall";
                    break;
            }
            tokens.add(token);
            types.add(this.type);
            token = getToken(false, 1);
        }
        System.out.println(String.format("After parseFunctionBodyItem %s", token));


        if (!token.equals("semicolon")) {
            throw new IllegalArgumentException("Expected end of line semicolon");
        }


        switch (whatis) {
            case "assignment":
                System.out.println("is an assignment");
                return parseAssignmentStatement(tokens, types);
            case "methodcall":
                System.out.println("Is a method call");
                return parseMethodCall();
        }
        return null;
    }

    private AST parseMethodCall() {
        return null;
    }

    private AST parseAssignmentStatement(List<String> tokens, List<String> types) {
        List<String> lhs = new ArrayList<>();
        List<String> operators = new ArrayList<>();
        List<String> rhs = new ArrayList<>();
        List<String> current = lhs;
        boolean finishedOperators = false;
        for (int i = 0 ; i < tokens.size(); i++) {
            if (tokens.get(i).equals("eq")) {
                finishedOperators = true;
                operators.add(tokens.get(i));
            } else if (!finishedOperators && types.get(i).equals("operator")) {
                current = operators;
                current.add(tokens.get(i));
            } else {
                current.add(tokens.get(i));
            }
            if (finishedOperators) {
                current = rhs;
            }
        }
        boolean compound = false;
        for (int i = 0 ; i < lhs.size(); i++) {
            System.out.println(lhs.get(i));
            if (lhs.get(i).equals("property")) {
                compound = true;
            }
        }
        List<Integer> removals = new ArrayList<>();
        for (int i = 0 ; i < lhs.size(); i++) {
            if (lhs.get(i).equals("property")) {
                removals.add(i);
            }
        }
        for (int i = removals.size() - 1; i >= 0; i--) {
            lhs.remove(removals.get(i));
        }
        DestinationVariable dv;
        if (compound) {
            dv = new CompoundDestinationVariable(lhs);
        } else {
            dv = new SimpleDestinationVariable(lhs);
        }
        return new AssignmentAST(dv, operators, rhs);
    }
}

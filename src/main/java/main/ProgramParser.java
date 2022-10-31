package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProgramParser {
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

    public Map<String, String> tokenHandling;
    private String lastType;

    public ProgramParser(String programString) {
        pattern = Pattern.compile("[a-zA-Z0-9_]+", Pattern.CASE_INSENSITIVE);


        matcher = null;
        this.programString = programString;
        this.tokenHandling = new HashMap<>();
        this.tokenHandling.put("type operator", "promote");
        this.tokenHandling.put("type identifier", "append");

    }

    public char getChar(int pos, boolean peek) {

        char letter = programString.charAt(pos);
        if (pos + 1 >= this.programString.length()) {
            this.end = true;
            return letter;
        } else {
            this.end = false;
        }
        this.pos = pos + 1;

        return letter;
    }

    public String getToken(boolean peek) {
        int remembered = this.pos;
        char remembered_char = this.last_char;

        String rememberedType = this.type;
        String rememberedlastToken = this.lastToken;
        this.lastPrecedence = this.precedence;
        this.type = "token";
        this.lastType = "token";
        while (this.end == false && (this.last_char == ' ' || this.last_char == '\n' || this.last_char == Character.MIN_VALUE)) {
            this.last_char = getChar(this.pos, peek);
        }


        if (this.last_char == '(') {
            this.last_char = getChar(this.pos, peek);
            lastToken = "openbracket";

            this.precedence = 100;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }

        if (this.last_char == ')') {
            this.last_char = this.getChar(this.pos, peek);
            lastToken = "closebracket";
            this.precedence = 100;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }

        if (this.last_char == '*') {
            this.last_char = this.getChar(this.pos, peek);
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
            this.last_char = this.getChar(this.pos, peek);
            identifier = "";
            while (!this.end && this.last_char != '\'') {
                if (this.last_char == '\\') {
                    this.last_char = this.getChar(this.pos, peek);
                }
                identifier = identifier + this.last_char;

                this.last_char = this.getChar(this.pos, peek);

                if (this.end && this.last_char != ')' && this.last_char != '\''){
                    identifier += this.last_char;

                }

                this.last_char = this.getChar(this.pos, peek);
                this.type = "string";
                this.lastType = "string";
                this.lastToken = identifier;
                if (peek) {
                    this.last_char = remembered_char;
                    System.out.println("Setting pos to remembered");
                    this.pos = remembered;
                    this.lastToken = rememberedlastToken;
                    this.type = rememberedType;
                }
                this.precedence = 7;
                return identifier;
            }
        }

        matcher = pattern.matcher(Character.toString(last_char));
        Pattern pattern2 = Pattern.compile("[a-zA-Z0-9_]+");
        if (matcher.find()) {
            identifier = "";

            while (!this.end && pattern2.matcher(Character.toString(this.last_char)).find()) {

                identifier = identifier + this.last_char;
                this.last_char = this.getChar(this.pos, peek);
            }

            if (this.end && this.last_char != ')') {
                identifier += this.last_char;
            }
            this.type = "identifier";
            this.lastType = "identifier";
            this.lastToken = identifier;
            if (peek) {
                this.last_char = remembered_char;
                System.out.println("Setting pos to remembered");
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            this.precedence = 1;
            return identifier;
        }

        if (this.last_char == '=') {
            this.last_char = this.getChar(this.pos, peek);
            this.type = "operator";
            this.lastType = "operator";
            this.lastToken = "eq";
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            this.precedence = 1;
            return lastToken;
        }

        if (this.last_char == '+') {
            this.last_char = this.getChar(this.pos, peek);
            this.type = "operator";
            this.lastType = "operator";
            this.lastToken = "plus";
            // BODMAS
            this.precedence = 5;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }

        if (this.last_char == '-') {
            this.last_char = this.getChar(this.pos, peek);
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
            this.last_char = this.getChar(this.pos, peek);
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
            this.last_char = this.getChar(this.pos, peek);
            lastToken = "comma";
            this.precedence = 7;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }

        if (this.last_char == '<') {
            this.last_char = this.getChar(this.pos, peek);
            this.type = "operator";
            this.lastType = "operator";
            this.lastToken = "lessthan";
            this.precedence = 2;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }
        if (this.last_char == '[') {
            this.last_char = this.getChar(this.pos, peek);
            this.lastToken = "opensquare";
            this.precedence = 7;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }
        if (this.last_char == ']') {
            this.last_char = this.getChar(this.pos, peek);
            this.lastToken = "closesquare";
            this.precedence = 7;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return this.lastToken;
        }
        if (this.last_char == '>') {
            this.last_char = this.getChar(this.pos, peek);
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
            this.last_char = this.getChar(this.pos, peek);
            lastToken = "property";
            this.precedence = 7;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return lastToken;
        }
        if (this.last_char == '{') {
            this.last_char = this.getChar(this.pos, peek);
            this.lastToken = "opencurly";
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
            this.last_char = this.getChar(this.pos, peek);
            this.lastToken = "closecurly";
            this.precedence = 7;
            if (peek) {
                this.last_char = remembered_char;
                this.pos = remembered;
                this.lastToken = rememberedlastToken;
                this.type = rememberedType;
            }
            return this.lastToken;
        }
        if (this.last_char == ';') {
            this.last_char = this.getChar(this.pos, peek);
            this.lastToken = "semicolon";
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
        String token = getToken(false);
        AST programAST = null;

        switch (token) {
            case "threads":
                programAST = new ProgramAST(getToken(false));
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
        System.out.println("nextast");
        String token = getToken(false);
        System.out.println(String.format("nextAst Token: %s", token));
        if (token == null) {
            return null;
        }
        switch (token) {
            case "lessthan":

                String start = getToken(false);
                if (!start.equals("start")) {
                    throw new IllegalArgumentException("Parse error, expected <start>");
                }
                String end = getToken(false);
                assert end.equals("greaterthan");
                System.out.println("Encountered start");
                return new StartProgramAST();
            case "function":
                return parseFunctionDeclaration();
            case "set":
                return parseVariableDeclaration();
            default:
                return parseMain(token);
        }

    }

    private AST parseVariableDeclaration() {
        String variableType = getToken(false);
        String variableName = getToken(false);
        String equals = getToken(false);
        if (!equals.equals("eq")) {
            throw new IllegalArgumentException("Expected variable declaration to have equals");
        }
        AST expression = parseExpression(0);

        return new VariableDeclarationAST(variableType, variableName, expression);
    }

    private AST parseExpression(int precedence) {
        System.out.println(String.format("Parsing expression %d", this.pos));

        String token = getToken(false);
        System.out.println(String.format("Expression token %s", token));
        if (token == null) {
            return null;
        }
        if (token.equals("semicolon")) {
            return null;
        }
        System.out.println(token);
        if (token.equals(null)) {
            return null;
        }
        switch (token) {
            case "opencurly":
                return parseStructure();
            case "opensquare":
                return parseList();
            default:
                // expression code
                AST left = new ExpressionAST(parseToken(token));
                System.out.println(String.format("left: %s %s", token, left));
                System.out.println(String.format("pos: %d", this.pos));
                int previousPos = this.pos;
                String peeked = getToken(true);
                int nextPos = this.pos;
                String peeked2 = getToken(true);
                // assert peeked.equals(peeked2);
                int lastPos = this.pos;
                assert (previousPos == nextPos) && previousPos == lastPos;
                System.out.println(String.format("pos: %d", this.pos));
                System.out.println(String.format("peeked: %s %d", peeked, this.precedence, precedence));
                String peekedType = this.lastType;
                while (this.precedence > precedence) {
                    System.out.println(String.format("%s.%d < %s.%d", peeked, this.precedence, token, precedence));
                    AST newLeft = parseExpression(7);
                    if (newLeft == null) {
                        System.out.println("new left was null");
                        break;
                    }
                    System.out.println(String.format("new left %s", newLeft));
                    String typeCheck = String.format("type %s", peekedType);
                    String behaviour = "";
                    System.out.println(typeCheck);
                    if (tokenHandling.containsKey(typeCheck)) {
                        behaviour = tokenHandling.get(typeCheck);
                        System.out.println(String.format("behaviour is %s", behaviour));
                    }
                    switch (behaviour) {
                        case "append":
                            System.out.println("Appending");
                            left.add(newLeft);
                            peeked = getToken(true);
                            peekedType = this.lastType;
                            break;
                        case "promote":
                            System.out.println(String.format("promoting %s", token));
                            if (newLeft != null) {
                                newLeft.add(left);
                                left = newLeft;
                                peeked = getToken(true);
                                peekedType = this.lastType;
                            } else {
                                break;
                            }
                        break;
                    }
                }
                System.out.println(String.format("left is %s", left));
                return left;
        }
    }

    private AST parseToken(String token) {
        AST ast = null;
        if (isNumber(token)) {
            ast = new LiteralNumberAST(token);
        }
        if (this.type.equals("string")) {
            ast = new LiteralStringAST(token);
        }
        if (this.type.equals("operator")) {
            ast = new OperatorAST(token);
        }
        if (this.type.equals("identifier")) {
            ast = new IdentifierAST(token);
        }
        return ast;
    }

    private boolean isNumber(String token) {
        Pattern pattern = Pattern.compile("[0-9.]+", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(token).find();
    }

    private AST parseList() {
        String token = getToken(false);
        Map<String, AST> ast = new HashMap<>();
        while (!token.equals("closesquare")) {
            String fieldName = getToken(false);
            AST fieldValue = parseExpression(0);
            ast.put(fieldName, fieldValue);
            token = getToken(false);
            if (token.equals("comma")) {
                token = getToken(false);
            }
        }
        return new StructureAST(ast);
    }

    private StructureAST parseStructure() {
        String token = getToken(false);
        if (token == null) {
            return null;
        }
        Map<String, AST> ast = new HashMap<>();
        while (token != null && !token.equals("closecurly")) {
            String fieldName = getToken(false);
            AST fieldValue = parseExpression(0);
            ast.put(fieldName, fieldValue);
            token = getToken(false);
        }
        return new StructureAST(ast);
    }

    private AST parseMain(String token) {

        System.out.println(String.format("parseMain Token: %s", token));
        while (token != null && !token.equals("closecurly")) {
            switch (token) {
                case "for":
                    System.out.println("Parsing loop");
                    token = getToken(false);
                    if (!token.equals("openbracket")) {
                        throw new IllegalArgumentException("Expected open bracket for loop beginning");
                    }
                    List<String> tokens = new ArrayList<>();
                    List<String> types = new ArrayList<>();
                    token = getToken(false);
                    while (token != null && !token.equals("semicolon")) {
                        tokens.add(token);
                        types.add(this.type);
                        token = getToken(false);


                    }
                    System.out.println("Parsing assignment of loop");
                    AST assignment = parseAssignmentStatement(tokens, types);
                    System.out.println(assignment);
                    if (!token.equals("semicolon")) {
                        throw new IllegalArgumentException("Expected semicolon after loop initializer");
                    }
                    System.out.println("Beginning parsing expression");
                    AST expression = parseExpression(0);
                    System.out.println(expression);
                    if (!lastToken.equals("semicolon")) {
                        throw new IllegalArgumentException(String.format("Expected semicolon in for loop postexpression, was %s", lastToken));
                    }
                    AST postexpression = parseExpression(0);
                    return new ForLoopAST(assignment, expression, postexpression);
            }
            token = getToken(false);
            System.out.println(String.format("parseMain Token: %s", token));

        }
        return null;
    }

    private AST parseFunctionDeclaration() {
        String functionName = getToken(false);
        String bracket = getToken(false);
        if (!bracket.equals("openbracket")) {
            throw new IllegalArgumentException("Parse error, expected bracket in function declaration beginning");
        }
        List<Argument> arguments = new ArrayList<>();

        String currentToken = getToken(false);
        while (!currentToken.equals("closebracket")) {
            System.out.println(String.format("Parsing function declaration: Token: %s", currentToken));
            String argumentType = currentToken;
            String argumentName = getToken(false);
            Argument e = new Argument(argumentName, argumentType);
            System.out.println(e);
            arguments.add(e);
            currentToken = getToken(false);
            if (currentToken.equals("comma")) {
                currentToken = getToken(false);
            }

        }


        String curly = getToken(false);
        System.out.println(String.format("CurlyToken: %s", curly));
        if (!curly.equals("opencurly")) {
            throw new IllegalArgumentException("Expected curly open for function declaration");
        }
        FunctionAST functionAST = new FunctionAST(functionName, arguments);
        currentToken = getToken(false);
        System.out.println(String.format("parseFunctionDeclaration Token: %s", currentToken));
        while ( currentToken != null && !currentToken.equals("closecurly")) {
            System.out.println("before parseFunctionDeclaration parseFunctionBodyItem");
            AST bodyItem = parseFunctionBodyItem(currentToken);
            functionAST.add(bodyItem);
            currentToken = getToken(false);

        }
        System.out.println(functionAST);
        return functionAST;
    }

    private AST parseFunctionBodyItem(String token) {
        List<String> tokens = new ArrayList<>();
        List<String> types = new ArrayList<>();
        tokens.add(token);
        types.add(this.type);
        System.out.println(String.format("FunctionBodyItem Token %s", token));
        token = getToken(false);
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
            token = getToken(false);
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

package ParserPackage;

import ParserPackage.ASTNodes.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


public class ASTBuilder {
    private static TokenHolder tokenHolder;
    private static HashMap<String, Integer> precedence;
    private static int functionPrecedence;
    private static boolean resolveImport;
    public static Node build(TokenHolder tokens, HashMap<String, Integer> precedence, int functionPrecedence) throws Exception {
        return build(tokens, precedence, functionPrecedence, false);
    }
    public static Node build(TokenHolder tokens, HashMap<String, Integer> precedence, int functionPrecedence, boolean resolveImport) throws Exception {
        return build(tokens, precedence, functionPrecedence, resolveImport, false, null);
    }
    public static Node build(TokenHolder tokens, HashMap<String, Integer> precedence, int functionPrecedence,
                                    boolean resolveImport, boolean executeImmediately, Environment environment) throws Exception {
        ASTBuilder.precedence = precedence;
        ASTBuilder.functionPrecedence = functionPrecedence;
        ASTBuilder.resolveImport = resolveImport;
        tokenHolder = tokens;
        if (!executeImmediately) {
            ProgramNode node = new ProgramNode();
            while (tokenHolder.hasNext()) {
                node.addNode(parseExpression());
                checkAndSkip("SEMICOLON");
            }
            return node;
        } else {
            Value returnValue = new Value(null);
            HashMap<String, Value> exports = Evaluator.EXPORTS;
            Evaluator.EXPORTS = new HashMap<>();
            while (tokenHolder.hasNext()) {
                Evaluator.evaluate(parseExpression(), environment);
            }
            returnValue.setProperties(Evaluator.EXPORTS);
            Evaluator.EXPORTS = exports;
            return new ValueNode(returnValue);
        }
    }
    private static Token checkAndSkip(String type) throws Exception {
        return checkToken(type) ? skipToken(type) : null;
    }
    private static boolean checkToken(String type) {
        Token token = null;
        try {
            token = tokenHolder.lookUp();
        } catch (Exception e) {
            return false;
        }
        return token != null && token.getName().equals(type);
    }
    private static Token skipToken(String type) throws Exception {
        if (checkToken(type)) {
            return tokenHolder.next();
        } else throw new Exception(type + " expected, got " + tokenHolder.lookUp().getName());
    }
    private static<T> Collection<T> delimited(String start, String separator, String stop, Supplier<T> parser) throws Exception {
        return delimited(start, separator, stop, parser, false);
    }
    private static<T> Collection<T> delimited(String start, String separator, String stop, Supplier<T> parser, boolean separatorOptional) throws Exception {
        Collection<T> nodes = new Collection<>();
        boolean first = true;
        if (start != null) skipToken(start);
        while (tokenHolder.hasNext()) {
            if (stop != null) {
                if (checkToken(stop)) break;
                if (first) {
                    first = false;
                } else {
                    if (separator != null) {
                        if (separatorOptional) {
                            checkAndSkip(separator);
                        } else skipToken(separator);
                    }
                }
                if (checkToken(stop)) break;
                nodes.add(parser.get());
            } else if (separator != null) {
                if (first) {
                    first = false;
                } else {
                    if (checkAndSkip(separator) == null) return nodes;
                }
                nodes.add(parser.get());
            }
        }
        if (stop != null) skipToken(stop);
        return nodes;
    }
    private static FunctionNode parseFunction() throws Exception {
        return parseFunction(true);
    }
    private static FunctionNode parseFunction(boolean identifierPossible) throws Exception {
        FunctionNode node = new FunctionNode();
        if (identifierPossible && checkToken("IDENTIFIER")) {
            node.setName(parseIdentifier());
        }
        node.setParameters(parseParameters());
        node.setBody(checkToken("LEFT_CURLY_PAREN") ? parseBody(true) : parseExpression());
        return node;
    }
    private static BodyNode parseBody() throws Exception {
        return parseBody(false);
    }
    private static BodyNode parseBody(boolean functionBody) throws Exception {
        BodyNode node;
        if (functionBody) {
            node = new BodyNode() {
                @Override
                public String getType() {
                    return "functionBody";
                }
            };
        } else {
            node = new BodyNode();
        }
        node.setExpressions(delimited("LEFT_CURLY_PAREN", null, "RIGHT_CURLY_PAREN", () -> {
            try {
                return parseExpression();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, true));
        return node;
    }
    private static UnaryNode parseUnary() throws Exception {
        UnaryNode unaryNode = new UnaryNode();
        unaryNode.setOperator(skipToken("OPERATOR").getValue());
        unaryNode.setValue(parseAtom());
        return unaryNode;
    }
    private static NewNode parseNew() throws Exception {
        skipToken("NEW");
        NewNode newNode = new NewNode();
        newNode.setClazz(parseNoCallAtom());
        newNode.setArguments(parseArguments());
        return newNode;
    }
    private static Collection<ParameterNode> parseParameters() throws Exception {
        return delimited("LEFT_PAREN", "COMMA", "RIGHT_PAREN", () -> {
            try {
                return parseParameter();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).to(ParameterNode.class);
    }
    private static ReturnNode parseReturn() throws Exception {
        skipToken("RETURN");
        return new ReturnNode(parseExpression());
    }
    private static ParameterNode parseParameter() throws Exception {
        ParameterNode node = new ParameterNode();
        node.setExpanded(checkAndSkip("EXPAND") != null);
        node.setName(parseIdentifier());
        if (checkToken("OPERATOR") && tokenHolder.lookUp().getValue().equals("=")) {
            skipToken("OPERATOR");
            node.setDefaultValue(parseExpression());
        }
        return node;
    }
    private static ImportNode parseImport() throws Exception {
        skipToken("IMPORT");
        ImportNode node = new ImportNode();
        if (checkAndSkip("EVERYTHING") == null) {
            Collection<Map.Entry<String, String>> collection = parseImports();
            for (Map.Entry<String, String> entry: collection) {
                node.put(entry.getKey(), entry.getValue());
            }
        }
        skipToken("FROM");
        node.setBuilt(checkAndSkip("BUILT") != null);
        node.setFilename(parseExpression());
        if (resolveImport) {
            if (node.getFilename().getType().equals("value")) {
                if (!node.isBuilt()) {
                    TokenHolder tokenHolder = ASTBuilder.tokenHolder;
                    HashMap<String, Integer> precedence = ASTBuilder.precedence;
                    int functionPrecedence = ASTBuilder.functionPrecedence;
                    boolean resolveImport = ASTBuilder.resolveImport;
                    Parser.compile(((ValueNode) node.getFilename()).getValue().toString());
                    node.setBuilt(true);
                    node.setFilename(new ValueNode(new Value(((ValueNode) node.getFilename()).getValue().toString() + ".build")));
                    ASTBuilder.tokenHolder = tokenHolder;
                    ASTBuilder.precedence = precedence;
                    ASTBuilder.functionPrecedence = functionPrecedence;
                    ASTBuilder.resolveImport = resolveImport;
                }
            } else {
                System.out.println("WARNING! Non-buildable library used while compiling");
            }
        }
        return node;
    }
    private static Collection<Map.Entry<String, String>> parseImports() throws Exception {
        return delimited(null, "COMMA", null, () -> {
            try {
                String name = parseIdentifier();
                String []pseudonym = new String[] {null};
                if (checkAndSkip("AS") != null) pseudonym[0] = parseIdentifier();
                return new Map.Entry<String, String>() {
                    @Override
                    public String getKey() {
                        return name;
                    }

                    @Override
                    public String getValue() {
                        return pseudonym[0] == null ? name : pseudonym[0];
                    }

                    @Override
                    public String setValue(String value) {
                        return pseudonym[0] == null ? name : pseudonym[0];
                    }
                };
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }
    private static Node parseExpression() throws Exception {
        return maybeCallOrIndex(() -> {
            try {
                return maybeBinary(parseAtom(), 0);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }
    private static ValueNode parseNumber() throws Exception {
        Token token = skipToken("NUMBER");
        return new ValueNode(
                new Value(
                        token.getValue().contains(".") ? Double.parseDouble(token.getValue()) : Integer.parseInt(token.getValue())
                )
        );
    }
    private static Node maybeCallOrIndex(Supplier<Node> parser) throws Exception {
        Node expr = parser.get();
        while (true) {
            if (checkToken("LEFT_PAREN")) {
                expr = parseCall(expr);
            } else if (checkToken("LEFT_SQUARE_PAREN")) {
                expr = parseIndex(expr);
            } else {
                return expr;
            }
        }
    }
    private static IndexNode parseIndex(Node value) throws Exception {
        IndexNode indexNode = new IndexNode(value);
        skipToken("LEFT_SQUARE_PAREN");
        indexNode.setBegin(parseAtom());
        Token operator = checkAndSkip("OPERATOR");
        if (operator != null && operator.getValue().equals("..")) {
            indexNode.setRange(true);
            if (!checkToken("RIGHT_SQUARE_PAREN")) {
                indexNode.setEnd(parseExpression());
            }
        }
        skipToken("RIGHT_SQUARE_PAREN");
        return indexNode;
    }
    private static Collection<Node> parseArguments() throws Exception {
        return delimited("LEFT_PAREN", "COMMA", "RIGHT_PAREN", () -> {
            try {
                return checkAndSkip("EXPAND") != null ? new ExpandNode(parseExpression()) : parseExpression();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }
    private static OperatorNode parseOperator() throws Exception {
        OperatorNode operatorNode = new OperatorNode();
        operatorNode.setBinary(checkToken("BINARY"));
        tokenHolder.next();
        skipToken("OPERATOR_KEYWORD");
        operatorNode.setOperator(skipToken("OPERATOR").getValue());
        if (checkAndSkip("PRECEDENCE") != null) {
            operatorNode.setPrecedence(parseNumber());
        }
        operatorNode.setFunction(parseFunction(false));
        if (operatorNode.isBinary()) {
            precedence.put(operatorNode.getOperator(), ((Number) ((ValueNode) operatorNode.getPrecedence()).getValue().getValue()).intValue());
        }
        return operatorNode;
    }
    private static CallNode parseCall(Node function) throws Exception {
        CallNode node = new CallNode();
        node.setFunction(function);
        node.setArguments(parseArguments());
        return node;
    }
    private static IfNode parseIf() throws Exception {
        IfNode node = new IfNode();
        skipToken("IF");
        skipToken("LEFT_PAREN");
        node.setCondition(parseExpression());
        skipToken("RIGHT_PAREN");
        if (checkToken("LEFT_CURLY_PAREN")) {
            node.setThen(parseBody());
        } else {
            node.setThen(parseExpression());
        }
        if(checkToken("ELSE")) {
            skipToken("ELSE");
            if (checkToken("LEFT_CURLY_PAREN")) {
                node.setOtherwise(parseBody());
            } else {
                node.setOtherwise(parseExpression());
            }
        }
        return node;
    }
    private static Node parseAtom() throws Exception {
        return parseAtom(0);
    }
    private static Node parseAtom(int precedence) throws Exception {
        if (precedence > functionPrecedence) {
            return parseNoCallAtom();
        } else {
            return maybeCallOrIndex(() -> {
                try {
                    return parseNoCallAtom();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            });
        }
    }
    private static Node parseNoCallAtom() throws Exception {
        return maybePropertied(() -> {
            try {
                if (checkToken("LEFT_PAREN")) {
                    Collection<Node> expressions = delimited("LEFT_PAREN", "COMMA", "RIGHT_PAREN", () -> {
                        try {
                            return parseExpression();
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    });
                    if (expressions.size() == 1) {
                        return new ParentedNode(expressions.get(0));
                    } else {
                        return new EnumerationNode(expressions);
                    }
                } else if (checkToken("CLASS")) {
                    return parseClass();
                } else if (checkToken("IMPORT")) {
                    return parseImport();
                } else if (checkToken("IF")) {
                    return parseIf();
                } else if (checkAndSkip("FUNCTION") != null) {
                    return parseFunction();
                } else if (checkToken("IDENTIFIER")) {
                    return parseVariable();
                } else if (checkToken("EXPORT")) {
                    return parseExport();
                } else if (checkToken("RETURN")) {
                    return parseReturn();
                } else if (checkToken("NUMBER")) {
                    return parseNumber();
                } else if (checkToken("STRING")) {
                    return parseString();
                } else if (checkToken("NEW")) {
                    return parseNew();
                } else if (checkAndSkip("INITIALIZER") != null) {
                    return parseBody(false);
                } else if (checkToken("LEFT_SQUARE_PAREN")) {
                    return parseArray();
                } else if (checkToken("SINGLE_LINE_COMMENT")) {
                    skipToken("SINGLE_LINE_COMMENT");
                    return new ValueNode(Value.NULL);
                } else if (checkToken("WHILE")) {
                    return parseWhile();
                } else if (checkToken("FOR")) {
                    return parseFor();
                }else if (checkToken("WHEN")){
                    return parseWhen();
                } else if (checkToken("OPERATOR")) {
                    return parseUnary();
                } else if (checkToken("BINARY") || checkToken("UNARY")) {
                    return parseOperator();
                } else throw new Exception("Unexpected token " + tokenHolder.lookUp());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private static Node parseWhen() throws Exception{
        WhenNode whenNode = new WhenNode();
        skipToken("WHEN");
        skipToken("LEFT_CURLY_PAREN");
        Collection<Node> conditions = new Collection<>();
        Collection<Node> bodies = new Collection<>();
        while (!checkToken("RIGHT_CURLY_PAREN")) {
            if (checkToken("ELSE")) {
                skipToken("ELSE");
                skipToken("COLON");
                whenNode.setOtherwise(parseExpression());
            } else {
                conditions.add(parseExpression());
                skipToken("COLON");
                if (checkToken("LEFT_CURLY")) {
                    bodies.add(parseBody());
                } else {
                    bodies.add(parseExpression());
                }
            }
        }
        skipToken("RIGHT_CURLY_PAREN");
        whenNode.setConditions(conditions);
        whenNode.setBodies(bodies);
        return whenNode;
    }

    private static ForNode parseFor() throws Exception {
        ForNode forNode = new ForNode();
        skipToken("FOR");
        skipToken("LEFT_PAREN");
        //for(IDENTIFIER in EXPRESSION){
        forNode.setName(parseIdentifier());
        skipToken("IN");
        forNode.setCollection(parseExpression());
        skipToken("RIGHT_PAREN");
        if (checkToken("LEFT_CURLY_PAREN")) {
            forNode.setBody(parseBody());
        } else {
            forNode.setBody(parseExpression());
        }
        return forNode;
    }
    private static WhileNode parseWhile() throws Exception {
        WhileNode whileNode = new WhileNode();
        skipToken("WHILE");
        skipToken("LEFT_PAREN");
        whileNode.setCondition(parseExpression());
        skipToken("RIGHT_PAREN");
        if (checkToken("LEFT_CURLY_PAREN")) {
            whileNode.setBody(parseBody());
        } else {
            whileNode.setBody(parseExpression());
        }
        return whileNode;
    }
    private static ArrayNode parseArray() throws Exception {
        ArrayNode arrayNode = new ArrayNode();
        arrayNode.setArray(delimited("LEFT_SQUARE_PAREN", "COMMA", "RIGHT_SQUARE_PAREN", () -> {
            try {
                return parseExpression();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }));
        return arrayNode;
    }
    private static ClassNode parseClass() throws Exception {
        skipToken("CLASS");
        ClassNode classNode = new ClassNode();
        if (checkToken("IDENTIFIER")) {
            classNode.setName(parseIdentifier());
        }
        Collection<Map.Entry<String, ClassFieldNode>> collection = delimited("LEFT_CURLY_PAREN", "SEMICOLON", "RIGHT_CURLY_PAREN", () -> {
            try {
                return parseClassField();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, true);
        HashMap<String, ClassFieldNode> map = new HashMap<>();
        for (Map.Entry<String, ClassFieldNode> entry: collection) {
            map.put(entry.getKey(), entry.getValue());
        }
        classNode.setFields(map);
        return classNode;
    }
    private static Map.Entry<String, ClassFieldNode> parseClassField() throws Exception {
        ClassFieldNode classFieldNode = new ClassFieldNode();
        if (checkToken("ACCESS_MODIFIER")) {
            classFieldNode.setGetModifier(AccessModifiers.forName(skipToken("ACCESS_MODIFIER").getValue()));
        }

        classFieldNode.setStatic(checkAndSkip("STATIC") != null);

        String name = parseIdentifier();
        if (checkToken("LEFT_PAREN")) {
            classFieldNode.setValue(parseFunction());
        }

        if (checkToken("OPERATOR") && skipToken("OPERATOR").getValue().equals("=")) {
            classFieldNode.setValue(parseExpression());
        }

        while (true) {
            if (checkToken("ACCESS_MODIFIER")) {
                Token token = skipToken("ACCESS_MODIFIER");
                if (checkAndSkip("GET") != null) {
                    classFieldNode.setGetModifier(AccessModifiers.forName(token.getValue()));
                    if (checkToken("LEFT_CURLY_PAREN")) {
                        classFieldNode.setGetAction(parseBody(true));
                    }
                } else if (checkAndSkip("SET") != null) {
                    classFieldNode.setSetModifier(AccessModifiers.forName(token.getValue()));
                    if (checkToken("LEFT_CURLY_PAREN")) {
                        classFieldNode.setSetAction(parseBody(true));
                    }
                } else throw new Exception("Expected get or set, got " + tokenHolder.lookUp().getName());
            } else break;
        }

        return new Map.Entry<String, ClassFieldNode>() {
            @Override
            public String getKey() {
                return name;
            }

            @Override
            public ClassFieldNode getValue() {
                return classFieldNode;
            }

            @Override
            public ClassFieldNode setValue(ClassFieldNode value) {
                return classFieldNode;
            }
        };
    }
    private static Node maybePropertied(Supplier<Node> lambda) throws Exception {
        Node node = new ValueNode(Value.NULL);
        if (!checkToken("LEFT_CURLY_PAREN")) node = lambda.get();
        if (checkToken("LEFT_CURLY_PAREN") || checkToken("OVERRIDE")) {
            return new PropertiedNode(checkAndSkip("OVERRIDE") != null, node, parseProperties());
        } else return node;
    }
    private static HashMap<String, Node> parseProperties() throws Exception {
        Collection<Map.Entry<String, Node>> properties =
                delimited("LEFT_CURLY_PAREN", "COMMA", "RIGHT_CURLY_PAREN", () -> {
                    try {
                        return parseProperty();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }, true);
        HashMap<String, Node> result = new HashMap<>();
        for (Map.Entry<String, Node> property: properties) {
            result.put(property.getKey(), property.getValue());
        }
        return result;
    }
    private static Map.Entry<String, Node> parseProperty() throws Exception {
        String name = parseIdentifier();
        skipToken("CAST_OPERATOR");
        Node value = parseExpression();
        return new Map.Entry<String, Node>() {
            @Override
            public String getKey() {
                return name;
            }

            @Override
            public Node getValue() {
                return value;
            }

            @Override
            public Node setValue(Node value1) {
                return value;
            }
        };
    }
    private static ExportNode parseExport() throws Exception {
        skipToken("EXPORT");
        Node expression = parseExpression();
        String as = null;
        if (checkToken("AS")) {
            skipToken("AS");
            as = parseIdentifier();
        }
        return new ExportNode(expression, as);
    }
    private static VariableNode parseVariable() throws Exception {
        return new VariableNode(parseIdentifier());
    }
    private static String parseIdentifier() throws Exception {
        return skipToken("IDENTIFIER").getValue();
    }
    private static ValueNode parseString() throws Exception {
        Token token = skipToken("STRING");
        return new ValueNode(
                new Value(
                        token.getValue()
                                .substring(1, token.getValue().length() - 1)
                                .replaceAll("\\\\n", "\n")
                                .replaceAll("\\\\r", "\r")
                                .replaceAll("(\\\\)(.)", "$2")
                )
        );
    }
    private static Node maybeBinary(Node left, int myPrecedence) throws Exception {
        if (nextOperator()) {
            Token operator = tokenHolder.lookUp();
            int hisPrecedence = precedence.get(operator.getValue());
            if (hisPrecedence > myPrecedence) {
                tokenHolder.next();
                Node atom = parseAtom(hisPrecedence);
                Node right = hisPrecedence > functionPrecedence ? maybeBinary(atom, hisPrecedence) :
                        maybeCallOrIndex(() -> {
                            try {
                                return maybeBinary(atom, hisPrecedence);
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        });
                BinaryNode node = new BinaryNode();
                node.setLeft(left);
                node.setRight(right);
                node.setOperator(operator.getValue());
                return maybeBinary(node, myPrecedence);
            } else return left;
        }
        return left;
    }
    private static boolean nextOperator() {
        return checkToken("OPERATOR");
    }
}

package ParserPackage;

import ParserPackage.ASTNodes.Node;
import ParserPackage.ASTNodes.ParentedNode;
import ParserPackage.ASTNodes.ProgramNode;
import sun.awt.Symbol;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser {
    private static final Collection<Rule> rules = new Collection<>(
            new Rule("LEFT_PAREN", Pattern.compile("\\(")),
            new Rule("RIGHT_PAREN", Pattern.compile("\\)")),
            new Rule("LEFT_CURLY_PAREN", Pattern.compile("\\{")),
            new Rule("LEFT_SQUARE_PAREN", Pattern.compile("\\[")),
            new Rule("RIGHT_SQUARE_PAREN", Pattern.compile("]")),
            new Rule("RIGHT_CURLY_PAREN", Pattern.compile("}")),
            new Rule("COMMA", Pattern.compile(",")),
            new Rule("SINGLE_LINE_COMMENT", Pattern.compile("//[^\n]*")),
            new Rule("NUMBER", Pattern.compile("\\d+(\\.\\d+)?")),
            new Rule("STRING", Pattern.compile("('([^']|(\\\\.))*')|(\"([^\"]|(\\\\.))*\")")),
            new Rule("SEMICOLON", Pattern.compile(";")),
            new Rule("FUNCTION", Pattern.compile("function")),
            new Rule("WHEN", Pattern.compile("when")),
            new Rule("NATIVE", Pattern.compile("native")),
            new Rule("EXPAND", Pattern.compile("\\.\\.\\.")),
            new Rule("COLON", Pattern.compile(":")),
            new Rule("EXPORT", Pattern.compile("export")),
            new Rule("WHILE", Pattern.compile("while")),
            new Rule("FOR", Pattern.compile("for")),
            new Rule("IN", Pattern.compile("in")),
            new Rule("AS", Pattern.compile("as")),
            new Rule("NEW", Pattern.compile("new")),
            new Rule("IF", Pattern.compile("if")),
            new Rule("ELSE", Pattern.compile("else")),
            new Rule("IMPORT", Pattern.compile("import")),
            new Rule("BUILT", Pattern.compile("built")),
            new Rule("CLASS", Pattern.compile("class")),
            new Rule("DEFAULT", Pattern.compile("default")),
            new Rule("GET", Pattern.compile("get")),
            new Rule("INITIALIZER", Pattern.compile("initializer")),
            new Rule("SWITCH", Pattern.compile("switch")),
            new Rule("CASE", Pattern.compile("case")),
            new Rule("SET", Pattern.compile("set")),
            new Rule("OVERRIDE", Pattern.compile("override")),
            new Rule("PRECEDENCE", Pattern.compile("precedence")),
            new Rule("OPERATOR_KEYWORD", Pattern.compile("operator")),
            new Rule("BINARY", Pattern.compile("binary")),
            new Rule("UNARY", Pattern.compile("unary")),
            new Rule("STATIC", Pattern.compile("static")),
            new Rule("FROM", Pattern.compile("from")),
            new Rule("EVERYTHING", Pattern.compile("everything")),
            new Rule("RETURN", Pattern.compile("return")),
            new Rule("MULTILINE_COMMENT", Pattern.compile("/\\*([^/])*\\*/"))
    );
    static {
        rules.add(new Rule("IDENTIFIER", Pattern.compile("[a-zA-Z$_][a-zA-Z&_0-9]*")));
    }
    private static Rule toSkip = new Rule("kar", Pattern.compile("\\s+"));
    public static void compile(String filename) throws Exception {
        compile(filename, filename + ".build", Environment.DEFAULT_ENVIRONMENT);
    }
    public static void compile(String filename, Environment environment) throws Exception {
        compile(filename, filename + ".build", environment);
    }
    public static void compile(String filename, String toFilename) throws Exception {
        compile(filename, toFilename, Environment.DEFAULT_ENVIRONMENT);
    }
    public static void compile(String filename, String toFilename, Environment environment) throws Exception {
        System.out.println("Reading file...");
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String code = new String(data, StandardCharsets.UTF_8);

        Rule operatorRule = new Rule("OPERATOR", new Collection<>());

        Set<String> set = Stream.concat(
                environment.getBinaryOperators().keySet().stream(),
                environment.getUnaryOperators().keySet().stream()).collect(Collectors.toSet());
        for (String name: new Collection<>(set.toArray())
                                .to(String.class)
                                .qsort((o1, o2) -> o2.length() - o1.length())) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Character character: name.toCharArray()) {
                stringBuilder.append("\\").append(character);
            }
            operatorRule.getPatterns().add(
                    Pattern.compile(stringBuilder.toString())
            );
        }

        Parser.rules.add(operatorRule);

        System.out.println("Lexing...");
        TokenHolder tokenHolder = new Lexer().lexFully(code, rules, toSkip);

        //System.out.println(tokenHolder);

        HashMap<String, Integer> precedence = new HashMap<>();
        for (Map.Entry<String, BinaryOperator> entry: environment.getBinaryOperators().entrySet()) {
            precedence.put(entry.getKey(), entry.getValue().getPrecedence());
        }
        System.out.println("Building AST...");
        Node program = ASTBuilder.build(tokenHolder, precedence, 100, true);
        //System.out.println(program);
        System.out.println("Writing to file...");
        FileOutputStream fileOut = new FileOutputStream(toFilename);
        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
        objectOut.writeObject(program);
        objectOut.close();
        System.out.println("Finished!");
    }
    public static Value run(String filename) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(filename);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

        ProgramNode program = (ProgramNode) objectInputStream.readObject();
        Value result = Evaluator.evaluate(program, Environment.DEFAULT_ENVIRONMENT);
        return result;
    }
    public static void kar() {}
    public static Value interpret(String filename) throws Exception {
        return interpret(filename, Environment.DEFAULT_ENVIRONMENT);
    }
    public static Value interpret(String filename, Environment environment) throws Exception {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String code = new String(data, StandardCharsets.UTF_8);

        Rule operatorRule = new Rule("OPERATOR", new Collection<>());

        Set<String> set = Stream.concat(
                environment.getBinaryOperators().keySet().stream(),
                environment.getUnaryOperators().keySet().stream()).collect(Collectors.toSet());
        for (String name: new Collection<>(set.toArray())
                            .to(String.class)
                            .qsort((o1, o2) -> o2.length() - o1.length())) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Character character: name.toCharArray()) {
                stringBuilder.append("\\").append(character);
            }
            operatorRule.getPatterns().add(
                    Pattern.compile(stringBuilder.toString())
            );
        }
        //System.out.println(operatorRule.getPatterns());
        Parser.rules.add(operatorRule);

        TokenHolder tokenHolder = new Lexer().lexFully(code, rules, toSkip);

        //System.out.println(tokenHolder);

        HashMap<String, Integer> precedence = new HashMap<>();
        for (Map.Entry<String, BinaryOperator> entry: environment.getBinaryOperators().entrySet()) {
            precedence.put(entry.getKey(), entry.getValue().getPrecedence());
        }
        Node program = ASTBuilder.build(tokenHolder, precedence, 100, false, true, environment);
        //System.out.println(program);

        Value result = Evaluator.evaluate(program, Environment.DEFAULT_ENVIRONMENT);
        //System.out.println("Properties: " + result.getProperties());
        return result;
    }
}

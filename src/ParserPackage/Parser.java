package ParserPackage;

import ParserPackage.ASTNodes.*;
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
            new KeywordRule("FUNCTION", "function"),
            new Rule("WHEN", Pattern.compile("when[^0-9A-Za-z_$]")),
            new Rule("NATIVE", Pattern.compile("native[^0-9A-Za-z_$]")),
            new Rule("EXPAND", Pattern.compile("\\.\\.\\.")),
            new Rule("COLON", Pattern.compile(":")),
            new Rule("EXPORT", Pattern.compile("export[^0-9A-Za-z_$]")),
            new Rule("WHILE", Pattern.compile("while[^0-9A-Za-z_$]")),
            new Rule("FOR", Pattern.compile("for[^0-9A-Za-z_$]")),
            new Rule("IN", Pattern.compile("in[^0-9A-Za-z_$]")),
            new Rule("AS", Pattern.compile("as[^0-9A-Za-z_$]")),
            new Rule("NEW", Pattern.compile("new[^0-9A-Za-z_$]")),
            new Rule("IF", Pattern.compile("if[^0-9A-Za-z_$]")),
            new Rule("ELSE", Pattern.compile("else[^0-9A-Za-z_$]")),
            new Rule("IMPORT", Pattern.compile("import[^0-9A-Za-z_$]")),
            new Rule("BUILT", Pattern.compile("built[^0-9A-Za-z_$]")),
            new Rule("CLASS", Pattern.compile("class[^0-9A-Za-z_$]")),
            new Rule("DEFAULT", Pattern.compile("default[^0-9A-Za-z_$]")),
            new Rule("GET", Pattern.compile("get[^0-9A-Za-z_$]")),
            new Rule("INITIALIZER", Pattern.compile("initializer[^0-9A-Za-z_$]")),
            new Rule("SWITCH", Pattern.compile("switch[^0-9A-Za-z_$]")),
            new Rule("CASE", Pattern.compile("case[^0-9A-Za-z_$]")),
            new Rule("SET", Pattern.compile("set[^0-9A-Za-z_$]")),
            new Rule("OVERRIDE", Pattern.compile("override[^0-9A-Za-z_$]")),
            new Rule("PRECEDENCE", Pattern.compile("precedence[^0-9A-Za-z_$]")),
            new Rule("OPERATOR_KEYWORD", Pattern.compile("operator[^0-9A-Za-z_$]")),
            new Rule("BINARY", Pattern.compile("binary[^0-9A-Za-z_$]")),
            new Rule("UNARY", Pattern.compile("unary[^0-9A-Za-z_$]")),
            new Rule("STATIC", Pattern.compile("static[^0-9A-Za-z_$]")),
            new Rule("FROM", Pattern.compile("from[^0-9A-Za-z_$]")),
            new Rule("EVERYTHING", Pattern.compile("everything[^0-9A-Za-z_$]")),
            new Rule("RETURN", Pattern.compile("return[^0-9A-Za-z_$]")),
            new Rule("MULTILINE_COMMENT", Pattern.compile("/\\*([^/])*\\*/"))
    );
    static {
        rules.add(new Rule("IDENTIFIER", Pattern.compile("[a-zA-Z$_][a-zA-Z&_0-9]*")));
    }
    public static Rule toSkip = new Rule("kar", Pattern.compile("\\s+"));
    public static void compile(String filename) throws Exception {
        compile(filename, filename + ".build", Environment.DEFAULT_ENVIRONMENT);
    }
    public static void compile(String filename, Environment environment) throws Exception {
        compile(filename, filename + ".build", environment);
    }
    public static void compile(String filename, String toFilename) throws Exception {
        compile(filename, toFilename, Environment.DEFAULT_ENVIRONMENT);
    }
    public static Collection<Rule> getRules(Environment environment) {
        Collection<Rule> rules = Parser.rules;
        if (environment != null) {
            Rule operatorRule = new Rule("OPERATOR", new Collection<>());
            Set<String> set = Stream.concat(
                    environment.getBinaryOperators().keySet().stream(),
                    environment.getUnaryOperators().keySet().stream()).collect(Collectors.toSet());
            for (String name : new Collection<>(set.toArray())
                    .to(String.class)
                    .qsort((o1, o2) -> o2.length() - o1.length())) {
                StringBuilder stringBuilder = new StringBuilder();
                for (Character character : name.toCharArray()) {
                    stringBuilder.append("\\").append(character);
                }
                operatorRule.getPatterns().add(
                        Pattern.compile(stringBuilder.toString())
                );
            }

            rules.add(operatorRule);
        }
        return rules;
    }
    public static void compile(String filename, String toFilename, Environment environment) throws Exception {
        System.out.println("Reading file...");
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String code = new String(data, StandardCharsets.UTF_8);

        Collection<Rule> rules = getRules(environment);

        System.out.println("Lexing...");
        TokenHolder tokenHolder = new Lexer().lexFully(code, rules, toSkip);

        //System.out.println(tokenHolder);

        HashMap<String, Integer> precedence = new HashMap<>();
        for (Map.Entry<String, BinaryOperator> entry: environment.getBinaryOperators().entrySet()) {
            precedence.put(entry.getKey(), entry.getValue().getPrecedence());
        }
        System.out.println("Building AST...");
        Node program = new ASTBuilder().build(tokenHolder, precedence, 100, true);
        System.out.println(program);
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

        Collection<Rule> rules = getRules(environment);

        TokenHolder tokenHolder = new Lexer().lexFully(code, rules, toSkip);

        //System.out.println(tokenHolder);

        HashMap<String, Integer> precedence = new HashMap<>();
        for (Map.Entry<String, BinaryOperator> entry: environment.getBinaryOperators().entrySet()) {
            precedence.put(entry.getKey(), entry.getValue().getPrecedence());
        }
        Node program = new ASTBuilder().build(tokenHolder, precedence, 100, false, true, environment);
        //System.out.println(program);

        Value result = Evaluator.evaluate(program, Environment.DEFAULT_ENVIRONMENT);
        //System.out.println("Properties: " + result.getProperties());
        return result;
    }
}

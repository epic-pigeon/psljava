package ParserPackage;

import ParserPackage.ASTNodes.Node;
import ParserPackage.ASTNodes.ParentedNode;
import ParserPackage.ASTNodes.ProgramNode;
import sun.awt.Symbol;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Parser {
    private static final Collection<Rule> rules = new Collection<>(
            new Rule("LEFT_PAREN", Pattern.compile("\\(")),
            new Rule("RIGHT_PAREN", Pattern.compile("\\)")),
            new Rule("LEFT_CURLY_PAREN", Pattern.compile("\\{")),
            new Rule("LEFT_SQUARE_PAREN", Pattern.compile("\\[")),
            new Rule("RIGHT_SQUARE_PAREN", Pattern.compile("]")),
            new Rule("RIGHT_CURLY_PAREN", Pattern.compile("}")),
            new Rule("COMMA", Pattern.compile(",")),
            new Rule("PROPERTY_OPERATOR", Pattern.compile("\\.")),
            new Rule("ASSIGN", Pattern.compile("=")),
            new Rule("CAST_OPERATOR", Pattern.compile(":")),
            new Rule("NUMBER", Pattern.compile("\\d+(\\.\\d+)?")),
            new Rule("STRING", Pattern.compile("('([^']|(\\\\.))*')|(\"([^\"]|(\\\\.))*\")")),
            new Rule("SEMICOLON", Pattern.compile(";")),
            new Rule("FUNCTION", Pattern.compile("function")),
            new Rule("EXPORT", Pattern.compile("export")),
            new Rule("AS", Pattern.compile("as")),
            new Rule("NEW", Pattern.compile("new")),
            new Rule("IF", Pattern.compile("if")),
            new Rule("ELSE", Pattern.compile("else")),
            new Rule("IMPORT", Pattern.compile("import")),
            new Rule("BUILT", Pattern.compile("built")),
            new Rule("CLASS", Pattern.compile("class")),
            new Rule("GET", Pattern.compile("get")),
            new Rule("SET", Pattern.compile("set")),
            new Rule("OVERRIDE", Pattern.compile("override")),
            new Rule("STATIC", Pattern.compile("static")),
            new Rule("FROM", Pattern.compile("from")),
            new Rule("TO", Pattern.compile("to")),
            new Rule("END", Pattern.compile("end")),
            new Rule("EVERYTHING", Pattern.compile("everything")),
            new Rule("RETURN", Pattern.compile("return")),
            new Rule("MULTILINE_COMMENT", Pattern.compile("/\\*([^/])*\\*/"))
    );
    static {
        Rule rule = new Rule("ACCESS_MODIFIER", new Collection<>());
        for (AccessModifiers accessModifier: AccessModifiers.values()) {
            rule.addPattern(Pattern.compile(accessModifier.getName()));
        }
        rules.add(rule);
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

        for (String name: environment.getBinaryOperators().keySet()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Character character: name.toCharArray()) {
                stringBuilder.append("\\").append(character);
            }
            operatorRule.getPatterns().add(
                    Pattern.compile(stringBuilder.toString())
            );
        }

        Collection<Rule> rules = new Collection<>(operatorRule);
        rules.addAll(Parser.rules);

        System.out.println("Lexing...");
        TokenHolder tokenHolder = new Lexer().lexFully(code, rules, toSkip);

        //System.out.println(tokenHolder);

        HashMap<String, Integer> precedence = new HashMap<>();
        for (Map.Entry<String, BinaryOperator> entry: environment.getBinaryOperators().entrySet()) {
            precedence.put(entry.getKey(), entry.getValue().getPrecedence());
        }
        System.out.println("Building AST...");
        Node program = ASTBuilder.build(tokenHolder, precedence, 100, true);

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

        for (String name: environment.getBinaryOperators().keySet()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Character character: name.toCharArray()) {
                stringBuilder.append("\\").append(character);
            }
            operatorRule.getPatterns().add(
                    Pattern.compile(stringBuilder.toString())
            );
        }
        //System.out.println(operatorRule.getPatterns());
        Collection<Rule> rules = new Collection<>(operatorRule);
        rules.addAll(Parser.rules);

        TokenHolder tokenHolder = new Lexer().lex(code, rules, toSkip);

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

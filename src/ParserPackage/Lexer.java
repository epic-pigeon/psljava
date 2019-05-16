package ParserPackage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private Collection<Token> tokens;
    private String code;
    private Collection<Rule> rules;
    private Rule toSkip;
    private int position;

    public TokenHolder lexFully(String code, Collection<Rule> rules, Rule toSkip) throws Exception {
        position = 0;
        tokens = new Collection<>();
        this.code = code;
        Collection<Rule> newRules = new Collection<>(new Rule("__LEXER_SETTINGS", Pattern.compile("/<([^/<>]|(\\\\.))*>/")));
        newRules.addAll(rules);
        rules = newRules;
        this.rules = rules;
        this.toSkip = toSkip;
        while (position < code.length()) {
            readNext();
        }
        return new TokenHolder(tokens);
    }

    public TokenHolder lex(String code, Collection<Rule> rules, Rule toSkip) throws LexingException {
        position = 0;
        tokens = new Collection<>();
        this.code = code;
        this.rules = rules;
        this.toSkip = toSkip;
        /*while (position < code.length()) {
            for (Pattern pattern : toSkip.getPatterns()) {
                Matcher skipMatcher = pattern.matcher(code.substring(position));
                if (skipMatcher.find() && skipMatcher.start() == 0) {
                    position += skipMatcher.end();
                    break;
                }
            }
            if (position >= code.length()) break;

            try {
                for (Rule rule : rules) {
                    for (Pattern pattern : rule.getPatterns()) {
                        Matcher matcher = pattern.matcher(code.substring(position));
                        if (matcher.find() && matcher.start() == 0) {
                            tokens.add(new Token(rule.getName(), matcher.group(), position, matcher.group().split("\\r?\\r").length - 1));
                            position += matcher.group().length();
                            throw new ContinueException();
                        }
                    }
                }
            } catch (ContinueException ignored) {
                continue;
            }
            throw new LexingException(tokens, position);
        }*/

        return new TokenHolder(null) {
            @Override
            public Collection<Token> getTokens() {
                return tokens;
            }

            private Token getByPosition(int pos) throws Exception {
                while (pos >= tokens.size()) {
                    readNext();
                }
                return tokens.get(pos);
            }

            @Override
            public boolean hasNext() throws Exception {
                try {
                    getByPosition(this.position);
                    return true;
                } catch (LexingException e) {
                    return false;
                }
            }

            @Override
            public Token next() throws Exception {
                try {
                    return getByPosition(this.position++);
                } catch (LexingException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public Token lookUp() throws Exception {
                try {
                    return hasNext() ? getByPosition(position) : null;
                } catch (LexingException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public Iterator<Token> iterator() {
                return tokens.iterator();
            }
        };
    }

    private Token readNext() throws Exception {
        for (Pattern pattern : toSkip.getPatterns()) {
            Matcher skipMatcher = pattern.matcher(code.substring(position));
            if (skipMatcher.find() && skipMatcher.start() == 0) {
                position += skipMatcher.group().length();
                break;
            }
        }
        if (position >= code.length()) return null;

        for (Rule rule : rules) {
            for (Pattern pattern : rule.getPatterns()) {
                Matcher matcher = pattern.matcher(code.substring(position));
                if (matcher.find() && matcher.start() == 0) {
                    position += matcher.group().length();
                    if (rule.getName().equals("__LEXER_SETTINGS")) {
                        this.rules = parseLexerSettings(matcher.group().substring(2, matcher.group().length() - 2).replaceAll("(\\\\)([/<>])", "$2"), rules);
                        return readNext();
                    } else if (!rule.getName().endsWith("COMMENT")) {
                        Token token = new Token(rule.getName(), matcher.group(), position, matcher.group().split("\\r?\\n").length - 1);
                        tokens.add(token);
                        return token;
                    } else return readNext();
                }
            }
        }

        throw new LexingException(tokens, position);
    }
    private Collection<Rule> parseLexerSettings(String s, Collection<Rule> oldRules) throws Exception {
        Collection<Rule> newRules = new Collection<>();
        for (Rule rule: oldRules) newRules.add(new Rule(rule.getPatterns(), rule.getName()));
        Rule toSkip = new Rule("\\s+");
        Collection<Rule> rules = new Collection<>(
                new Rule("COMMAND_DEFINE", "define"),
                new Rule("COMMAND_REDEFINE", "redefine"),
                new Rule("COMMAND_ADD", "add"),
                new Rule("COMMAND_DELETE", "delete"),
                new Rule("COMMAND_PRINT", "print"),
                new Rule("COMMAND_LOAD", "load"),
                new Rule("VALUE_IDENTIFIER", "[A-Z0-9_]+"),
                new Rule("VALUE_REGEX", "\\`([^`\\\\]|(\\\\[`\\\\]))*\\`"),
                new Rule("VALUE_STRING", "\\'[^']*\\'")
        );
        TokenHolder tokens = new Lexer().lexFully(s, rules, toSkip);
        Iterator<Token> iterator = tokens.iterator();
        while (iterator.hasNext()) {
            Token token = iterator.next();
            if (token.getName().startsWith("COMMAND_")) {
                if (token.getName().equals("COMMAND_DEFINE")) {
                    Token name = iterator.next();
                    assert name.getName().equals("VALUE_IDENTIFIER");
                    newRules.add(new Rule(new Collection<>(), name.getValue()));
                } else if (token.getName().equals("COMMAND_REDEFINE")) {
                    Token ruleToken = iterator.next();
                    assert ruleToken.getName().equals("VALUE_IDENTIFIER");
                    Rule rule = newRules.findFirst(rule1 -> ruleToken.getValue().equals(rule1.getName()));
                    if (rule != null)
                        rule.setPatterns(new Collection<>());
                    else
                        newRules.add(new Rule(new Collection<>(), ruleToken.getValue()));
                } else if (token.getName().equals("COMMAND_ADD")) {
                    Token ruleToken = iterator.next();
                    Token regexToken = iterator.next();
                    assert ruleToken.getName().equals("VALUE_IDENTIFIER");
                    assert regexToken.getName().equals("VALUE_REGEX");
                    Rule rule = newRules.findFirst(rule1 -> ruleToken.getValue().equals(rule1.getName()));
                    rule.addPattern(parseRegex(regexToken.getValue()));
                } else if (token.getName().equals("COMMAND_DELETE")) {
                    Token ruleToken = iterator.next();
                    assert ruleToken.getName().equals("VALUE_IDENTIFIER");
                    Rule rule = newRules.findFirst(rule1 -> ruleToken.getValue().equals(rule1.getName()));
                    if (rule != null) newRules.remove(rule);
                } else if (token.getName().equals("COMMAND_PRINT")) {
                    Token ruleToken = iterator.next();
                    assert ruleToken.getName().equals("VALUE_IDENTIFIER");
                    Rule rule = newRules.findFirst(rule1 -> ruleToken.getValue().equals(rule1.getName()));
                    System.out.println(rule);
                } else if (token.getName().equals("COMMAND_LOAD")) {
                    Token fileToken = iterator.next();
                    assert fileToken.getName().equals("VALUE_STRING");
                    String filename = parseString(fileToken.getValue());
                    File file = new File(filename);
                    byte[] data = new byte[(int) file.length()];
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        fis.read(data);
                        fis.close();
                    } catch (IOException ignored) {}
                    String code = new String(data, StandardCharsets.UTF_8);
                    try {
                        newRules = parseLexerSettings(code, newRules);
                    } catch (LexingException e) {
                        throw new LexingException(tokens.getTokens(), fileToken.getPosition());
                    }
                }
            }
        }
        return newRules;
    }
    private static Pattern parseRegex(String s) {
        s = s.replaceAll("(\\\\)([`\\\\])", "$2");
        return Pattern.compile(s.substring(1, s.length() - 1));
    }
    private static String parseString(String s) {
        return s.substring(1, s.length() - 1);
    }
}

class ContinueException extends Exception {
    ContinueException() {
        super("Continuation exception, if you see it, then something fucked up");
    }
}
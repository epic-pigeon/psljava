package ParserPackage;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private Collection<Token> tokens;
    private String code;
    private Collection<Rule> rules;
    private Rule toSkip;
    private int position;

    public TokenHolder lexFully(String code, Collection<Rule> rules, Rule toSkip) throws LexingException {
        position = 0;
        tokens = new Collection<>();
        this.code = code;
        this.rules = rules;
        this.toSkip = toSkip;
        while (position < code.length()) {
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

            private Token getByPosition(int pos) throws LexingException {
                while (pos >= tokens.size()) {
                    readNext();
                }
                return tokens.get(pos);
            }

            @Override
            public boolean hasNext() {
                try {
                    getByPosition(this.position);
                    return true;
                } catch (LexingException e) {
                    return false;
                }
            }

            @Override
            public Token next() {
                try {
                    return getByPosition(this.position++);
                } catch (LexingException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public Token lookUp() {
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

    private Token readNext() throws LexingException {
        for (Pattern pattern : toSkip.getPatterns()) {
            Matcher skipMatcher = pattern.matcher(code.substring(position));
            if (skipMatcher.find() && skipMatcher.start() == 0) {
                position += skipMatcher.end();
                break;
            }
        }
        if (position >= code.length()) throw new LexingException(tokens, position);

        for (Rule rule : rules) {
            for (Pattern pattern : rule.getPatterns()) {
                Matcher matcher = pattern.matcher(code.substring(position));
                if (matcher.find() && matcher.start() == 0) {
                    Token token = new Token(rule.getName(), matcher.group(), position, matcher.group().split("\\r?\\n").length - 1);
                    tokens.add(token);
                    position += matcher.group().length();
                    return token;
                }
            }
        }

        throw new LexingException(tokens, position);
    }
}

class ContinueException extends Exception {
    ContinueException() {
        super("Continuation exception, if you see it, then something fucked up");
    }
}
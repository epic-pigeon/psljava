package ParserPackage;

import java.util.Iterator;

public class TokenHolder {
    private Collection<Token> tokens;
    protected int position;

    public TokenHolder(Collection<Token> tokens) {
        this.tokens = tokens;
        position = 0;
    }

    public Collection<Token> getTokens() {
        return tokens;
    }

    public void setTokens(Collection<Token> tokens) {
        this.tokens = tokens;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("[\n    ");
        for (int i = 0; i < tokens.size(); i++) {
            if (i != 0) stringBuilder.append(",\n    ");
            stringBuilder.append(tokens.get(i).toString());
        }
        stringBuilder.append("\n]");
        return stringBuilder.toString();
    }

    public boolean hasNext() {
        return tokens.size() > position;
    }

    public Token next() {
        return tokens.get(position++);
    }

    public Token lookUp() {
        return hasNext() ? tokens.get(position) : null;
    }

    public Iterator<Token> iterator() {
        return tokens.iterator();
    }
}

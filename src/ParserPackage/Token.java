package ParserPackage;

public class Token {
    private String name;
    private String value;
    private int position;

    public int getNewLines() {
        return newLines;
    }

    public void setNewLines(int newLines) {
        this.newLines = newLines;
    }

    private int newLines;

    public Token(String name, String value, int position, int newLines) {
        this.name = name;
        this.value = value;
        this.position = position;
        this.newLines = newLines;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String toString() {
        return name + " '" + value.replaceAll("\\r?\\n", "<new line>") + "' on " + position;
    }
}

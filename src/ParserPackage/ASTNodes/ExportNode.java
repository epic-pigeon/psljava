package ParserPackage.ASTNodes;

public class ExportNode extends Node {
    private Node value;
    private String as;
    @Override
    public String getType() {
        return "export";
    }

    public Node getValue() {
        return value;
    }

    public void setValue(Node value) {
        this.value = value;
    }

    public ExportNode(Node value, String as) {
        this.value = value;
        this.as = as;
    }

    public String getAs() {
        return as;
    }

    public void setAs(String as) {
        this.as = as;
    }
}

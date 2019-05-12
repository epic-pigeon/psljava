package ParserPackage.ASTNodes;

public class NewNode extends Node {
    private Node call;
    @Override
    public String getType() {
        return "new";
    }

    public NewNode(Node call) {
        this.call = call;
    }

    public void setCall(Node call) {
        this.call = call;
    }

    public Node getCall() {
        return call;
    }
}

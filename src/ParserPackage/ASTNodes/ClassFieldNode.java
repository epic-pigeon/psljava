package ParserPackage.ASTNodes;

import ParserPackage.AccessModifiers;
import ParserPackage.Value;

public class ClassFieldNode extends Node {
    private Node value;
    private Node getAction;
    private AccessModifiers getModifier = AccessModifiers.PUBLIC;
    private Node setAction;
    private AccessModifiers setModifier = AccessModifiers.DISABLED;
    private boolean isStatic;

    @Override
    public String getType() {
        return "field";
    }

    public Node getValue() {
        return value;
    }

    public void setValue(Node value) {
        this.value = value;
    }

    public Node getGetAction() {
        return getAction;
    }

    public void setGetAction(Node getAction) {
        this.getAction = getAction;
    }

    public AccessModifiers getGetModifier() {
        return getModifier;
    }

    public void setGetModifier(AccessModifiers getModifier) {
        this.getModifier = getModifier;
    }

    public Node getSetAction() {
        return setAction;
    }

    public void setSetAction(Node setAction) {
        this.setAction = setAction;
    }

    public AccessModifiers getSetModifier() {
        return setModifier;
    }

    public void setSetModifier(AccessModifiers setModifier) {
        this.setModifier = setModifier;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }
}

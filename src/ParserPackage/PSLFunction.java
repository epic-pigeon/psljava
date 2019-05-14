package ParserPackage;

public abstract class PSLFunction {
    abstract public Value apply(Collection<Value> t) throws Exception;
    public String toString() {
        return "[native function]";
    }
}

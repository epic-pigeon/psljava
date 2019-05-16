package ParserPackage;

import java.io.Serializable;

public abstract class PSLFunction implements Serializable {
    abstract public Value apply(Collection<Value> t) throws Exception;
    public String toString() {
        return "[native function]";
    }
}

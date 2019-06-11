import ParserPackage.Collection;
import ParserPackage.PSLFunction;
import ParserPackage.Value;

import java.util.HashMap;

public class MathLibBuilder extends LibBuilder {
    public static void main(String[] args) throws Exception {

        Value math = new Value();

        math.put("e", new Value(Math.E));
        math.put("pi", new Value(Math.PI));
        math.put("sin", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(Math.sin(((Number) t.get(0).getValue()).doubleValue()));
                    }
                }
        ));
        math.put("cos", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(Math.cos(((Number) t.get(0).getValue()).doubleValue()));
                    }
                }
        ));
        math.put("tan", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(Math.tan(((Number) t.get(0).getValue()).doubleValue()));
                    }
                }
        ));
        math.put("ceil", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(Math.ceil(((Number) t.get(0).getValue()).doubleValue()));
                    }
                }
        ));
        math.put("round", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(Math.round(((Number) t.get(0).getValue()).doubleValue()));
                    }
                }
        ));
        math.put("arcsin", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(Math.asin(((Number) t.get(0).getValue()).doubleValue()));
                    }
                }
        ));
        math.put("arccos", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(Math.acos(((Number) t.get(0).getValue()).doubleValue()));
                    }
                }
        ));
        math.put("arctan", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(Math.atan(((Number) t.get(0).getValue()).doubleValue()));
                    }
                }
        ));
        math.put("log10", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(Math.log10(((Number) t.get(0).getValue()).doubleValue()));
                    }
                }
        ));
        math.put("ln", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(Math.log(((Number) t.get(0).getValue()).doubleValue()));
                    }
                }
        ));
        math.put("log", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(
                                Math.log(((Number) t.get(1).getValue()).doubleValue()) /
                                        Math.log(((Number) t.get(0).getValue()).doubleValue())
                        );
                    }
                }
        ));
        math.put("floor", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(Math.floor(((Number) t.get(0).getValue()).doubleValue()));
                    }
                }
        ));

        HashMap<String, Value> exports = new HashMap<>();
        exports.put("math", math);
        build("lib/math", exports);
    }
}

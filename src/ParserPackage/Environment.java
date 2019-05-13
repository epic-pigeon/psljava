package ParserPackage;

import ParserPackage.ASTNodes.BinaryNode;
import ParserPackage.ASTNodes.Node;
import ParserPackage.ASTNodes.VariableNode;

import java.util.HashMap;
import java.util.function.Function;

public class Environment {
    private HashMap<String, Variable> variables = new HashMap<>();
    private HashMap<String, BinaryOperator> binaryOperators = new HashMap<>();
    private Value thiz = Value.NULL;
    private Environment parent;
    public Environment(Environment parent) {
        this.parent = parent;
        variables.putAll(parent != null ? parent.variables : new HashMap<>());
        binaryOperators.putAll(parent != null ? parent.binaryOperators : new HashMap<>());
        if (parent != null) thiz = parent.thiz;
    }
    public Environment extend() {
        return new Environment(this);
    }
    public Environment lookupVariable(String name) {
        Environment environment = this;
        while (environment != null) {
            if (environment.variables.containsKey(name)) return environment;
            environment = environment.parent;
        }
        return null;
    }
    public Variable defVariable(String name, Value value) {
        Variable variable = new Variable(value);
        variables.put(name, variable);
        return variable;
    }
    public Variable getVariable(String name) {
        return variables.getOrDefault(name, null);
    }
    public Variable setVariable(String name, Value value) {
        Environment environment = lookupVariable(name);
        if (environment == null && parent != null) return null;
        if (getVariable(name) != null) {
            getVariable(name).setValue(value);
        } else {
            defVariable(name, value);
        }
        return getVariable(name);
    }
    public Variable deleteVariable(String name) {
        return variables.remove(name);
    }

    public BinaryOperator defBinaryOperator(String name, BinaryOperator operator) {
        return binaryOperators.put(name, operator);
    }

    public Environment lookUpBinaryOperator(String name) {
        Environment environment = this;
        while (environment != null) {
            if (environment.binaryOperators.containsKey(name)) return environment;
            environment = environment.parent;
        }
        return null;
    }
    public BinaryOperator getBinaryOperator(String name) {
        return binaryOperators.getOrDefault(name, null);
    }

    public BinaryOperator setBinaryOperator(String name, BinaryOperator operator) {
        Environment environment = lookUpBinaryOperator(name);
        if (environment == null && parent != null) return null;
        if (getBinaryOperator(name) != null) {
            binaryOperators.put(name, operator);
        } else {
            defBinaryOperator(name, operator);
        }
        return getBinaryOperator(name);
    }

    public HashMap<String, Variable> getVariables() {
        return variables;
    }

    public void setVariables(HashMap<String, Variable> variables) {
        this.variables = variables;
    }

    public HashMap<String, BinaryOperator> getBinaryOperators() {
        return binaryOperators;
    }

    public void setBinaryOperators(HashMap<String, BinaryOperator> binaryOperators) {
        this.binaryOperators = binaryOperators;
    }

    public static final Environment DEFAULT_ENVIRONMENT = new Environment(null);

    static {
        DEFAULT_ENVIRONMENT.defBinaryOperator("+", new BinaryOperator(
                (node1, node2, environment) -> {
                    Value v1 = Evaluator.evaluate(node1, environment);
                    Value v2 = Evaluator.evaluate(node2, environment);
                    Object value1 = v1.getValue();
                    Object value2 = v2.getValue();
                    Value result;
                    if (value1.getClass() == Double.class) {
                        if (value2.getClass() == Double.class) {
                            result = new Value(((Double) value1) + ((Double) value2));
                        } else {
                            result = new Value(String.valueOf(value1) + value2);
                        }
                    } else {
                        result = new Value(String.valueOf(value1) + value2);
                    }
                    result.putAll(v1.getProperties());
                    result.putAll(v2.getProperties());
                    return result;
                }, 20
        ));
        DEFAULT_ENVIRONMENT.defBinaryOperator("=", new BinaryOperator(
                (node1, node2, environment) -> {
                    Value v1 = Evaluator.evaluate(node1, environment);
                    Value v2 = Evaluator.evaluate(node2, environment);
                    if (v1.isSettable()) {
                        ((SettableValue) v1).set(v2);
                        if (v2.isSettable()) {
                            return new SettableValue(v2) {
                                @Override
                                public Value set(Value value) throws Exception {
                                    ((SettableValue) v1).set(value);
                                    return ((SettableValue) v2).set(value);
                                }

                                @Override
                                public Value setProp(String name, Value value) throws Exception {
                                    ((SettableValue) v1).setProp(name, value);
                                    return ((SettableValue) v2).setProp(name, value);
                                }
                            };
                        } else {
                            return ((SettableValue) v1).set(v2);
                        }
                    } else throw new Exception(v1 + " is not an lvalue");
                }, 10
        ));
        DEFAULT_ENVIRONMENT.defBinaryOperator(".", new BinaryOperator(
                (node1, node2, environment) -> {
                    Value value2 = Evaluator.evaluate(node1, environment);
                    String prop;
                    if (node2.getType().equals("variable")) {
                        prop = ((VariableNode) node2).getValue();
                    } else {
                        prop = Evaluator.evaluate(node2, environment).toString();
                    }
                    if (value2.isSettable()) {
                        if (value2.get(prop) == null) value2.put(prop, Value.NULL);
                        return new SettableValue(value2.get(prop)) {
                            @Override
                            public Value set(Value value1) throws Exception {
                                return value2.put(prop, value1);
                            }

                            @Override
                            public Value setProp(String name, Value value1) throws Exception {
                                return value2.get(prop).put(name, value1);
                            }
                        };
                    } else return value2.get(prop);
                }, 200
        ));
        Value system = new Value(null);
        Value stdout = new Value(null);
        try {
            stdout.put("println", new Value(
                    new PSLFunction() {
                        @Override
                        public Value apply(Value thiz, Collection<Value> args, Environment environment) throws Exception {
                            for (Value arg : args) {
                                System.out.println(arg);
                            }
                            return Value.NULL;
                        }
                    }
            ));
            stdout.put("print", new Value(
                    new PSLFunction() {
                        @Override
                        public Value apply(Value thiz, Collection<Value> args, Environment environment) throws Exception {
                            for (Value arg : args) {
                                System.out.print(arg);
                            }
                            return Value.NULL;
                        }
                    }
            ));
            system.put("stdout", stdout);
        } catch (Exception ignored) {}
        DEFAULT_ENVIRONMENT.defVariable("system", system);
        DEFAULT_ENVIRONMENT.defVariable("null", Value.NULL);
    }

    public Value getThiz() {
        return thiz;
    }

    public void setThiz(Value thiz) {
        this.thiz = thiz;
    }
}
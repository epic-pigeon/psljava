package ParserPackage;

import ParserPackage.ASTNodes.*;
import jdk.nashorn.internal.codegen.CompilerConstants;

import java.beans.EventSetDescriptor;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Evaluator {
    private static HashMap<String, Value> EXPORTS = new HashMap<>();
    public static Value evaluate(Node node, Environment environment) throws Exception {
        switch (node.getType()) {
            case "program":
                Value returnValue = new Value(null);
                for (Node node1 : ((ProgramNode) node).getProgram()) {
                    Evaluator.evaluate(node1, environment);
                }
                returnValue.setProperties(EXPORTS);
                EXPORTS = new HashMap<>();
                return returnValue;
            case "value":
                return ((ValueNode) node).getValue();
            case "function":
                Value func = makeFunction((FunctionNode) node, environment);
                if (((FunctionNode) node).getName() != null) {
                    environment.setVariable(((FunctionNode) node).getName(), func);
                }
                return func;
            case "call":
                return evalCall(node, environment);
            case "variable":
                String name = ((VariableNode) node).getValue();
                if (name.equals("global")) {
                    Value value = new SettableValue(Value.NULL) {
                        @Override
                        public Value set(Value value) throws Exception {
                            return this;
                        }

                        @Override
                        public Value setProp(String name, Value value) throws Exception {
                            return null;
                        }
                    };
                    for (Map.Entry<String, Variable> entry : environment.getVariables().entrySet()) {
                        value.put(entry.getKey(), new SettableValue(entry.getValue().getValue()) {
                            @Override
                            public Value set(Value value) throws Exception {
                                return ((SettableValue)value).setProp(name, value);
                            }

                            @Override
                            public Value setProp(String name1, Value value) throws Exception {
                                Variable variable =  environment.getVariable(name);
                                if (variable.getValue() == null) variable.setValue(Value.NULL);
                                return variable.put(name1, value);
                            }
                        });
                    }
                    return value;
                }
                Variable variable = environment.getVariable(name);
                if (variable != null) {
                    return new SettableValue(variable.getValue()) {
                        @Override
                        public Value set(Value value) {
                            variable.setValue(value);
                            return value;
                        }

                        @Override
                        public Value setProp(String name, Value value) throws Exception {
                            variable.put(name, value);
                            return value;
                        }
                    };
                } else {
                    return new SettableValue(Value.NULL) {
                        @Override
                        public Value set(Value value) {
                            return environment.setVariable(((VariableNode) node).getValue(), value).getValue();
                        }

                        @Override
                        public Value setProp(String name, Value value) throws Exception {
                            throw new Exception(((VariableNode) node).getValue() + " is not defined");
                        }
                    };
                }
            case "export":
                Node exportedNode = ((ExportNode) node).getValue();
                String as = ((ExportNode) node).getAs();
                Value evaluated = Evaluator.evaluate(exportedNode, environment);
                if (as == null) {
                    try {
                        Field field = exportedNode.getClass().getDeclaredField("name");
                        field.setAccessible(true);
                        if (field.get(exportedNode) != null) {
                            EXPORTS.put((String) field.get(exportedNode), evaluated);
                        } else throw new NoSuchFieldException();
                    } catch (NoSuchFieldException e) {
                        throw new Exception("Bad export value");
                    }
                } else {
                    EXPORTS.put(as, evaluated);
                }
                return evaluated;
            case "binary":
                BinaryOperator operator = environment.getBinaryOperator(((BinaryNode) node).getOperator());
                return operator.getAction().apply(
                        ((BinaryNode) node).getLeft(),
                        ((BinaryNode) node).getRight(),
                        environment
                );
            case "functionBody":
                for (Node node1: ((BodyNode) node).getExpressions()) try {
                    Evaluator.evaluate(node1, environment);
                } catch (ReturnException e) {
                    return e.getReturnValue();
                }
                return Value.NULL;
            case "body":
                Collection<Value> result = new Collection<>();
                for (Node node1: ((BodyNode) node).getExpressions()) {
                    result.add(Evaluator.evaluate(node1, environment));
                }
                return new Value(result);
            case "return":
                throw new ReturnException(evaluate(((ReturnNode) node).getValue(), environment));
            case "enumeration":
                Collection<Value> values = ((EnumerationNode) node).getNodes().map(node1 -> {
                    try {
                        return evaluate(node1, environment);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
                return new SettableValue(new Value(values)) {
                    @Override
                    public Value set(Value value) throws Exception {
                        Value lastValue = null;
                        for (Value value1: values) {
                            try {
                                lastValue = ((SettableValue) value1).set(value);
                            } catch (ClassCastException e) {
                                throw new Exception(value1 + " is not an lvalue");
                            }
                        }
                        return lastValue;
                    }

                    @Override
                    public Value setProp(String name, Value value) throws Exception {
                        Value lastValue = null;
                        for (Value value1: values) {
                            try {
                                lastValue = ((SettableValue) value1).setProp(name, value);
                            } catch (ClassCastException e) {
                                throw new Exception(value1 + " is not an lvalue");
                            }
                        }
                        return lastValue;
                    }
                };
            case "import":
                ImportNode importNode = (ImportNode) node;
                Value exports = null;
                String filename = Evaluator.evaluate(importNode.getFilename(), environment).getValue().toString();
                if (!(new File(filename + ".build").exists() || importNode.isBuilt())) {
                    Value filenameValue = Evaluator.evaluate(importNode.getFilename(), environment);
                    if (String.class.isAssignableFrom(filenameValue.getValue().getClass())) {
                        exports = Parser.interpret(filename, environment);
                    } else throw new Exception("String expected after FROM");
                } else {
                    exports = Parser.run(filename + (importNode.isBuilt() ? "" : ".build"));
                }
                if (!importNode.isEmpty()) for (Map.Entry<String, String> entry: importNode.entrySet()) {
                    if (exports.get(entry.getKey()) != null) {
                        environment.setVariable(entry.getValue(), exports.get(entry.getKey()));
                    } else throw new Exception("Bad import name: " + entry.getKey());
                } else for (Map.Entry<String, Value> entry: exports.getProperties().entrySet()) {
                    environment.setVariable(entry.getKey(), entry.getValue());
                }
                return exports;
            case "parented":
                return Evaluator.evaluate(((ParentedNode) node).getNode(), environment);
            case "propertied":
                PropertiedNode propertiedNode = (PropertiedNode) node;
                Value val = Evaluator.evaluate(propertiedNode.getNode(), environment);
                if (propertiedNode.isOverride()) {
                    HashMap<String, Value> props = new HashMap<>();
                    for (Map.Entry<String, Node> entry: propertiedNode.getProperties().entrySet()) {
                        props.put(entry.getKey(), Evaluator.evaluate(entry.getValue(), environment));
                    }
                    val.setProperties(props);
                } else {
                    for (Map.Entry<String, Node> entry: propertiedNode.getProperties().entrySet()) {
                        val.put(entry.getKey(), Evaluator.evaluate(entry.getValue(), environment));
                    }
                }
                return val;
            case "new":
                NewNode newNode = (NewNode) node;
                System.out.println(environment.getVariables());
                return ((PSLClass) Evaluator.evaluate(newNode.getClazz(), environment)).instantiate(newNode.getArguments().map(node1 -> {
                    try {
                        return Evaluator.evaluate(node1, environment);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }));
            case "class":
                PSLClass pslClass = new PSLClass(environment);
                ClassNode classNode = (ClassNode) node;
                HashMap<String, PSLClassField> statics = new HashMap<>();
                HashMap<String, PSLClassField> prototype = new HashMap<>();
                for (Map.Entry<String, ClassFieldNode> entry: classNode.getFields().entrySet()) {
                    if (entry.getValue().isStatic()) {
                        statics.put(entry.getKey(), (PSLClassField) Evaluator.evaluate(entry.getValue(), pslClass.getScope()));
                    } else {
                        prototype.put(entry.getKey(), (PSLClassField) Evaluator.evaluate(entry.getValue(), pslClass.getScope()));
                    }
                }
                pslClass.setPrototype(prototype);
                pslClass.setStatics(statics);
                return pslClass;
            case "field":
                PSLClassField pslClassField = new PSLClassField();
                ClassFieldNode classFieldNode = (ClassFieldNode) node;
                if (classFieldNode.getGetAction() != null)
                    pslClassField.setOnGet((PSLFunction) Evaluator.evaluate(classFieldNode.getGetAction(), environment).getValue());
                if (classFieldNode.getSetAction() != null)
                    pslClassField.setOnSet((PSLFunction) Evaluator.evaluate(classFieldNode.getSetAction(), environment).getValue());
                pslClassField.setGetModifier(classFieldNode.getGetModifier());
                pslClassField.setSetModifier(classFieldNode.getSetModifier());
                if (classFieldNode.getValue() != null)
                    pslClassField.setValue(Evaluator.evaluate(classFieldNode.getValue(), environment));
                return pslClassField;
            default: throw new Exception("Don't know how to evaluate " + node.getType());
        }
    }
    private static Value makeFunction(FunctionNode node, Environment environment) {
        Value value = new Value(null);
        PSLFunction function = new PSLFunction() {
            @Override
            public Value apply(Value thiz, Collection<Value> arguments, Environment environment1) throws Exception {
                Environment scope = environment.extend();
                Collection<ParameterNode> parameters = node.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    ParameterNode parameter = parameters.get(i);
                    Value argument1;
                    try {
                        argument1 = arguments.get(i);
                    } catch (IndexOutOfBoundsException e) {
                        argument1 = parameter.getDefaultValue() != null ?
                                Evaluator.evaluate(parameter.getDefaultValue(), environment)
                                : Value.NULL;
                    }
                    final Value argument = argument1;
                    if (argument != null) {
                        if (scope.getVariable(parameter.getName()) != null) scope.deleteVariable(parameter.getName());
                        scope.defVariable(parameter.getName(), argument);
                    }
                }
                scope.defVariable("this", thiz);
                return Evaluator.evaluate(node.getBody(), scope);
            }

            @Override
            public String toString() {
                return "[PSL function]";
            }
        };
        value.setValue(function);
        return value;
    }
    private static Value evalCall(Node node, Environment environment) throws Exception {
        PSLFunction function = (PSLFunction) (Evaluator.evaluate(((CallNode) node).getFunction(), environment).getValue());
        if (function == null) throw new Exception("Undefined function: " + ((CallNode) node).getFunction());
        Collection<Value> arguments = ((CallNode) node).getArguments().map(node1 -> {
            try {
                return Evaluator.evaluate(node1, environment);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
        return function.apply(environment.getThiz(), arguments, environment);
    }
    private static Exception croak(String reason) {
        Exception throwable = new Exception(reason);
        throwable.printStackTrace();
        return throwable;
    }
}

abstract class PSLFunction {
    abstract public Value apply(Value thiz, Collection<Value> t, Environment environment) throws Exception;
    public String toString() {
        return "[native function]";
    }
}

class PSLClass extends Value {
    private Environment scope;
    private HashMap<String, PSLClassField> prototype;
    private HashMap<String, PSLClassField> statics;
    public PSLClass(Environment environment) {
        super(null);
        scope = environment.extend();
        scope.setThiz(this);
    }

    @Override
    public Value get(String key) throws Exception {
        return statics.get(key).get(this, scope);
    }

    @Override
    public Value put(String key, Value value) throws Exception {
        return statics.get(key).set(this, value, scope);
    }

    public Environment getScope() {
        return scope;
    }

    public void setScope(Environment scope) {
        this.scope = scope;
    }

    public HashMap<String, PSLClassField> getPrototype() {
        return prototype;
    }

    public void setPrototype(HashMap<String, PSLClassField> prototype) {
        this.prototype = prototype;
    }

    public HashMap<String, PSLClassField> getStatics() {
        return statics;
    }

    public void setStatics(HashMap<String, PSLClassField> statics) {
        this.statics = statics;
        for (Map.Entry<String, PSLClassField> entry: statics.entrySet()) {
            try {
                this.properties.put(entry.getKey(), entry.getValue().get(this, scope));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Value instantiate(Collection<Value> args) throws Exception {
        Value result = new Value(null);
        PSLClassField constructor = prototype.get("constructor");
        ((PSLFunction) constructor.getValue().getValue()).apply(result, args, scope);
        return result;
    }
}

class PSLClassField extends Value {
    public PSLClassField() {
        super(null);
    }
    private Value value;
    private PSLFunction onGet;
    private AccessModifiers getModifier;
    private PSLFunction onSet;
    private AccessModifiers setModifier;
    public Value get(Value clazz, Environment scope) throws Exception {
        if (getModifier == AccessModifiers.PUBLIC) {
            if (onGet == null) {
                return value;
            } else {
                return onGet.apply(clazz, new Collection<>(), scope);
            }
        } else throw new Exception("Getting is " + getModifier);
    }
    public Value set(Value clazz, Value val, Environment scope) throws Exception {
        if (setModifier == AccessModifiers.PUBLIC) {
            if (onSet == null) {
                return value = val;
            } else {
                return onSet.apply(clazz, new Collection<>(val), scope);
            }
        } else throw new Exception("Setting is " + setModifier);
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public PSLFunction getOnGet() {
        return onGet;
    }

    public void setOnGet(PSLFunction onGet) {
        this.onGet = onGet;
    }

    public AccessModifiers getGetModifier() {
        return getModifier;
    }

    public void setGetModifier(AccessModifiers getModifier) {
        this.getModifier = getModifier;
    }

    public PSLFunction getOnSet() {
        return onSet;
    }

    public void setOnSet(PSLFunction onSet) {
        this.onSet = onSet;
    }

    public AccessModifiers getSetModifier() {
        return setModifier;
    }

    public void setSetModifier(AccessModifiers setModifier) {
        this.setModifier = setModifier;
    }
}

abstract class SettableValue extends Value {
    @Override
    public boolean isSettable() {
        return true;
    }

    public SettableValue(Object value, HashMap<String, Value> properties) {
        super(value, properties);
    }
    public SettableValue(Object value) {
        super(value);
    }
    public SettableValue(Value value) {
        super(value.getValue(), value.getProperties());
    }
    abstract public Value set(Value value) throws Exception;
    abstract public Value setProp(String name, Value value) throws Exception;
}

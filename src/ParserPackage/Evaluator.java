package ParserPackage;

import ParserPackage.ASTNodes.*;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class Evaluator {
    public static HashMap<String, Value> EXPORTS = new HashMap<>();
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
                            return environment.setVariable(name, value).getValue();
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
                    Value val = variable.getValue();
                    return new SettableValue(val) {
                        @Override
                        public Value get(String key) throws Exception {
                            return val.get(key);
                        }

                        @Override
                        public Value put(String key, Value value) throws Exception {
                            return val.put(key, value);
                        }

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
                String as = ((ExportNode) node).getAlias();
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
                Value classValue = Evaluator.evaluate(newNode.getClazz(), environment);
                if (classValue.getClass().equals(PSLClass.class)) {
                    return ((PSLClass) classValue).instantiate(newNode.getArguments().map(node1 -> {
                        try {
                            return Evaluator.evaluate(node1, environment);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }));
                } else return ((PSLClass) ((SettableValue) classValue).getRealValue()).instantiate(newNode.getArguments().map(node1 -> {
                    try {
                        return Evaluator.evaluate(node1, environment);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }));
            case "class":
                PSLClass pslClass = new PSLClass(environment);
                //System.out.println(pslClass.getScope().getThiz());
                ClassNode classNode = (ClassNode) node;
                HashMap<String, PSLClassField> statics = new HashMap<>();
                HashMap<String, ClassFieldNode> prototype = new HashMap<>();
                for (Map.Entry<String, ClassFieldNode> entry: classNode.getFields().entrySet()) {
                    if (entry.getValue().isStatic()) {
                        statics.put(entry.getKey(), (PSLClassField) Evaluator.evaluate(entry.getValue(), pslClass.getScope()));
                    } else {
                        prototype.put(entry.getKey(), entry.getValue());
                    }
                }
                pslClass.setPrototype(prototype);
                pslClass.setStatics(statics);
                return pslClass;
            case "field":
                PSLClassField pslClassField = new PSLClassField();
                ClassFieldNode classFieldNode = (ClassFieldNode) node;
                if (classFieldNode.getGetAction() != null) {
                    FunctionNode functionNode = new FunctionNode();
                    ParameterNode parameterNode = new ParameterNode();
                    parameterNode.setName("value");
                    functionNode.setParameters(new Collection<>(parameterNode));
                    functionNode.setBody(classFieldNode.getGetAction());
                    pslClassField.setOnGet((PSLFunction) Evaluator.evaluate(functionNode, environment).getValue());
                }
                if (classFieldNode.getSetAction() != null) {
                    FunctionNode functionNode = new FunctionNode();
                    functionNode.setBody(classFieldNode.getSetAction());
                    ParameterNode parameterNode0 = new ParameterNode();
                    parameterNode0.setName("currentValue");
                    ParameterNode parameterNode1 = new ParameterNode();
                    parameterNode1.setName("value");
                    functionNode.setParameters(new Collection<>(parameterNode0, parameterNode1));
                    pslClassField.setOnSet((PSLFunction) Evaluator.evaluate(functionNode, environment).getValue());
                }
                pslClassField.setGetModifier(classFieldNode.getGetModifier());
                pslClassField.setSetModifier(classFieldNode.getSetModifier());
                if (classFieldNode.getValue() != null)
                    pslClassField.setDefaultValue(Evaluator.evaluate(classFieldNode.getValue(), environment));
                return pslClassField;
            case "if":
                IfNode ifNode = (IfNode) node;
                if (toBoolean(Evaluator.evaluate(ifNode.getCondition(), environment))) {
                    return Evaluator.evaluate(ifNode.getThen(), environment);
                } else {
                    if (ifNode.getOtherwise() != null) {
                        return Evaluator.evaluate(ifNode.getOtherwise(), environment);
                    } else return Value.NULL;
                }
            case "array":
                Collection<Value> array = new Collection<>();
                for (Node node1 : ((ArrayNode) node).getArray()) {
                    array.add(Evaluator.evaluate(node1, environment));
                }
                return new Value(array);
            case "index":
                IndexNode indexNode = (IndexNode) node;
                Value value = Evaluator.evaluate(indexNode.getValue(), environment);
                if (value.getValue().getClass() == Collection.class) {
                    Collection<Value> arr = (Collection<Value>) value.getValue();
                    if (indexNode.isRange()) {
                        Value start = Evaluator.evaluate(indexNode.getBegin(), environment);
                        if (start.getValue().getClass() == Double.class) {
                            int begin = ((Double) start.getValue()).intValue();
                            if (indexNode.getEnd() == null) {
                                return new Value(arr.slice(begin));
                            } else {
                                Value finish = Evaluator.evaluate(indexNode.getEnd(), environment);
                                if (finish.getValue().getClass() == Double.class) {
                                    int end = ((Double) finish.getValue()).intValue();
                                    return new Value(arr.slice(begin, end));
                                }
                            }
                        } else {
                            throw new Exception("Index value should be an integer");
                        }
                    } else {
                        Value index = Evaluator.evaluate(indexNode.getBegin(), environment);
                        if (index.getValue().getClass() == Double.class) {
                            return arr.get(((Double) index.getValue()).intValue());
                        } else {
                            throw new Exception("Index value should be an integer");
                        }
                    }
                } else throw new Exception("Bad value for index");
            default: throw new Exception("Don't know how to evaluate " + node.getType());
        }
    }
    private static Value makeFunction(FunctionNode node, Environment environment) {
        Value value = new Value(null);
        PSLFunction function = new PSLFunction() {
            @Override
            public Value apply(Collection<Value> arguments) throws Exception {
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
                scope.defVariable("this", scope.getThiz());
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
        Value functionValue = Evaluator.evaluate(((CallNode) node).getFunction(), environment);
        PSLFunction function = (PSLFunction) (functionValue.getValue());
        if (function == null) throw new Exception("Undefined function: " + ((CallNode) node).getFunction());
        Collection<Value> arguments = ((CallNode) node).getArguments().map(node1 -> {
            try {
                return Evaluator.evaluate(node1, environment);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
        return function.apply(arguments);
    }
    private static Exception croak(String reason) {
        Exception throwable = new Exception(reason);
        throwable.printStackTrace();
        return throwable;
    }
    private static boolean toBoolean(Value value) {
        Object val = value.getValue();
        if (val.getClass() == String.class) {
            return ((String) val).length() > 0;
        } else if (val.getClass() == Double.class) {
            return ((Double) val) != 0;
        } else if (val.getClass() == Boolean.class) {
            return (Boolean) val;
        } else return value != null;
    }
}


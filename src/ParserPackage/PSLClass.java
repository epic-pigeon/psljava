package ParserPackage;

import ParserPackage.ASTNodes.ClassFieldNode;

import java.util.HashMap;
import java.util.Map;

public class PSLClass extends Value {
    private Environment scope;
    private HashMap<String, ClassFieldNode> prototype;
    private HashMap<String, PSLClassField> statics;
    public PSLClass(Environment environment) {
        super(null);
        scope = environment.extend();
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

    public HashMap<String, PSLClassField> getStatics() {
        return statics;
    }

    public void setStatics(HashMap<String, PSLClassField> statics) {
        this.statics = statics;
    }

    public Value instantiate(Collection<Value> args) throws Exception {
        Value result = new Value(null) {
            @Override
            public Value get(String key) throws Exception {
                Value value = super.get(key);
                if (value == null) return null;
                if (value.getClass().equals(PSLClassField.class)) {
                    if ((((PSLClassField) value).getGetModifier().stricter(AccessModifiers.PUBLIC)))
                        throw new Exception("Get access is " + ((PSLClassField) value).getGetModifier());
                    if (((PSLClassField) value).getOnGet() != null) {
                        return ((PSLClassField) value).getOnGet().apply(new Collection<>(value));
                    }
                }
                return value;
            }

            @Override
            public Value put(String key, Value setValue) throws Exception {
                Value value = super.get(key);
                if (value != null) {
                    if (value.getClass().equals(PSLClassField.class)) {
                        if ((((PSLClassField) value).getSetModifier().stricter(AccessModifiers.PUBLIC))) {
                            throw new Exception("Set access is " + ((PSLClassField) value).getSetModifier());
                        }
                        if (((PSLClassField) value).getOnSet() != null) {
                            return ((PSLClassField) value).getOnSet().apply(new Collection<>(value, setValue));
                        }
                    }
                }
                return super.put(key, setValue);
            }
        };
        scope.setThiz(result);
        for (Map.Entry<String, ClassFieldNode> entry: prototype.entrySet()) {
            Value value = Evaluator.evaluate(entry.getValue(), scope);
            Value defaultValue = ((PSLClassField) value).getDefaultValue();
            value.setValue(defaultValue == null ? Value.NULL : defaultValue.getValue());
            result.put(entry.getKey(), value);
        }
        if (prototype.get("constructor") != null) {
            PSLClassField constructor = (PSLClassField) Evaluator.evaluate(prototype.get("constructor"), scope);
            ((PSLFunction) constructor.getDefaultValue().getValue()).apply(args);
        }
        return result;
    }

    public HashMap<String, ClassFieldNode> getPrototype() {
        return prototype;
    }

    public void setPrototype(HashMap<String, ClassFieldNode> prototype) {
        this.prototype = prototype;
    }

    @Override
    public HashMap<String, Value> getProperties() {
        this.properties = new HashMap<>();
        for (Map.Entry<String, PSLClassField> entry: statics.entrySet()) {
            try {
                this.properties.put(entry.getKey(), entry.getValue().get(this, scope));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return properties;
    }
}
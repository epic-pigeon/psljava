package ParserPackage;

public class PSLClassField extends Value {
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
                return onGet.apply(new Collection<>());
            }
        } else throw new Exception("Getting is " + getModifier);
    }
    public Value set(Value clazz, Value val, Environment scope) throws Exception {
        if (setModifier == AccessModifiers.PUBLIC) {
            if (onSet == null) {
                return value = val;
            } else {
                return onSet.apply(new Collection<>(val));
            }
        } else throw new Exception("Setting is " + setModifier);
    }

    @Override
    public Object getValue() {
        return super.value;
    }

    @Override
    public void setValue(Object value) {
        super.value = value;
    }

    public Value getDefaultValue() {
        return this.value;
    }

    public void setDefaultValue(Value value) {
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

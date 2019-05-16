package ParserPackage;

public class ThisValue extends Value {
    public Value get(String key, Environment environment) throws Exception {
        Value value = super.get(key);
        if (value == null) return null;
        if (value.getClass().equals(PSLClassField.class)) {
            if (environment.getThiz().equals(this)) {
                if ((((PSLClassField) value).getGetModifier().stricter(AccessModifiers.PRIVATE))) {
                    throw new Exception("Get access is " + ((PSLClassField) value).getGetModifier());
                }
            } else {
                if ((((PSLClassField) value).getGetModifier().stricter(AccessModifiers.PUBLIC))) {
                    throw new Exception("Get access is " + ((PSLClassField) value).getGetModifier());
                }
            }
            if (((PSLClassField) value).getOnGet() != null) {
                return ((PSLClassField) value).getOnGet().apply(new Collection<>(value));
            }
        }
        return value;
    }

    public Value put(String key, Value setValue, Environment environment) throws Exception {
        Value value = super.get(key);
        if (value != null) {
            if (value.getClass().equals(PSLClassField.class)) {
                if (environment.getThiz().equals(this)) {
                    if ((((PSLClassField) value).getSetModifier().stricter(AccessModifiers.PRIVATE))) {
                        throw new Exception("Set access is " + ((PSLClassField) value).getSetModifier());
                    }
                } else {
                    if ((((PSLClassField) value).getSetModifier().stricter(AccessModifiers.PUBLIC))) {
                        throw new Exception("Set access is " + ((PSLClassField) value).getSetModifier());
                    }
                }
                if (((PSLClassField) value).getOnSet() != null) {
                    return ((PSLClassField) value).getOnSet().apply(new Collection<>(value, setValue));
                }
            }
        }
        return super.put(key, setValue);
    }

    @Override
    public Value get(String key) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value put(String key, Value value) throws Exception {
        throw new UnsupportedOperationException();
    }
}

package ParserPackage;

public enum AccessModifiers {
    PUBLIC("public", 0),
    PROTECTED("protected", 1),
    PRIVATE("private", 2),
    DISABLED("disabled", 3);

    private String name;
    private int strictness;
    AccessModifiers(String name, int strictness) {
        this.name = name;
        this.strictness = strictness;
    }

    public String getName() {
        return name;
    }

    public static AccessModifiers forName(String name) {
        for (AccessModifiers accessModifier: AccessModifiers.values()) {
            if (accessModifier.getName().equals(name)) return accessModifier;
        }
        return null;
    }

    public boolean stricter(AccessModifiers accessModifier) {
        return this.strictness > accessModifier.strictness;
    }
}

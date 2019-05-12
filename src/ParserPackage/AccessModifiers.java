package ParserPackage;

public enum AccessModifiers {
    PRIVATE("private"),
    PUBLIC("public"),
    DISABLED("disabled"),
    PROTECTED("protected");

    private String name;
    AccessModifiers(String name) {
        this.name = name;
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
}

public class PSLNativeLib {
    private static String __NAME__ = "lib";
    public static Object object = new Object() {
        public int value = 2;
    };
    public static String string = "kar string";
    public static void sayKar() {
        System.out.println("kar");
    }
    private static class Kar {
        static String kar = "ne kar";
    }
    public static Integer sum(Integer i1, Integer i2) {
        return i1 + i2;
    }
    public PSLNativeLib() {
        System.out.println("kar");
    }
    public int kar = 1;
}

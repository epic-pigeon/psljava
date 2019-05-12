import ParserPackage.Parser;

public class Main {
    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "interpret":
                set();
                Parser.interpret(args[1]);
                print("Interpretation");
                return;
            case "compile":
                set();
                Parser.compile(args[1], args.length > 2 ? args[2] : args[1] + ".build");
                print("Compilation");
                return;
            case "run":
                set();
                Parser.run(args[1]);
                print("Runtime");
                return;
            default:
                System.out.println("wait what the fuck");
        }
        //Parser.kar();
        //set();
        //Parser.compile("program.psl", "program");
        //print("Compilation");
        //set();
        //Parser.run("program");
        //print("Runtime");
        //set();
        //Parser.interpret("program.psl");
        //print("Interpretation");
    }
    private static long t;
    private static void set() {
        t = System.nanoTime();
    }
    private static void print(String tag) {
        System.out.println(tag + ": " + ((int)(((double) (System.nanoTime() - t)) / 1000000)));
    }
}

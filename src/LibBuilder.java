import ParserPackage.ASTNodes.ExportNode;
import ParserPackage.ASTNodes.ProgramNode;
import ParserPackage.ASTNodes.ValueNode;
import ParserPackage.Collection;
import ParserPackage.PSLFunction;
import ParserPackage.Value;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public class LibBuilder {
    public static void main(String[] args) throws Exception {
        Value vorona = new Value("vorona blyat");
        vorona.put("kar", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        System.out.println("KAAAR blyat!!");
                        return Value.NULL;
                    }
                }
        ));
        vorona.put("debil", new Value(true));
        HashMap<String, Value> exports = new HashMap<>();
        exports.put("vorona", vorona);
        build("lib/vorona", exports);
    }

    public static void build(String path, HashMap<String, Value> exports) throws Exception {
        ProgramNode programNode = new ProgramNode();
        for (Map.Entry<String, Value> entry: exports.entrySet()) {
            programNode.addNode(new ExportNode(
                    new ValueNode(entry.getValue()),
                    entry.getKey()
            ));
        }
        FileOutputStream fileOut = new FileOutputStream(path);
        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
        objectOut.writeObject(programNode);
        objectOut.close();
    }
}

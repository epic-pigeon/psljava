import ParserPackage.ASTNodes.ExportNode;
import ParserPackage.ASTNodes.ProgramNode;
import ParserPackage.ASTNodes.ValueNode;
import ParserPackage.Collection;
import ParserPackage.Evaluator;
import ParserPackage.PSLFunction;
import ParserPackage.Value;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class LibBuilder {
    public static void main(String[] args) throws Exception {
        Value fs = new Value();
        fs.put("file", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        String path = (String) t.get(0).getValue();
                        File file = new File(path);
                        Scanner fileReader = new Scanner(file);
                        FileOutputStream fileWriter = new FileOutputStream(
                                path,
                                t.size() <= 1 || Evaluator.toBoolean(t.get(1))
                        );
                        Value fileValue = new Value();
                        Value fileIn = new Value();
                        Value fileOut = new Value();
                        fileIn.put("read_line", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        return new Value(fileReader.nextLine());
                                    }
                                }
                        ));
                        fileIn.put("has_line", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        return new Value(fileReader.hasNextLine());
                                    }
                                }
                        ));
                        fileIn.put("read_number", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        return new Value(
                                                fileReader.hasNextDouble() ?
                                                        fileReader.nextDouble():
                                                        fileReader.nextInt()
                                        );
                                    }
                                }
                        ));
                        fileIn.put("read_word", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        return new Value(
                                                fileReader.next()
                                        );
                                    }
                                }
                        ));
                        fileIn.put("has_word", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        return new Value(
                                                fileReader.hasNext()
                                        );
                                    }
                                }
                        ));
                        fileIn.put("has_number", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        return new Value(
                                                fileReader.hasNextDouble() || fileReader.hasNextInt()
                                        );
                                    }
                                }
                        ));
                        fileOut.put("write", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        for (Value value : t) {
                                            fileWriter.write(String.valueOf(value.getValue()).getBytes());
                                        }
                                        return Value.NULL;
                                    }
                                }
                        ));
                        fileOut.put("write_line", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        for (Value value : t) {
                                            fileWriter.write((value.getValue() + "\n").getBytes());
                                        }
                                        return Value.NULL;
                                    }
                                }
                        ));
                        fileValue.put("in", fileIn);
                        fileValue.put("out", fileOut);
                        return fileValue;
                    }
                }
        ));
        HashMap<String, Value> exports = new HashMap<>();
        exports.put("fs", fs);
        build("lib/fs", exports);
    }

    private static void build(String path, HashMap<String, Value> exports) throws Exception {
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

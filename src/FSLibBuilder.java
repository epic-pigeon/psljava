import ParserPackage.Collection;
import ParserPackage.Evaluator;
import ParserPackage.PSLFunction;
import ParserPackage.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Scanner;

public class FSLibBuilder extends LibBuilder {
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
                        Value fileInNumberStream = new Value();
                        fileInNumberStream.put("read", new Value(
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
                        fileIn.put("number_stream", fileInNumberStream);
                        Value fileInLineStream = new Value();
                        fileInLineStream.put("read", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        return new Value(
                                                fileReader.nextLine()
                                        );
                                    }
                                }
                        ));
                        fileIn.put("line_stream", fileInLineStream);
                        Value fileInWordStream = new Value();
                        fileInWordStream.put("read", new Value(
                                new PSLFunction() {
                                    @Override
                                    public Value apply(Collection<Value> t) throws Exception {
                                        return new Value(
                                                fileReader.next()
                                        );
                                    }
                                }
                        ));
                        fileIn.put("word_stream", fileInWordStream);
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
}

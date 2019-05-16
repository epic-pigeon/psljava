import ParserPackage.ASTNodes.*;
import ParserPackage.Collection;
import ParserPackage.PSLFunction;
import ParserPackage.Value;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SecondMain {
    public static void main(String[] args) throws Exception {
        ProgramNode node = new ProgramNode();
        Value timeClass = new Value(null);
        timeClass.put("now", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        Date date = new Date();
                        return buildTimeObject(date);
                    }
                }
        ));
        timeClass.put("from_unixtime", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        Date date = new Date(((Number)t.get(0).getValue()).longValue());
                        return buildTimeObject(date);
                    }
                }
        ));
        timeClass.put("from_pattern", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        String value = t.get(0).getValue().toString();
                        String pattern = t.get(1).getValue().toString();
                        Date date = new SimpleDateFormat(pattern).parse(value);
                        return buildTimeObject(date);
                    }
                }
        ));
        ExportNode exportNode = new ExportNode(
                new ValueNode(
                        timeClass
                ),
                "date"
        );
        node.setProgram(new Collection<>(exportNode));
        System.out.println(node);
        FileOutputStream fileOut = new FileOutputStream("lib/time");
        ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
        objectOut.writeObject(node);
        objectOut.close();
    }
    private static Value buildTimeObject(Date date) throws Exception {
        Value result = new Value(date);
        result.put("to_unixtime", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(date.getTime());
                    }
                }
        ));
        result.put("format", new Value(
                new PSLFunction() {
                    @Override
                    public Value apply(Collection<Value> t) throws Exception {
                        return new Value(
                                new SimpleDateFormat(t.get(0).getValue().toString()).format(date)
                        );
                    }
                }
        ));
        return result;
    }
}

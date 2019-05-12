package ParserPackage.ASTNodes;

import ParserPackage.Collection;
import ParserPackage.JSONToString;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;

abstract public class Node extends JSONToString implements Serializable {
    protected String type = getType();
    abstract public String getType();
}

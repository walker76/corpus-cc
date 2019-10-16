package edu.baylor.ecs;

import edu.baylor.ecs.antlr.Python3Lexer;
import edu.baylor.ecs.antlr.Python3Parser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class Runner {
    public static void main(String[] args) {
        String pythonClassContext = "def met(): print(\"Hello\")";
        Python3Lexer lexer = new Python3Lexer(new ANTLRInputStream(pythonClassContext));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Python3Parser parser = new Python3Parser(tokens);
    }
}

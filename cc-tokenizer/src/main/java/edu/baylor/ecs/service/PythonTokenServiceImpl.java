package edu.baylor.ecs.service;

import edu.baylor.ecs.antlr.Python3BaseListener;
import edu.baylor.ecs.antlr.Python3Lexer;
import edu.baylor.ecs.antlr.Python3Parser;
import edu.baylor.ecs.models.BCEToken;
import edu.baylor.ecs.models.MethodRepresentation;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PythonTokenServiceImpl implements TokenService {

    private final FileService fileService;

    public PythonTokenServiceImpl(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public List<MethodRepresentation> tokenizeFiles(List<String> files) throws IOException {
        List<MethodRepresentation> reps = new ArrayList<>();
        int count = 0;
        for (String file : files) {
            if (isIgnorableFile(file)) {
                count++;
                continue;
            }

            System.out.println(String.format("FILE (%d / %d ) - %s", count, files.size(), file));
            String fileContent = fileService.readFile(file);
            Python3Lexer lexer = new Python3Lexer(CharStreams.fromString(fileContent));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Python3Parser parser = new Python3Parser(tokens);
            parser.setBuildParseTree(true);

            ParseTreeWalker.DEFAULT.walk(new Python3BaseListener() {

                @Override
                public void enterFuncdef(@NotNull Python3Parser.FuncdefContext ctx) {
                    MethodRepresentation rep = parseMethod(ctx, tokens);
                    if(rep != null){
                        reps.add(rep);
                    }
                }

            }, parser.file_input());

        }

        return reps;
    }

    private MethodRepresentation parseMethod(Python3Parser.FuncdefContext ctx, CommonTokenStream tokenStream) {
        MethodRepresentation rep = new MethodRepresentation();
        Token startToken = ctx.getStart();
        Token endToken = ctx.getStart();
        int startIndex = startToken.getTokenIndex();
        int endIndex = endToken.getTokenIndex();

        // Get methodName from tokenType
        String methodName = null;
        List<TerminalNode> names = ctx.getTokens(35);
        if(names.size() > 0){
            Token t = names.get(0).getSymbol();
            methodName = t.getText();
        }

        if(isIgnorableMethod(methodName)){
            return null;
        }

        rep.setMethodName(methodName);

        // No className
        rep.setClassName(null);

        String raw = ctx.getText();
        rep.setRaw(raw);
        fileService.countLines(rep);
        fileService.hash(rep);
        List<Token> antlrTokens = tokenStream.get(startIndex, endIndex);
        List<BCEToken> tokens = antlrTokens.stream()
                                            .map(r -> new BCEToken(Python3Lexer.VOCABULARY.getSymbolicName(r.getType()), r))
                                            .collect(Collectors.toList());
        Map<String, Integer> zip = zipTokens(tokens);
        rep.setTokens(tokens);
        rep.setZip(zip);

        rep.setUniqueTokens(countUniqueTokens(rep.getTokens()));

        return rep;
    }

    private Map<String, Integer> zipTokens(List<BCEToken> tokens) {
        Map<String, Integer> counts = new HashMap<>();
        for(BCEToken token : tokens) {
            counts.merge(token.getTokenValue(), 1, Integer::sum);
        }
        return counts;
    }

    private int countUniqueTokens(List<BCEToken> tokens){
        int count = 0;
        for(BCEToken token : tokens){
            if(token.getTokenValue().contains("NAME")){
                count++;
            }
        }
        return count;
    }

    private boolean isIgnorableMethod(String methodName){
        return methodName == null || methodName.startsWith("get") || methodName.startsWith("set")
                || methodName.startsWith("is") || methodName.equals("equals")
                || methodName.equals("hashCode") || methodName.equals("toString");
    }

    private boolean isIgnorableFile(String fileName){
        return fileName.contains("\\src\\test") || fileName.contains("Test")
                || fileName.contains("Tester");
    }

    @Override
    public List<MethodRepresentation> tokenizeSnippets(List<String> files) throws IOException {
        return null;
    }

}

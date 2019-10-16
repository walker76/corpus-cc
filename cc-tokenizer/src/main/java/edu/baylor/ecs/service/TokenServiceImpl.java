package edu.baylor.ecs.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.baylor.ecs.handlers.BaseHandler;
import edu.baylor.ecs.handlers.HandlerFactory;
import edu.baylor.ecs.models.BCEToken;
import edu.baylor.ecs.models.MethodRepresentation;
import edu.baylor.ecs.models.Snippet;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class TokenServiceImpl implements TokenService{

    private final FileService fileService;
    private final TreeService treeService;

    public TokenServiceImpl(FileService fileService, TreeService treeService){
        this.fileService = fileService;
        this.treeService = treeService;
    }

    @Override
    public List<MethodRepresentation> tokenizeFiles(List<String> files) throws IOException {
        List<MethodRepresentation> reps = new ArrayList<>();
        int count = 0;
        for(String file : files) {
            if(isIgnorableFile(file)){
                count++;
                continue;
            }

            System.out.println(String.format("FILE (%d / %d ) - %s", count, files.size(), file));
            String javaContent = fileService.readFile(file);
            CompilationUnit compilationUnit;
            try {
                compilationUnit = StaticJavaParser.parse(javaContent);
            } catch(Exception e){
                System.err.println("Cannot parse " + file);
                continue;
            }
            List<Node> roots = compilationUnit.getChildNodes();
            for(Node root : roots) {
                List<Node> methodDeclarations = treeService.extractNodes(root, MethodDeclaration.class);
                for(Node dec : methodDeclarations) {
                    MethodRepresentation rep = new MethodRepresentation();
                    String methodName = ((MethodDeclaration) dec).getName().getIdentifier();
                    Optional<Node> parent = dec.getParentNode();
                    String parentName = "Undetermined";
                    if(parent.isPresent()){
                        if(parent.get() instanceof ClassOrInterfaceDeclaration) {
                            parentName = ((ClassOrInterfaceDeclaration) parent.get()).getNameAsString();
                        } else if (parent.get() instanceof EnumDeclaration){
                            parentName = ((EnumDeclaration) parent.get()).getNameAsString();
                        }
                    }
                    rep.setClassName(parentName);

                    if (isIgnorableMethod(methodName)){
                        continue;
                    }

                    System.out.println("\tMETHOD - " + methodName);
                    Optional<BlockStmt> bodyOptional = ((MethodDeclaration)dec).getBody();
                    if(bodyOptional.isEmpty()){
                        continue;
                    }
                    Node body = bodyOptional.get();
                    rep.setMethodName(methodName);
                    String raw = body.toString();
                    rep.setRaw(raw);
                    fileService.countLines(rep);
                    fileService.hash(rep);
                    BaseHandler handler = HandlerFactory.getHandler(body);
                    if (handler != null) {
                        List<BCEToken> tokens = new ArrayList<>(handler.handle(body));
                        Map<String, Integer> zip = TokenServiceImpl.zipTokens(tokens);
                        rep.setTokens(tokens);
                        rep.setZip(zip);
                    } else {
                        System.err.println("Unknown node type");
                        System.exit(-1);
                    }
                    rep.setUniqueTokens(TokenServiceImpl.countUniqueTokens(rep.getTokens()));
                    reps.add(rep);
                }
            }
            count++;
        }
        return reps;
    }

    private static Map<String, Integer> zipTokens(List<BCEToken> tokens) {
        Map<String, Integer> counts = new HashMap<>();
        for(BCEToken token : tokens) {
            counts.merge(token.getTokenValue(), 1, Integer::sum);
        }
        return counts;
    }

    private static int countUniqueTokens(List<BCEToken> tokens){
        int count = 0;
        for(BCEToken token : tokens){
            if(token.getTokenValue().contains("NAME")){
                count++;
            }
        }
        return count;
    }

    private boolean isIgnorableMethod(String methodName){
        return methodName.startsWith("get") || methodName.startsWith("set")
                || methodName.startsWith("is") || methodName.equals("equals")
                || methodName.equals("hashCode") || methodName.equals("toString");
    }

    private boolean isIgnorableFile(String fileName){
        return fileName.contains("\\src\\test") || fileName.contains("Test")
                || fileName.contains("Tester");
    }

    @Override
    public List<MethodRepresentation> tokenizeSnippets(List<String> files) throws IOException{
        List<MethodRepresentation> reps = new ArrayList<>();
        Gson gson = new Gson();
        int fileCount = 1;
        for(String file : files){
            System.out.println(String.format("FILE (%d / %d ) - %s", fileCount, files.size(), file));
            String fileContent = fileService.readFile(file);
            List<Snippet> snippets = gson.fromJson(fileContent, new TypeToken<List<Snippet >>(){}.getType());
            int snippetCount = 1;
            for(Snippet snippet : snippets){

                System.out.println(String.format("\tSNIPPET (%d / %d ) - %s", snippetCount, snippets.size(), snippet.getLink()));
                CompilationUnit compilationUnit;
                try {
                    compilationUnit = StaticJavaParser.parse(snippet.getSnippet());
                } catch(Exception e){
                    System.err.println("Cannot parse " + snippet.getLink());
                    snippetCount++;
                    continue;
                }
                List<Node> roots = compilationUnit.getChildNodes();
                for(Node root : roots) {
                    List<Node> methodDeclarations = treeService.extractNodes(root, MethodDeclaration.class);
                    for(Node dec : methodDeclarations) {
                        String methodName = ((MethodDeclaration) dec).getName().getIdentifier();

                        if (isIgnorableMethod(methodName)){
                            continue;
                        }

                        System.out.println("\t\tMETHOD - " + methodName);
                        MethodRepresentation rep = new MethodRepresentation();
                        Optional<BlockStmt> bodyOptional = ((MethodDeclaration)dec).getBody();
                        if(bodyOptional.isEmpty()){
                            continue;
                        }
                        Node body = bodyOptional.get();
                        rep.setMethodName(methodName);
                        rep.setClassName(snippet.getLink());
                        String raw = body.toString();
                        rep.setRaw(raw);
                        fileService.countLines(rep);
                        fileService.hash(rep);
                        BaseHandler handler = HandlerFactory.getHandler(body);
                        if (handler != null) {
                            List<BCEToken> tokens = new ArrayList<>(handler.handle(body));
                            Map<String, Integer> zip = TokenServiceImpl.zipTokens(tokens);
                            rep.setTokens(tokens);
                            rep.setZip(zip);
                        } else {
                            System.err.println("Unknown node type");
                            System.exit(-1);
                        }
                        rep.setUniqueTokens(TokenServiceImpl.countUniqueTokens(rep.getTokens()));
                        reps.add(rep);
                    }
                }
                snippetCount++;
            }
            fileCount++;
        }

        return reps;
    }
}

package edu.baylor.ecs.service;

import edu.baylor.ecs.models.MethodRepresentation;

import java.io.IOException;
import java.util.List;

public interface TokenService {

    List<MethodRepresentation> tokenizeFiles(List<String> files) throws IOException;
    List<MethodRepresentation> tokenizeSnippets(List<String> files) throws IOException;
}

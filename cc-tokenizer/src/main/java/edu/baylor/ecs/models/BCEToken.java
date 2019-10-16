package edu.baylor.ecs.models;

import org.antlr.v4.runtime.Token;

public class BCEToken {

    private String tokenValue;
    private String node;

    private BCEToken() {
        // Private for serialization
    }

    public BCEToken(String tokenValue, Token token) {
        this.tokenValue = tokenValue;
        this.node = String.valueOf(token.getType());
    }

    public BCEToken(String tokenValue, String node) {
        this.tokenValue = tokenValue;
        this.node = node.getClass().getSimpleName();
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

}

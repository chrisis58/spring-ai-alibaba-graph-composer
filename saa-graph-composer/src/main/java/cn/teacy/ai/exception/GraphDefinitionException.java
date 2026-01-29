package cn.teacy.ai.exception;

public class GraphDefinitionException extends RuntimeException {
    public GraphDefinitionException(String message) {
        super(message);
    }

    public GraphDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}

package fr.astromate.application.model;

public class ApplicationException extends RuntimeException {
    public enum Kind {
        BAD_REQUEST, NOT_FOUND
    }

    private final Kind kind;

    public ApplicationException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public ApplicationException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}

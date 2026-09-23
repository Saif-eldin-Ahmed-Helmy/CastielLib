package dev.castiel.lib.actions;

/** Result of validating and executing a configured action sequence. */
public final class ActionRunResult {
    private static final ActionRunResult SUCCESS = new ActionRunResult(true, null);
    private final boolean successful;
    private final String diagnostic;

    private ActionRunResult(boolean successful, String diagnostic) {
        this.successful = successful;
        this.diagnostic = diagnostic;
    }

    public static ActionRunResult success() {
        return SUCCESS;
    }

    public static ActionRunResult failure(String diagnostic) {
        return new ActionRunResult(false, diagnostic == null ? "Action failed" : diagnostic);
    }

    public boolean successful() {
        return successful;
    }

    public String diagnostic() {
        return diagnostic;
    }
}

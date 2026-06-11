package dev.castiel.lib.actions;

@FunctionalInterface
public interface ActionHandler {
    void execute(ActionContext context, String payload);
}

package dev.castiel.lib.requirements;

public interface Requirement {
    boolean test(RequirementContext context);
}

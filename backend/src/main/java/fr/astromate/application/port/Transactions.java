package fr.astromate.application.port;

public interface Transactions {
    void execute(Runnable action);

    void afterCommit(Runnable action);
}

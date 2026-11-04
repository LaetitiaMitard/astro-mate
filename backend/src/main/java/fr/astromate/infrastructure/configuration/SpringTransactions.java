package fr.astromate.infrastructure.configuration;

import fr.astromate.application.port.Transactions;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringTransactions implements Transactions {
    private final TransactionTemplate template;

    public SpringTransactions(PlatformTransactionManager manager) {
        this.template = new TransactionTemplate(manager);
    }

    @Override
    public void execute(Runnable action) {
        template.executeWithoutResult(status -> action.run());
    }

    @Override
    public void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}

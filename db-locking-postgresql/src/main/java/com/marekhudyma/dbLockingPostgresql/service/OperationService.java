package com.marekhudyma.dbLockingPostgresql.service;

import com.marekhudyma.dbLockingPostgresql.model.Account;
import com.marekhudyma.dbLockingPostgresql.model.Operation;
import com.marekhudyma.dbLockingPostgresql.repository.AccountNativeRepository;
import com.marekhudyma.dbLockingPostgresql.repository.AccountPessimisticWriteRepository;
import com.marekhudyma.dbLockingPostgresql.repository.OperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OperationService {

    private final PlatformTransactionManager transactionManager;

    private final AccountPessimisticWriteRepository accountPessimisticWriteRepository;

    private final AccountNativeRepository accountNativeRepository;

    private final OperationRepository operationRepository;

    public OperationService(PlatformTransactionManager transactionManager,
                            AccountPessimisticWriteRepository accountPessimisticWriteRepository,
                            AccountNativeRepository accountNativeRepository,
                            OperationRepository operationRepository) {
        this.transactionManager = transactionManager;
        this.accountPessimisticWriteRepository = accountPessimisticWriteRepository;
        this.accountNativeRepository = accountNativeRepository;
        this.operationRepository = operationRepository;
    }

    /**
     * This method creates list of operations for given account.
     *
     * Method execute given algorithm:
     * <code>
     * Begin Transaction
     *    SELECT FOR UPDATE
     *    if(result == null) {
     *       INSERT INTO ACCOUNT ON CONFLICT DO NOTHING
     *       COMMIT
     *       Begin Transaction
     *       SELECT FOR UPDATE
     *     }
     *     DO OPERATIONS
     * COMMIT
     * </code>
     *
     * @param operations list of operations that need to be executed
     */
    public void createOperations(UUID accountId, List<Operation> operations) {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        boolean accountExisted = transactionTemplate.execute(
                transactionStatus -> createAccountIfNotExistOrExecuteOperations(accountId, operations));

        if (!accountExisted) {
            TransactionTemplate transactionTemplateForMissingAccount = new TransactionTemplate(transactionManager);
            transactionTemplateForMissingAccount.execute(
                    transactionStatus -> executeOperations(accountId, operations));
        }
    }

    private boolean createAccountIfNotExistOrExecuteOperations(UUID accountId,
                                                               List<Operation> operations) {
        Optional<Account> account = accountPessimisticWriteRepository.findById(accountId);
        if (!account.isPresent()) {
            accountNativeRepository.insertOnConflictDoNothing(accountId);
            return false;
        } else {
            executeOperations(accountId, operations);
            return true;
        }
    }

    /**
     * This method should be executed inside transaction (by TransactionTemplate).
     * First lock account (now it need to exist) and execute list of operations.
     *
     * @param accountId  id of account
     * @param operations list of operations
     */
    private List<Operation> executeOperations(UUID accountId, List<Operation> operations) {
        Optional<Account> accountForLocking = accountPessimisticWriteRepository.findById(accountId);
        return operationRepository.saveAll(operations);
    }

}

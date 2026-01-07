package com.programmingwithtyler.financeforge.repository;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.domain.Transaction;
import com.programmingwithtyler.financeforge.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByTransactionDateBetween(LocalDate start, LocalDate end);
    List<Transaction> findByCategory(BudgetCategory category);
    List<Transaction> findBySourceAccountOrDestinationAccount(Account source, Account destination);
    List<Transaction> findByType(TransactionType type);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.category = :category AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumAmountByCategoryAndDateRange(@Param("category") BudgetCategory category,
                                               @Param("start") LocalDate start,
                                               @Param("end") LocalDate end);
}
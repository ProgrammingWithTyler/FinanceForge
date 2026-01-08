package com.programmingwithtyler.financeforge.repository;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.RecurringExpense;
import com.programmingwithtyler.financeforge.domain.TransactionFrequency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    List<RecurringExpense> findByActive(boolean active);
    List<RecurringExpense> findByNextScheduledDate(LocalDate nextDate);
    List<RecurringExpense> findByNextScheduledDateBetween(LocalDate start, LocalDate end);
    List<RecurringExpense> findByActiveAndNextScheduledDateBetween(boolean active, LocalDate start, LocalDate end);
    List<RecurringExpense> findByFrequency(TransactionFrequency frequency);
    List<RecurringExpense> findBySourceAccount(Account source);
}
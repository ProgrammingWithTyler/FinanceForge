package com.programmingwithtyler.financeforge.repository;

import com.programmingwithtyler.financeforge.domain.Budget;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

  List<Budget> findByCategory(BudgetCategory category);
  List<Budget> findByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(LocalDate start, LocalDate end);
  List<Budget> findByActive(boolean active);
}
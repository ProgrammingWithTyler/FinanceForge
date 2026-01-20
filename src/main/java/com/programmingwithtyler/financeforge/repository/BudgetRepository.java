package com.programmingwithtyler.financeforge.repository;

import com.programmingwithtyler.financeforge.domain.Budget;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Budget entity persistence operations.
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

  /**
   * Find all budgets ordered by period start descending.
   */
  List<Budget> findAllByOrderByPeriodStartDesc();

  /**
   * Find budgets by category ordered by period start descending.
   */
  List<Budget> findByCategoryOrderByPeriodStartDesc(BudgetCategory category);

  /**
   * Find budgets by active status ordered by period start descending.
   */
  List<Budget> findByActiveOrderByPeriodStartDesc(boolean active);

  /**
   * Find budgets by category and active status ordered by period start.
   */
  List<Budget> findByCategoryAndActiveOrderByPeriodStartDesc(
      BudgetCategory category,
      boolean active
  );

  /**
   * Find active budget for a category that contains the given date.
   */
  @Query("""
        SELECT b FROM Budget b 
        WHERE b.category = :category 
          AND b.active = true
          AND :date BETWEEN b.periodStart AND b.periodEnd
        """)
  Optional<Budget> findByCategoryAndDateWithinPeriod(
      @Param("category") BudgetCategory category,
      @Param("date") LocalDate date
  );

  /**
   * Find budgets for a category with overlapping period.
   * Used to enforce non-overlapping budget constraint.
   */
  @Query("""
        SELECT b FROM Budget b 
        WHERE b.category = :category
          AND b.active = true
          AND (
            (b.periodStart <= :end AND b.periodEnd >= :start)
          )
        """)
  List<Budget> findByCategoryAndPeriodOverlap(
      @Param("category") BudgetCategory category,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end
  );

  /**
   * Find active budgets with periods overlapping the given date range.
   * Used for period-based reporting and rollover.
   */
  @Query("""
        SELECT b FROM Budget b 
        WHERE b.active = :active
          AND (
            (b.periodStart <= :end AND b.periodEnd >= :start)
          )
        """)
  List<Budget> findByActiveAndPeriodOverlap(
      @Param("active") boolean active,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end
  );
}

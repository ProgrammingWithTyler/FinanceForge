package com.programmingwithtyler.financeforge.api.service.impl;

import com.programmingwithtyler.financeforge.domain.Budget;
import com.programmingwithtyler.financeforge.domain.BudgetCategory;
import com.programmingwithtyler.financeforge.api.dto.PeriodSummary;
import com.programmingwithtyler.financeforge.repository.BudgetRepository;
import com.programmingwithtyler.financeforge.repository.TransactionRepository;
import com.programmingwithtyler.financeforge.service.BudgetService;
import com.programmingwithtyler.financeforge.service.impl.MonthEndRolloverServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonthEndRolloverService Tests")
class MonthEndRolloverServiceImplTest {

    @Mock
    private BudgetService budgetService;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private MonthEndRolloverServiceImpl rolloverService;

    private LocalDate jan2024Start;
    private LocalDate jan2024End;
    private Budget groceriesBudget;
    private Budget utilitiesBudget;

    @BeforeEach
    void setUp() {
        jan2024Start = LocalDate.of(2024, 1, 1);
        jan2024End = LocalDate.of(2024, 1, 31);

        groceriesBudget = createBudget(1L, BudgetCategory.GROCERIES, new BigDecimal("500.00"), jan2024Start, jan2024End);
        utilitiesBudget = createBudget(2L, BudgetCategory.UTILITIES, new BigDecimal("200.00"), jan2024Start, jan2024End);
    }

    // ========== closeBudgetPeriod Tests ==========

    @Test
    @DisplayName("Should close all active budgets for valid period")
    void closeBudgetPeriod_validPeriod_closesAllBudgets() {
        // Arrange
        List<Budget> budgets = List.of(groceriesBudget, utilitiesBudget);
        when(budgetRepository.findByActiveAndPeriodOverlap(true, jan2024Start, jan2024End))
            .thenReturn(budgets);

        // Act
        int closedCount = rolloverService.closeBudgetPeriod(2024, 1);

        // Assert
        assertThat(closedCount).isEqualTo(2);
        verify(budgetRepository, times(2)).save(any(Budget.class));
        assertThat(groceriesBudget.isActive()).isFalse();
        assertThat(utilitiesBudget.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should return 0 when no budgets exist for period")
    void closeBudgetPeriod_noBudgets_returnsZero() {
        // Arrange
        when(budgetRepository.findByActiveAndPeriodOverlap(true, jan2024Start, jan2024End))
            .thenReturn(Collections.emptyList());

        // Act
        int closedCount = rolloverService.closeBudgetPeriod(2024, 1);

        // Assert
        assertThat(closedCount).isZero();
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid month")
    void closeBudgetPeriod_invalidMonth_throwsException() {
        assertThatThrownBy(() -> rolloverService.closeBudgetPeriod(2024, 13))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Month must be between 1");
    }

    @Test
    @DisplayName("Should throw exception for future period")
    void closeBudgetPeriod_futurePeriod_throwsException() {
        int futureYear = YearMonth.now().plusMonths(1).getYear();
        int futureMonth = YearMonth.now().plusMonths(1).getMonthValue();

        assertThatThrownBy(() -> rolloverService.closeBudgetPeriod(futureYear, futureMonth))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot close future period");
    }

    // ========== initializeBudgetPeriod Tests ==========

    @Test
    @DisplayName("Should initialize new period from source period")
    void initializeBudgetPeriod_validRequest_createsNewBudgets() {
        // Arrange
        LocalDate feb2024Start = LocalDate.of(2024, 2, 1);
        LocalDate feb2024End = LocalDate.of(2024, 2, 29);

        List<Budget> sourceBudgets = List.of(groceriesBudget, utilitiesBudget);
        List<Budget> newBudgets = List.of(
            createBudget(3L, BudgetCategory.GROCERIES, new BigDecimal("500.00"), feb2024Start, feb2024End),
            createBudget(4L, BudgetCategory.UTILITIES, new BigDecimal("200.00"), feb2024Start, feb2024End)
        );

        when(budgetRepository.findByActiveAndPeriodOverlap(true, jan2024Start, jan2024End))
            .thenReturn(sourceBudgets);
        when(budgetRepository.findByActiveAndPeriodOverlap(true, feb2024Start, feb2024End))
            .thenReturn(Collections.emptyList());
        when(budgetService.rolloverBudgets(jan2024Start, jan2024End, feb2024Start, feb2024End))
            .thenReturn(newBudgets);

        // Act
        List<Budget> result = rolloverService.initializeBudgetPeriod(2024, 2, 2024, 1);

        // Assert
        assertThat(result).hasSize(2);
        verify(budgetService).rolloverBudgets(jan2024Start, jan2024End, feb2024Start, feb2024End);
    }

    @Test
    @DisplayName("Should throw exception when source period has no budgets")
    void initializeBudgetPeriod_noSourceBudgets_throwsException() {
        // Arrange
        when(budgetRepository.findByActiveAndPeriodOverlap(eq(true), any(), any()))
            .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> rolloverService.initializeBudgetPeriod(2024, 2, 2024, 1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No active budgets found for source period");
    }

    @Test
    @DisplayName("Should throw exception when target period already has budgets")
    void initializeBudgetPeriod_targetHasBudgets_throwsException() {
        // Arrange
        LocalDate feb2024Start = LocalDate.of(2024, 2, 1);
        LocalDate feb2024End = LocalDate.of(2024, 2, 29);

        when(budgetRepository.findByActiveAndPeriodOverlap(true, jan2024Start, jan2024End))
            .thenReturn(List.of(groceriesBudget));
        when(budgetRepository.findByActiveAndPeriodOverlap(true, feb2024Start, feb2024End))
            .thenReturn(List.of(utilitiesBudget)); // Target already has budgets

        // Act & Assert
        assertThatThrownBy(() -> rolloverService.initializeBudgetPeriod(2024, 2, 2024, 1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already has")
            .hasMessageContaining("active budget");
    }

    // ========== generatePeriodSummary Tests ==========

    @Test
    @DisplayName("Should generate accurate period summary")
    void generatePeriodSummary_validPeriod_returnsAccurateSummary() {
        // Arrange
        List<Budget> budgets = List.of(groceriesBudget, utilitiesBudget);
        when(budgetRepository.findByPeriodOverlap(jan2024Start, jan2024End))
            .thenReturn(budgets);

        // FIXED: Return Optional<BigDecimal> instead of BigDecimal
        when(transactionRepository.sumByCategoryAndPeriod(BudgetCategory.GROCERIES, jan2024Start, jan2024End))
            .thenReturn(Optional.of(new BigDecimal("450.00")));
        when(transactionRepository.sumByCategoryAndPeriod(BudgetCategory.UTILITIES, jan2024Start, jan2024End))
            .thenReturn(Optional.of(new BigDecimal("220.00"))); // Over budget

        // Act
        PeriodSummary summary = rolloverService.generatePeriodSummary(2024, 1);

        // Assert
        assertThat(summary.year()).isEqualTo(2024);
        assertThat(summary.month()).isEqualTo(1);
        assertThat(summary.periodStart()).isEqualTo(jan2024Start);
        assertThat(summary.periodEnd()).isEqualTo(jan2024End);
        assertThat(summary.totalAllocated()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(summary.totalSpent()).isEqualByComparingTo(new BigDecimal("670.00"));
        assertThat(summary.utilization()).isGreaterThan(BigDecimal.ZERO);
        assertThat(summary.overBudgetCount()).isEqualTo(1); // Only utilities
        assertThat(summary.totalBudgets()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle zero allocation without division error")
    void generatePeriodSummary_zeroAllocation_returnsZeroUtilization() {
        // Arrange
        Budget zeroBudget = createBudget(1L, BudgetCategory.GROCERIES, BigDecimal.ZERO, jan2024Start, jan2024End);
        when(budgetRepository.findByPeriodOverlap(jan2024Start, jan2024End))
            .thenReturn(List.of(zeroBudget));

        // FIXED: Return Optional<BigDecimal>
        when(transactionRepository.sumByCategoryAndPeriod(BudgetCategory.GROCERIES, jan2024Start, jan2024End))
            .thenReturn(Optional.of(BigDecimal.ZERO));

        // Act
        PeriodSummary summary = rolloverService.generatePeriodSummary(2024, 1);

        // Assert
        assertThat(summary.utilization()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should handle empty Optional from transaction repository")
    void generatePeriodSummary_noTransactions_returnsZeroSpent() {
        // Arrange
        List<Budget> budgets = List.of(groceriesBudget);
        when(budgetRepository.findByPeriodOverlap(jan2024Start, jan2024End))
            .thenReturn(budgets);

        // FIXED: Return Optional.empty() for no transactions
        when(transactionRepository.sumByCategoryAndPeriod(BudgetCategory.GROCERIES, jan2024Start, jan2024End))
            .thenReturn(Optional.empty());

        // Act
        PeriodSummary summary = rolloverService.generatePeriodSummary(2024, 1);

        // Assert
        assertThat(summary.totalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.overBudgetCount()).isZero();
    }

    @Test
    @DisplayName("Should throw exception when no budgets exist for period")
    void generatePeriodSummary_noBudgets_throwsException() {
        // Arrange
        when(budgetRepository.findByPeriodOverlap(jan2024Start, jan2024End))
            .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> rolloverService.generatePeriodSummary(2024, 1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No budgets found for period");
    }

    @Test
    @DisplayName("Should correctly count over-budget items")
    void generatePeriodSummary_multipleOverBudgets_countsCorrectly() {
        // Arrange
        Budget entertainmentBudget = createBudget(3L, BudgetCategory.ENTERTAINMENT,
            new BigDecimal("100.00"), jan2024Start, jan2024End);

        List<Budget> budgets = List.of(groceriesBudget, utilitiesBudget, entertainmentBudget);
        when(budgetRepository.findByPeriodOverlap(jan2024Start, jan2024End))
            .thenReturn(budgets);

        // All three are over budget
        when(transactionRepository.sumByCategoryAndPeriod(BudgetCategory.GROCERIES, jan2024Start, jan2024End))
            .thenReturn(Optional.of(new BigDecimal("550.00"))); // Over by 50
        when(transactionRepository.sumByCategoryAndPeriod(BudgetCategory.UTILITIES, jan2024Start, jan2024End))
            .thenReturn(Optional.of(new BigDecimal("220.00"))); // Over by 20
        when(transactionRepository.sumByCategoryAndPeriod(BudgetCategory.ENTERTAINMENT, jan2024Start, jan2024End))
            .thenReturn(Optional.of(new BigDecimal("150.00"))); // Over by 50

        // Act
        PeriodSummary summary = rolloverService.generatePeriodSummary(2024, 1);

        // Assert
        assertThat(summary.overBudgetCount()).isEqualTo(3);
        assertThat(summary.totalBudgets()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should validate year parameter bounds")
    void closeBudgetPeriod_invalidYear_throwsException() {
        assertThatThrownBy(() -> rolloverService.closeBudgetPeriod(1899, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Year must be between 1900 and 2100");

        assertThatThrownBy(() -> rolloverService.closeBudgetPeriod(2101, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Year must be between 1900 and 2100");
    }

    @Test
    @DisplayName("Should validate month parameter bounds")
    void closeBudgetPeriod_invalidMonthBounds_throwsException() {
        assertThatThrownBy(() -> rolloverService.closeBudgetPeriod(2024, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Month must be between 1");

        assertThatThrownBy(() -> rolloverService.closeBudgetPeriod(2024, 13))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Month must be between 1");
    }

    // ========== Helper Methods ==========

    /**
     * Creates a Budget entity with the specified parameters.
     * Uses reflection to set the ID since it's auto-generated in production.
     */
    private Budget createBudget(Long id, BudgetCategory category, BigDecimal amount,
                                LocalDate start, LocalDate end) {
        Budget budget = new Budget(category, amount, start, end);
        budget.setActive(true);

        // Use reflection to set ID since it's generated by JPA
        try {
            var idField = Budget.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(budget, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // In test context, this is acceptable
            // Alternative: use ReflectionTestUtils from Spring Test
        }

        return budget;
    }
}
package com.programmingwithtyler.financeforge.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.programmingwithtyler.financeforge.domain.*;
import com.programmingwithtyler.financeforge.api.dto.request.*;
import com.programmingwithtyler.financeforge.api.exception.GlobalExceptionHandler;
import com.programmingwithtyler.financeforge.service.TransactionService;
import com.programmingwithtyler.financeforge.service.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TransactionController.
 *
 * Tests cover:
 * - Happy path scenarios for all transaction types
 * - Validation failures (negative amounts, future dates, null fields)
 * - Business rule violations (insufficient funds, same-account transfer, inactive accounts)
 * - Filtering with various combinations
 * - Error response formats
 */
@WebMvcTest(controllers = TransactionController.class)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    // ============================================
    // INCOME TRANSACTION TESTS
    // ============================================

    @Test
    void recordIncome_withValidRequest_returns201Created() throws Exception {
        // Arrange
        RecordIncomeRequest request = new RecordIncomeRequest(
            1L,
            new BigDecimal("3000.00"),
            LocalDate.of(2026, 1, 15),
            "Salary deposit"
        );

        Account destinationAccount = createAccount(1L, "Chase Checking", AccountType.CHECKING);
        Transaction mockTransaction = createIncomeTransaction(1L, destinationAccount, request);

        when(transactionService.recordIncome(any())).thenReturn(mockTransaction);

        // Act & Assert
        mockMvc.perform(post("/transactions/income")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/transactions/1"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.type").value("INCOME"))
            .andExpect(jsonPath("$.amount").value(3000.00))
            .andExpect(jsonPath("$.destinationAccount.id").value(1))
            .andExpect(jsonPath("$.destinationAccount.accountName").value("Chase Checking"))
            .andExpect(jsonPath("$.sourceAccount").doesNotExist())
            .andExpect(jsonPath("$.isRecurring").value(false));
    }

    @Test
    void recordIncome_withNegativeAmount_returns400BadRequest() throws Exception {
        // Arrange
        RecordIncomeRequest request = new RecordIncomeRequest(
            1L,
            new BigDecimal("-100.00"),
            LocalDate.of(2026, 1, 15),
            "Invalid negative income"
        );

        // Act & Assert
        mockMvc.perform(post("/transactions/income")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.amount").value("Amount must be positive"));
    }

    @Test
    void recordIncome_withFutureDate_returns400BadRequest() throws Exception {
        // Arrange
        RecordIncomeRequest request = new RecordIncomeRequest(
            1L,
            new BigDecimal("3000.00"),
            LocalDate.of(2027, 12, 31), // Future date
            "Invalid future income"
        );

        // Act & Assert
        mockMvc.perform(post("/transactions/income")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.transactionDate")
                .value("Transaction date cannot be in the future"));
    }

    @Test
    void recordIncome_withNonExistentAccount_returns404NotFound() throws Exception {
        // Arrange
        RecordIncomeRequest request = new RecordIncomeRequest(
            999L,
            new BigDecimal("3000.00"),
            LocalDate.of(2026, 1, 15),
            "Income to non-existent account"
        );

        when(transactionService.recordIncome(any()))
            .thenThrow(new AccountNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(post("/transactions/income")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Account not found with ID: 999"));
    }

    @Test
    void recordIncome_withInactiveAccount_returns409Conflict() throws Exception {
        // Arrange
        RecordIncomeRequest request = new RecordIncomeRequest(
            1L,
            new BigDecimal("3000.00"),
            LocalDate.of(2026, 1, 15),
            "Income to inactive account"
        );

        when(transactionService.recordIncome(any()))
            .thenThrow(new InactiveAccountException(1L));

        // Act & Assert
        mockMvc.perform(post("/transactions/income")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value(containsString("inactive")));
    }

    // ============================================
    // EXPENSE TRANSACTION TESTS
    // ============================================

    @Test
    void recordExpense_withValidRequest_returns201Created() throws Exception {
        // Arrange
        RecordExpenseRequest request = new RecordExpenseRequest(
            1L,
            new BigDecimal("87.50"),
            BudgetCategory.GROCERIES,
            LocalDate.of(2026, 1, 16),
            "Weekly grocery shopping"
        );

        Account sourceAccount = createAccount(1L, "Chase Checking", AccountType.CHECKING);
        Transaction mockTransaction = createExpenseTransaction(2L, sourceAccount, request);

        when(transactionService.recordExpense(any())).thenReturn(mockTransaction);

        // Act & Assert
        mockMvc.perform(post("/transactions/expense")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/transactions/2"))
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.type").value("EXPENSE"))
            .andExpect(jsonPath("$.amount").value(87.50))
            .andExpect(jsonPath("$.budgetCategory").value("GROCERIES"))
            .andExpect(jsonPath("$.sourceAccount.id").value(1))
            .andExpect(jsonPath("$.destinationAccount").doesNotExist());
    }

    @Test
    void recordExpense_withInsufficientFunds_returns409Conflict() throws Exception {
        // Arrange
        RecordExpenseRequest request = new RecordExpenseRequest(
            1L,
            new BigDecimal("5000.00"),
            BudgetCategory.GROCERIES,
            LocalDate.of(2026, 1, 16),
            "Expensive groceries"
        );

        when(transactionService.recordExpense(any()))
            .thenThrow(new InsufficientFundsException(
                1L, new BigDecimal("1500.00"), new BigDecimal("5000.00")));

        // Act & Assert
        mockMvc.perform(post("/transactions/expense")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message")
                .value(containsString("Insufficient funds")));
    }

    @Test
    void recordExpense_withMissingCategory_returns400BadRequest() throws Exception {
        // Arrange - using JSON directly to bypass Java validation
        String invalidJson = """
            {
                "sourceAccountId": 1,
                "amount": 87.50,
                "transactionDate": "2026-01-16",
                "description": "Missing category"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/transactions/expense")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.category").exists());
    }

    // ============================================
    // TRANSFER TRANSACTION TESTS
    // ============================================

    @Test
    void recordTransfer_withValidRequest_returns201Created() throws Exception {
        // Arrange
        RecordTransferRequest request = new RecordTransferRequest(
            1L,
            2L,
            new BigDecimal("500.00"),
            LocalDate.of(2026, 1, 16),
            "Transfer to savings"
        );

        Account sourceAccount = createAccount(1L, "Chase Checking", AccountType.CHECKING);
        Account destinationAccount = createAccount(2L, "Savings Account", AccountType.SAVINGS);
        Transaction mockTransaction = createTransferTransaction(3L, sourceAccount, destinationAccount, request);

        when(transactionService.recordTransfer(any())).thenReturn(mockTransaction);

        // Act & Assert
        mockMvc.perform(post("/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/transactions/3"))
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.type").value("TRANSFER"))
            .andExpect(jsonPath("$.amount").value(500.00))
            .andExpect(jsonPath("$.sourceAccount.id").value(1))
            .andExpect(jsonPath("$.destinationAccount.id").value(2))
            .andExpect(jsonPath("$.budgetCategory").doesNotExist());
    }

    @Test
    void recordTransfer_withSameSourceAndDestination_returns400BadRequest() throws Exception {
        // Arrange - same account for source and destination
        RecordTransferRequest request = new RecordTransferRequest(
            1L,
            1L, // Same as source!
            new BigDecimal("500.00"),
            LocalDate.of(2026, 1, 16),
            "Invalid same-account transfer"
        );

        // Act & Assert
        mockMvc.perform(post("/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.validTransfer")
                .value("Source and destination accounts must be different"));
    }

    // ============================================
    // LIST TRANSACTIONS TESTS
    // ============================================

    @Test
    void listTransactions_withNoFilters_returns200Ok() throws Exception {
        // Arrange
        Account account = createAccount(1L, "Chase Checking", AccountType.CHECKING);
        Transaction transaction = createExpenseTransaction(1L, account,
            new RecordExpenseRequest(1L, new BigDecimal("50.00"), BudgetCategory.GROCERIES,
                LocalDate.of(2026, 1, 16), "Groceries"));

        when(transactionService.listTransactions(isNull(), isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(transaction));

        // Act & Assert
        mockMvc.perform(get("/transactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].type").value("EXPENSE"));
    }

    @Test
    void listTransactions_withDateRangeFilter_returns200Ok() throws Exception {
        // Arrange
        when(transactionService.listTransactions(
            eq(LocalDate.of(2026, 1, 1)),
            eq(LocalDate.of(2026, 1, 31)),
            isNull(), isNull(), isNull()))
            .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/transactions")
                .param("dateFrom", "2026-01-01")
                .param("dateTo", "2026-01-31"))
            .andExpect(status().isOk());

        verify(transactionService).listTransactions(
            eq(LocalDate.of(2026, 1, 1)),
            eq(LocalDate.of(2026, 1, 31)),
            isNull(), isNull(), isNull()
        );
    }

    @Test
    void listTransactions_withInvalidDateRange_returns400BadRequest() throws Exception {
        // Act & Assert - dateFrom after dateTo
        mockMvc.perform(get("/transactions")
                .param("dateFrom", "2026-01-31")
                .param("dateTo", "2026-01-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("dateFrom must be before or equal to dateTo"));
    }

    @Test
    void listTransactions_withCategoryFilter_returns200Ok() throws Exception {
        // Arrange
        when(transactionService.listTransactions(
            isNull(), isNull(), eq(BudgetCategory.GROCERIES), isNull(), isNull()))
            .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/transactions")
                .param("category", "GROCERIES"))
            .andExpect(status().isOk());

        verify(transactionService).listTransactions(
            isNull(), isNull(), eq(BudgetCategory.GROCERIES), isNull(), isNull()
        );
    }

    @Test
    void listTransactions_withAccountIdFilter_returns200Ok() throws Exception {
        // Arrange
        when(transactionService.listTransactions(
            isNull(), isNull(), isNull(), eq(1L), isNull()))
            .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/transactions")
                .param("accountId", "1"))
            .andExpect(status().isOk());

        verify(transactionService).listTransactions(
            isNull(), isNull(), isNull(), eq(1L), isNull()
        );
    }

    @Test
    void listTransactions_withTypeFilter_returns200Ok() throws Exception {
        // Arrange
        when(transactionService.listTransactions(
            isNull(), isNull(), isNull(), isNull(), eq(TransactionType.EXPENSE)))
            .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/transactions")
                .param("type", "EXPENSE"))
            .andExpect(status().isOk());

        verify(transactionService).listTransactions(
            isNull(), isNull(), isNull(), isNull(), eq(TransactionType.EXPENSE)
        );
    }

    // ============================================
    // GET TRANSACTION BY ID TESTS
    // ============================================

    @Test
    void getTransaction_withExistingId_returns200Ok() throws Exception {
        // Arrange
        Account account = createAccount(1L, "Chase Checking", AccountType.CHECKING);
        Transaction mockTransaction = createExpenseTransaction(1L, account,
            new RecordExpenseRequest(1L, new BigDecimal("50.00"), BudgetCategory.GROCERIES,
                LocalDate.of(2026, 1, 16), "Groceries"));

        when(transactionService.getTransaction(1L)).thenReturn(mockTransaction);

        // Act & Assert
        mockMvc.perform(get("/transactions/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    void getTransaction_withNonExistentId_returns404NotFound() throws Exception {
        // Arrange
        when(transactionService.getTransaction(999L))
            .thenThrow(new TransactionNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(get("/transactions/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Transaction with ID 999 not found"));
    }

    // ============================================
    // DELETE TRANSACTION TESTS
    // ============================================

    @Test
    void deleteTransaction_withExistingId_returns204NoContent() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/transactions/1"))
            .andExpect(status().isNoContent());

        verify(transactionService).deleteTransaction(1L);
    }

    @Test
    void deleteTransaction_withNonExistentId_returns404NotFound() throws Exception {
        // Arrange
        doThrow(new TransactionNotFoundException(999L))
            .when(transactionService).deleteTransaction(999L);

        // Act & Assert
        mockMvc.perform(delete("/transactions/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Transaction with ID 999 not found"));
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private Account createAccount(Long id, String name, AccountType type) {
        Account account = new Account(name, type, "Test account");
        account.setId(id);
        account.setCurrentBalance(new BigDecimal("5000.00"));
        return account;
    }

    private Transaction createIncomeTransaction(Long id, Account destinationAccount,
                                                RecordIncomeRequest request) {
        Transaction transaction = Transaction.income(
            destinationAccount,
            request.amount(),
            request.description()
        );
        transaction.setId(id);
        transaction.updateDetails(request.amount(), request.transactionDate(), request.description());
        return transaction;
    }

    private Transaction createExpenseTransaction(Long id, Account sourceAccount,
                                                 RecordExpenseRequest request) {
        Transaction transaction = Transaction.expense(
            sourceAccount,
            request.amount(),
            request.category(),
            request.description()
        );
        transaction.setId(id);
        transaction.updateDetails(request.amount(), request.transactionDate(), request.description());
        return transaction;
    }

    private Transaction createTransferTransaction(Long id, Account sourceAccount,
                                                  Account destinationAccount,
                                                  RecordTransferRequest request) {
        Transaction transaction = Transaction.transfer(
            sourceAccount,
            destinationAccount,
            request.amount(),
            request.description()
        );
        transaction.setId(id);
        transaction.updateDetails(request.amount(), request.transactionDate(), request.description());
        return transaction;
    }
}
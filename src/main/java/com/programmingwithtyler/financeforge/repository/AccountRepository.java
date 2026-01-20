package com.programmingwithtyler.financeforge.repository;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Account entity persistence operations.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

  /**
   * Check if an account with the given name exists.
   */
  boolean existsByAccountName(String accountName);

  /**
   * Find all accounts ordered by name ascending.
   */
  List<Account> findAllByOrderByAccountNameAsc();

  /**
   * Find accounts by active status ordered by name.
   */
  List<Account> findByActiveOrderByAccountNameAsc(boolean active);

  /**
   * Find accounts by type ordered by name.
   */
  List<Account> findByTypeOrderByAccountNameAsc(AccountType type);

  /**
   * Find accounts by active status and type ordered by name.
   */
  List<Account> findByActiveAndTypeOrderByAccountNameAsc(boolean active, AccountType type);

  /**
   * Calculate sum of balances for active accounts.
   */
  @Query("SELECT SUM(a.currentBalance) FROM Account a WHERE a.active = :active")
  Optional<BigDecimal> sumBalancesByActive(@Param("active") boolean active);

  /**
   * Calculate sum of balances for active accounts of a specific type.
   */
  @Query("""
        SELECT SUM(a.currentBalance) 
        FROM Account a 
        WHERE a.active = :active AND a.type = :type
        """)
  Optional<BigDecimal> sumBalancesByActiveAndType(
      @Param("active") boolean active,
      @Param("type") AccountType type
  );

  /**
   * Calculate sum of balances for accounts below a threshold.
   */
  @Query("""
        SELECT SUM(a.currentBalance) 
        FROM Account a 
        WHERE a.currentBalance < :threshold
        """)
  Optional<BigDecimal> sumBalancesBelowThreshold(@Param("threshold") BigDecimal threshold);
}
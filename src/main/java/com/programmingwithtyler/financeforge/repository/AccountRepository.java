package com.programmingwithtyler.financeforge.repository;

import com.programmingwithtyler.financeforge.domain.Account;
import com.programmingwithtyler.financeforge.domain.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
  List<Account> findByType(AccountType type);
  List<Account> findByActive(boolean active);
}
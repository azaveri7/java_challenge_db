package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientFundsException;
import com.db.awmd.challenge.exception.StaleDataException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

	private final Map<String, Account> accounts = new ConcurrentHashMap<>();

	@Override
	public void createAccount(Account account) throws DuplicateAccountIdException {
		Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
		if (previousAccount != null) {
			throw new DuplicateAccountIdException("Account id " + account.getAccountId() + " already exists!");
		}
	}

	@Override
	public Account getAccount(String accountId) {
		return accounts.get(accountId);
	}

	@Override
	public void clearAccounts() {
		accounts.clear();
	}

	@Override
	public synchronized void transferMoney(Account accountFrom, Account accountTo, BigDecimal amount) throws Exception {

		if (null != accountFrom && null != accountTo && null != amount) {

			if (accountFrom.getBalance().compareTo(amount) == -1)
				throw new InsufficientFundsException(
						"Source account " + accountFrom.getAccountId() + " has insufficient funds.");
			
			Account srcAccount = getAccount(accountFrom.getAccountId());
			Account destAccount = getAccount(accountTo.getAccountId());
			
			if((srcAccount.getBalance().compareTo(accountFrom.getBalance()) == 0)
				&& (destAccount.getBalance().compareTo(accountTo.getBalance()) == 0)){
				
				accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
				accountTo.setBalance(accountTo.getBalance().add(amount));

				accounts.put(accountFrom.getAccountId(), accountFrom);
				accounts.put(accountTo.getAccountId(), accountTo);
			} else
				throw new StaleDataException("Please try again.");
			
		} else
			throw new Exception("Please check input parameters");

	}

}

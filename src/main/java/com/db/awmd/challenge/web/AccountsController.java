package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferTransaction;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientFundsException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;

import java.math.BigDecimal;

import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;
  
  private final NotificationService notificationService;

  @Autowired
  public AccountsController(AccountsService accountsService, NotificationService notificationService) {
    this.accountsService = accountsService;
    this.notificationService = notificationService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
    this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }
  
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, path="/transfer-money")
  public ResponseEntity<Object> transferMoney(@RequestBody @Valid TransferTransaction
		  transferTransaction) {
    
	String message = "Money transfer of amount " + transferTransaction.getAmount() + " from account " + transferTransaction.getAccountFromId()
    		+ " to account " + transferTransaction.getAccountToId();

    log.info(message);
    
    Account sourceAccount = null;
    Account destinationAccount = null;
    
    try {
    	
    	sourceAccount = this.accountsService.getAccount(transferTransaction.getAccountFromId());
    	destinationAccount = this.accountsService.getAccount(transferTransaction.getAccountToId());
    	this.accountsService.
    		transferMoney(sourceAccount, destinationAccount, transferTransaction.getAmount());
    	
    } catch (InsufficientFundsException ife) {
    	
    	this.notificationService.notifyAboutTransfer(sourceAccount, ife.getMessage());
    	return new ResponseEntity<>(ife.getMessage(), HttpStatus.BAD_REQUEST);
    	
    } catch (Exception e){
    	
    	this.notificationService.notifyAboutTransfer(sourceAccount, e.getMessage());
    	return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    this.notificationService.notifyAboutTransfer(sourceAccount, message);
	this.notificationService.notifyAboutTransfer(destinationAccount, message);
	
    return new ResponseEntity<>(HttpStatus.OK);
  }

}

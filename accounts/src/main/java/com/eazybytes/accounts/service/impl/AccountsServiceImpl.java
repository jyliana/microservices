package com.eazybytes.accounts.service.impl;

import com.eazybytes.accounts.constants.AccountsConstants;
import com.eazybytes.accounts.dto.AccountsMsgDto;
import com.eazybytes.accounts.dto.CustomerDto;
import com.eazybytes.accounts.exception.CustomerAlreadyExistsException;
import com.eazybytes.accounts.exception.ResourceNotFoundException;
import com.eazybytes.accounts.dto.AccountsDto;
import com.eazybytes.accounts.enitity.Accounts;
import com.eazybytes.accounts.enitity.Customer;
import com.eazybytes.accounts.mapper.AccountsMapper;
import com.eazybytes.accounts.mapper.CustomerMapper;
import com.eazybytes.accounts.repository.AccountsRepository;
import com.eazybytes.accounts.repository.CustomerRepository;
import com.eazybytes.accounts.service.IAccountsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.Random;

@Slf4j
@Service
@AllArgsConstructor
public class AccountsServiceImpl implements IAccountsService {

  private AccountsRepository accountsRepository;
  private CustomerRepository customerRepository;
  private final StreamBridge streamBridge;

  @Override
  public void createAccount(CustomerDto customerDto) {
    var customer = CustomerMapper.mapToCustomer(customerDto, new Customer());
    var optionalCustomer = customerRepository.findByMobileNumber(customerDto.getMobileNumber());

    if (optionalCustomer.isPresent()) {
      throw new CustomerAlreadyExistsException("Customer already registered with given mobileNumber " + customerDto.getMobileNumber());
    }

    var savedCustomer = customerRepository.save(customer);
    var savedAccount = accountsRepository.save(createNewAccount(savedCustomer));
    sendCommunication(savedAccount, savedCustomer);
  }

  private void sendCommunication(Accounts account, Customer customer) {
    var accountsMsgDto = new AccountsMsgDto(account.getAccountNumber(), customer.getName(),
            customer.getEmail(), customer.getMobileNumber());
    log.info("Sending Communication request for the details: {}", accountsMsgDto);
    var result = streamBridge.send("sendCommunication-out-0", accountsMsgDto);
    log.info("Is the Communication request successfully triggered ? : {}", result);
  }

  @Override
  public CustomerDto fetchAccount(String mobileNumber) {
    var customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
            () -> new ResourceNotFoundException("Customer", "mobile number", mobileNumber));

    var accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
            () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
    );

    var customerDto = CustomerMapper.mapToCustomerDto(customer, new CustomerDto());
    customerDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

    return customerDto;
  }

  @Override
  public boolean updateAccount(CustomerDto customerDto) {
    boolean isUpdated = false;
    var accountsDto = customerDto.getAccountsDto();
    if (accountsDto != null) {
      var accounts = accountsRepository.findById(accountsDto.getAccountNumber()).orElseThrow(
              () -> new ResourceNotFoundException("Account", "AccountNumber", accountsDto.getAccountNumber().toString())
      );
      AccountsMapper.mapToAccounts(accountsDto, accounts);
      accounts = accountsRepository.save(accounts);

      Long customerId = accounts.getCustomerId();
      Customer customer = customerRepository.findById(customerId).orElseThrow(
              () -> new ResourceNotFoundException("Customer", "CustomerID", customerId.toString())
      );
      CustomerMapper.mapToCustomer(customerDto, customer);
      customerRepository.save(customer);
      isUpdated = true;
    }
    return isUpdated;
  }

  @Override
  public boolean deleteAccount(String mobileNumber) {
    var customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
            () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
    );
    accountsRepository.deleteByCustomerId(customer.getCustomerId());
    customerRepository.deleteById(customer.getCustomerId());
    return true;
  }

  /**
   * @param customer - Customer Object
   * @return the new account details
   */
  private Accounts createNewAccount(Customer customer) {
    var newAccount = new Accounts();
    newAccount.setCustomerId(customer.getCustomerId());
    long randomAccNumber = 1000000000L + new Random().nextInt(900000000);

    newAccount.setAccountNumber(randomAccNumber);
    newAccount.setAccountType(AccountsConstants.SAVINGS);
    newAccount.setBranchAddress(AccountsConstants.ADDRESS);
    return newAccount;
  }

}

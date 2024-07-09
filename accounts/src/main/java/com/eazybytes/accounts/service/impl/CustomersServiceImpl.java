package com.eazybytes.accounts.service.impl;

import com.eazybytes.accounts.dto.AccountsDto;
import com.eazybytes.accounts.dto.CardsDto;
import com.eazybytes.accounts.dto.CustomerDetailsDto;
import com.eazybytes.accounts.dto.LoansDto;
import com.eazybytes.accounts.enitity.Accounts;
import com.eazybytes.accounts.enitity.Customer;
import com.eazybytes.accounts.exception.ResourceNotFoundException;
import com.eazybytes.accounts.mapper.AccountsMapper;
import com.eazybytes.accounts.mapper.CustomerMapper;
import com.eazybytes.accounts.repository.AccountsRepository;
import com.eazybytes.accounts.repository.CustomerRepository;
import com.eazybytes.accounts.service.ICustomersService;
import com.eazybytes.accounts.service.client.CardsFeignClient;
import com.eazybytes.accounts.service.client.LoansFeignClient;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomersServiceImpl implements ICustomersService {

  private final AccountsRepository accountsRepository;
  private final CustomerRepository customerRepository;
  private final CardsFeignClient cardsFeignClient;
  private final LoansFeignClient loansFeignClient;

  @Override
  public CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId) {
	Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
			() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
	);
	Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
			() -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
	);

	CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());

	customerDetailsDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

	ResponseEntity<LoansDto> loansDtoResponseEntity = loansFeignClient.fetchLoanDetails(correlationId, mobileNumber);
	customerDetailsDto.setLoansDto(loansDtoResponseEntity.getBody());

	ResponseEntity<CardsDto> cardsDtoResponseEntity = cardsFeignClient.fetchCardDetails(correlationId, mobileNumber);
	customerDetailsDto.setCardsDto(cardsDtoResponseEntity.getBody());

	return customerDetailsDto;
  }

}

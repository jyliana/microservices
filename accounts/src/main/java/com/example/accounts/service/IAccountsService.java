package com.example.accounts.service;

import com.example.accounts.dto.CustomerDto;

public interface IAccountsService {

  /**
   * @param customerDto - CustomerDto Object
   */
  void createAccount(CustomerDto customerDto);

}

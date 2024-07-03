package com.eazybytes.accounts.enitity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Accounts extends BaseEntity {

  @Id
  @Column(name = "account_number")
  private Long accountNumber;

  @Column(name = "account_type")
  private String accountType;

  @Column(name = "customer_id")
  private Long customerId;

  @Column(name = "branch_address")
  private String branchAddress;
}

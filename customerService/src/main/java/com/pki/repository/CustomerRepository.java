package com.pki.repository;

import com.pki.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer,Integer> {
//    Optional<Customer> findByUsername(String name);
}

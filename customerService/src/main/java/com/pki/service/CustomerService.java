package com.pki.service;

import com.pki.dto.ApiResponse;
import com.pki.dto.CustomerDto;
import com.pki.model.Customer;
import com.pki.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {
//    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper mapper;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public ApiResponse save( CustomerDto customerDto){
        try {
            Customer customer = mapper.convertValue(customerDto, Customer.class);
            customerRepository.save(customer);

            return new ApiResponse("Customer successfully created.");
        }catch (Exception e){
            e.printStackTrace();
            return new ApiResponse("Failed to create.");
        }
    }

}

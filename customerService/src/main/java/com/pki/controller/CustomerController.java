package com.pki.controller;

import com.pki.dto.ApiResponse;
//import com.apache.kafka.dto.OrderDto;
import com.pki.dto.CustomerDto;
import com.pki.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerController {
//    @Autowired
    private CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }


    @PostMapping("api/save-customer")
    public ResponseEntity<ApiResponse> saveCustomer(@RequestBody CustomerDto customerDto){
        ApiResponse response = this.customerService.save(customerDto);
        return ResponseEntity.ok(response);
    }

}

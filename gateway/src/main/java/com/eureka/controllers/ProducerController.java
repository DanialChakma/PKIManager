//package com.apache.kafka.controllers;
//
//import com.apache.kafka.dto.ApiResponse;
//import com.apache.kafka.dto.OrderDto;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import com.apache.kafka.services.Producer;
//
//@RestController
//public class ProducerController {
//
//    private final Producer producer;
//
//    @Autowired
//    public ProducerController(Producer producer) {
//        this.producer = producer;
//    }
//
//    @PostMapping("/publish")
//    public void messageToTopic(@RequestParam("message") String message){
//        this.producer.sendMessage(message);
//    }
//
//    @PostMapping("/place-order")
//    public ResponseEntity<ApiResponse> placeOrder(@RequestBody OrderDto orderData){
//        this.producer.sendOrder(orderData);
//        return ResponseEntity.ok(new ApiResponse("Order placed successfully"));
//    }
//
//}

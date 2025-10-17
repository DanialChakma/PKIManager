//package com.apache.kafka.services;
//
//import com.apache.kafka.dto.OrderDto;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Service;
//
//@Service
//public class Consumer {
//    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
//
//    @KafkaListener(topics = "test_topic", groupId = "my-consumer-group")
//    public void consumeMessage(OrderDto message){
//
//        logger.info("Message arrived: " + message);
////        System.out.println(message);
//
//    }
//}

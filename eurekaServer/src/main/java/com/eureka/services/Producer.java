//package com.apache.kafka.services;
//
//import com.apache.kafka.dto.OrderDto;
//import org.apache.kafka.clients.admin.NewTopic;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//public class Producer {
//    private static final String TOPIC = "test_topic";
//    @Autowired
//    private KafkaTemplate<String,String> kafkaTemplate;
//
//    @Autowired
//    private KafkaTemplate<String, OrderDto> kafkaOrderTemplate;
//
//    public void sendMessage(String message){
//
//        this.kafkaTemplate.send(TOPIC, message);
//    }
//
//    public void sendOrder(OrderDto message){
//        this.kafkaOrderTemplate.send(TOPIC, message);
//    }
//
//
//
//    @Bean
//    public NewTopic createTopic(){
//
//        return new NewTopic(TOPIC,1,(short) 1);
//    }
//
//
//
//}
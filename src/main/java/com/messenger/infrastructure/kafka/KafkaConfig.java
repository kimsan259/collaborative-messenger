package com.messenger.infrastructure.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * KafkaConfig - Kafka Producer/Consumer 설정
 * ============================================================
 *
 * 【역할】
 * Kafka 메시지를 보내는(Producer) 설정과 받는(Consumer) 설정을 정의합니다.
 *
 * 【직렬화 전략: String 기반】
 * Spring Boot 4 + Jackson 3.x 호환성을 위해 Key/Value 모두 String으로 직렬화합니다.
 * JSON ↔ 객체 변환은 Producer/Consumer 코드에서 직접 처리합니다.
 * - Key: chatRoomId (문자열)
 * - Value: ChatMessageEvent를 JSON 문자열로 변환
 *
 * 【이 프로젝트에서의 흐름】
 * 1. 사용자가 채팅 메시지를 보냄 (WebSocket)
 * 2. Producer가 "chat.message.sent" 토픽에 JSON 문자열을 발행
 *    - 파티션 키 = chatRoomId → 같은 채팅방의 메시지는 같은 파티션으로 → 순서 보장
 * 3. Consumer가 JSON 문자열을 소비 → 역직렬화 → DB 저장 + WebSocket 브로드캐스트
 * ============================================================
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // ===== Producer 설정 =====

    /**
     * 【Producer 설정 맵】
     * Key/Value 모두 StringSerializer를 사용합니다.
     * JSON 변환은 ChatMessageProducer에서 직접 처리합니다.
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * 【KafkaTemplate】
     * kafkaTemplate.send(토픽, 키, JSON문자열) 형태로 사용합니다.
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ===== Consumer 설정 =====

    /**
     * 【Consumer 설정 맵】
     * Key/Value 모두 StringDeserializer를 사용합니다.
     * JSON → 객체 변환은 ChatMessageConsumer에서 직접 처리합니다.
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * 【Kafka Listener Container Factory】
     * @KafkaListener 어노테이션이 붙은 메서드를 실행할 컨테이너를 생성합니다.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);  // 3개의 Consumer 스레드가 동시에 메시지 처리
        return factory;
    }
}

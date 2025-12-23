package com.example.cart.config;

import com.example.cart.dto.CartDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, CartDTO> cartRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, CartDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JDK serialization for values (simpler and works with Serializable objects)
        JdkSerializationRedisSerializer valueSerializer = new JdkSerializationRedisSerializer();
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}



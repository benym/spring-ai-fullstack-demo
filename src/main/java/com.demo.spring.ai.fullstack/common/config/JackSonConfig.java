package com.demo.spring.ai.fullstack.common.config;


import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson配置 支持LocalDateTime系列序列化、反序列化自动格式
 * 解决Long类型精度丢失问题
 */
@Configuration
public class JackSonConfig {

    /**
     * DateTime格式化字符串
     */
    private static final String DEFAULT_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * Date格式化字符串
     */
    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

    /**
     * Time格式化字符串
     */
    private static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer defaultJackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder.timeZone(TimeZone.getDefault())
                .dateFormat(new SimpleDateFormat(DEFAULT_DATETIME_PATTERN))
                .serializerByType(LocalDateTime.class,
                        new LocalDateTimeSerializer(
                                DateTimeFormatter.ofPattern(DEFAULT_DATETIME_PATTERN)))
                .deserializerByType(LocalDateTime.class,
                        new LocalDateTimeDeserializer(
                                DateTimeFormatter.ofPattern(DEFAULT_DATETIME_PATTERN)))
                .serializerByType(LocalDate.class,
                        new LocalDateSerializer(
                                DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN)))
                .deserializerByType(LocalDate.class,
                        new LocalDateDeserializer(
                                DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN)))
                .serializerByType(LocalTime.class,
                        new LocalTimeSerializer(
                                DateTimeFormatter.ofPattern(DEFAULT_TIME_PATTERN)))
                .deserializerByType(LocalTime.class,
                        new LocalTimeDeserializer(
                                DateTimeFormatter.ofPattern(DEFAULT_TIME_PATTERN)))
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}

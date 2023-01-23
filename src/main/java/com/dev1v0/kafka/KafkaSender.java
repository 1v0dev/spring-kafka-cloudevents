package com.dev1v0.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaSender {

    private static final Logger log = LoggerFactory.getLogger(KafkaSender.class);

    private final KafkaTemplate<String, CloudEvent> template;
    private final ObjectMapper mapper;

    public KafkaSender(KafkaTemplate<String, CloudEvent> template,
                       ObjectMapper mapper) {
        this.template = template;
        this.mapper = mapper;
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void send() throws JsonProcessingException {
        SampleData data = new SampleData();
        CloudEvent ce = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("https://1v0dev/producer"))
                .withType("com.dev1v0.producer")
                .withData(mapper.writeValueAsBytes(data))
                .withExtension("name", data.getName())
                .build();

        template.send(TopicNames.MAIN_TOPIC, ce)
                .thenRun(() -> log.info("Message sent. Id: {}; Data: {}", ce.getId(), data));
    }

}

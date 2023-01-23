package com.dev1v0.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaReceiver {
    private static final Logger log = LoggerFactory.getLogger(KafkaSender.class);

    private final ObjectMapper mapper;

    public KafkaReceiver(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @KafkaListener(id = "producerListener", topics = TopicNames.MAIN_TOPIC)
    public void listen(CloudEvent message) {

        //convert message data from binary json to SampleData
        PojoCloudEventData<SampleData> deserializedData = CloudEventUtils
                .mapData(message, PojoCloudEventDataMapper.from(mapper, SampleData.class));

        if (deserializedData != null) {
            SampleData data = deserializedData.getValue();
            log.info("Received message. Id: {}; Data: {}", message.getId(), data.toString());
        } else {
            log.warn("No data in message {}", message.getId());
        }

    }

}

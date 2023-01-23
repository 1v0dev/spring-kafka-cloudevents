# Intro

This is a sample project on how to send [CloudEvents](https://cloudevents.io/) with [Spring Boot](https://spring.io/projects/spring-boot) and [Apache Kafka](https://kafka.apache.org/)

# Initial Configuration

## Running Kafka

The simplest way to run Kafka is using Docker containers and Docker Compose. Here is my compose file using [bitnami](https://bitnami.com/) images:
```yaml
version: "3"  
services:  
  zookeeper:  
    image: 'bitnami/zookeeper:latest'  
    container_name: zookeeper  
    ports:  
      - '2181:2181'  
    environment:  
      - ALLOW_ANONYMOUS_LOGIN=yes  
  kafka:  
    image: 'bitnami/kafka:latest'  
    container_name: kafka  
    ports:  
      - '9092:9092'  
    environment:  
      - KAFKA_BROKER_ID=1  
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092  
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://127.0.0.1:9092  
      - KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181  
      - ALLOW_PLAINTEXT_LISTENER=yes  
    depends_on:  
      - zookeeper
```

## Dependancies

We need to add our libraries. I will be using Gradle with Kotlin:

```kotlin
dependencies {  
    implementation("org.springframework.boot:spring-boot-starter")  
    implementation("org.springframework.boot:spring-boot-starter-json") //Json for serialization  
    implementation("org.springframework.kafka:spring-kafka") //Spring Kafka library  
    implementation("io.cloudevents:cloudevents-kafka:2.4.1") //CloudEvents Kafka serialization/deserialization  
    implementation("io.cloudevents:cloudevents-json-jackson:2.4.1") //CloudEvents Jackson bindings for event data  
    implementation("com.github.javafaker:javafaker:1.0.2") //Using faker to generate some data   
}
```

## Spring Application

To our `@SpringBootApplication` class we need to configure a Kafka topic to send and receive messages

```java
@Bean  
public NewTopic mainTopic() {  
    return TopicBuilder.  
            name(TopicNames.MAIN_TOPIC)  
            .build();  
}
```

And here is my `TopicNames` class
```java
public class TopicNames {  
  
    public static final String MAIN_TOPIC = "main-topic";  
  
}
```

# Producing events

Now onto more interesting stuff. In order to easily serialize and deserialize CloudEvents easily we need to configure Spring producer factory. We start with the configs:

```java
@Bean  
public Map<String, Object> producerConfigs() {  
    Map<String, Object> props = new HashMap<>();  
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");  
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);  
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CloudEventSerializer.class);  
    return props;  
}
```

The main thing here is adding `CloudEventSerializer.class` for serializing the event values. I don't use specific Kafka keys in this sample, so we just use strings and `StringSerializer.class` for them.

Then we use this config to create a factory and `KafkaTemplate`
```java
@Bean  
public ProducerFactory<String, CloudEvent> producerFactory() {  
    return new DefaultKafkaProducerFactory<>(producerConfigs());  
}  
  
@Bean  
public KafkaTemplate<String, CloudEvent> kafkaTemplate() {  
    return new KafkaTemplate<>(producerFactory());  
}
```

Sending the actual event is simple.`SampleData`is just a POJO, and we use the CloudEvents builder API to create our event object which we then pass to Spring's `KafkaTemplate`. The configured serializer will take care of the rest.
```java
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
```

Note how we serialise the `data` object to JSON byte array using Jackson's `ObjectMapper`. The CloudEvents API require using byte array for the data property. In the consumer we will use a class provided by CloudEvents to convert the data back to `SampleData`.

Of course using JSON is just one way of doing this. [Apache Avro](https://avro.apache.org/) or [ProtocolBuffers](https://developers.google.com/protocol-buffers) are also good fits here, especially if you need some kind of schema repository for synchronisation of the data format.

# Receiving Events

The same way as the producer, in order to receive CloudEvents we first need to configure Spring's consumer factory:
```java
private Map<String, Object> consumerProps() {  
    Map<String, Object> props = new HashMap<>();  
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");  
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "group");  
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);  
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CloudEventDeserializer.class);  
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");  
    return props;  
}
```

Again the important bits here are the `CloudEventDeserializer.class` which will handle our events and `StringDeserializer.class` to handle the keys. The rest is standard config.

And here is the client itself:
```java
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
```

We use the Spring's `@KafkaListener` annotation to configure our client. The deserializer will take care of the event conversion. But this is only for the CloudEvent itself. In order to get `SampleData` object from the `data` field of the event we need to use `PojoCloudEventDataMapper` provided by the CloudEvents Jackson library. Here is the relevant bit of the official documentation https://cloudevents.github.io/sdk-java/json-jackson.html#mapping-cloudeventdata-to-pojos-using-jackson-objectmapper

# Conclusion

And that is it. If you have Kafka running, you can start the sample app and you will send and receive CloudEvent messages through Kafka.
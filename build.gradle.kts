plugins {
    java
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
}

group = "com.dev1v0"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_19

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-json") //Json for serialization
    implementation("org.springframework.kafka:spring-kafka") //Spring Kafka library
    implementation("io.cloudevents:cloudevents-kafka:2.4.1") //CloudEvents Kafka serialization/deserialization
    implementation("io.cloudevents:cloudevents-json-jackson:2.4.1") //CloudEvents Jackson bindings for event data
    implementation("com.github.javafaker:javafaker:1.0.2") //Using faker to generate some data
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

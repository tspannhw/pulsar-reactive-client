package com.rpuch.pulsar.reactive.it;

import com.rpuch.pulsar.reactive.api.ReactiveProducer;
import com.rpuch.pulsar.reactive.api.ReactivePulsarClient;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Reader;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.impl.schema.StringSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Roman Puchkovskiy
 */
public class ProducerIntegrationTests extends TestWithPulsar {
    private PulsarClient coreClient;
    private ReactivePulsarClient reactiveClient;

    private final MessageConverter converter = new MessageConverter();

    private final String topic = UUID.randomUUID().toString();

    @BeforeEach
    void createClients() throws Exception {
        coreClient = PulsarClient.builder()
                .serviceUrl(pulsarBrokerUrl())
                .build();
        reactiveClient = ReactivePulsarClient.from(coreClient);
    }

    @AfterEach
    void cleanUp() throws Exception {
        reactiveClient.close();
    }

    @Test
    void producerSendViaForOnePublishesSuccessfully() throws Exception {
        reactiveClient.newProducer()
                .topic(topic)
                .forOne(producer -> Flux.range(0, 10)
                        .map(converter::intToBytes)
                        .concatMap(producer::send)
                        .then()
                )
                .block();

        assertThat10MessagesAreInTheTopicWithValuesFromZeroToNine();
    }

    private void assertThat10MessagesAreInTheTopicWithValuesFromZeroToNine() throws IOException {
        List<Integer> ints = new ArrayList<>();
        try (Reader<byte[]> reader = coreClient.newReader().topic(topic).startMessageId(MessageId.earliest).create()) {
            while (reader.hasMessageAvailable()) {
                Message<byte[]> message = reader.readNext();
                int n = converter.intFromBytes(message.getData());
                ints.add(n);
            }
        }

        assertThat(ints, equalTo(listOfZeroToNine()));
    }

    private List<Integer> listOfZeroToNine() {
        return IntStream.range(0, 10).boxed().collect(toList());
    }

    @Test
    void producerSendViaForManyPublishesSuccessfully() throws Exception {
        reactiveClient.newProducer()
                .topic(topic)
                .forMany(producer -> Flux.range(0, 10)
                        .map(converter::intToBytes)
                        .concatMap(producer::send)
                        .thenMany(Flux.empty())
                )
                .blockLast();

        assertThat10MessagesAreInTheTopicWithValuesFromZeroToNine();
    }

    @Test
    void producerSendPublishesSuccessfullyWithSchema() throws Exception {
        reactiveClient.newProducer(StringSchema.utf8())
                .topic(topic)
                .forOne(producer -> Flux.range(0, 10)
                        .map(converter::intToString)
                        .concatMap(producer::send)
                        .then()
                )
                .block();

        assertThat10MessagesAreInTheTopicWithValuesFromZeroToNine();
    }

    @Test
    void producerFlushWorksSuccessfully() {
        reactiveClient.newProducer()
                .topic(topic)
                .forOne(ReactiveProducer::flush)
                .as(StepVerifier::create)
                .verifyComplete();
    }
    
    @Test
    void messageSendPublishesSuccessfully() throws Exception {
        reactiveClient.newProducer()
                .topic(topic)
                .forOne(producer -> Flux.range(0, 10)
                        .map(converter::intToBytes)
                        .concatMap(payload -> producer.newMessage()
                                .keyBytes(payload)
                                .value(payload)
                                .send()
                        )
                        .then()
                )
                .block();

        assertThat10MessagesAreInTheTopicWithValuesFromZeroToNine();
    }

    @Test
    void messageSendPublishesSuccessfullyWithSchema() throws Exception {
        reactiveClient.newProducer()
                .topic(topic)
                .forOne(producer -> Flux.range(0, 10)
                        .map(converter::intToString)
                        .concatMap(payload -> producer.newMessage(Schema.STRING)
                                .key(payload)
                                .value(payload)
                                .send()
                        )
                        .then()
                )
                .block();

        assertThat10MessagesAreInTheTopicWithValuesFromZeroToNine();
    }
}

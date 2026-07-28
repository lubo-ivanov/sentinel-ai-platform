package com.sentinelai.payment;

import com.sentinelai.payment.kafka.SignalPublisher;
import com.sentinelai.payment.signal.RawSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SignalEmitterTest {

    private SignalPublisher publisher;
    private SignalEmitter emitter;

    @BeforeEach
    void setUp() {
        publisher = mock(SignalPublisher.class);
        emitter = new SignalEmitter(publisher);
    }

    @Test
    void emit_publishesPayloadWithExpectedShape() {
        emitter.emit();

        ArgumentCaptor<RawSignal> captor = ArgumentCaptor.forClass(RawSignal.class);
        verify(publisher).publish(captor.capture());
        RawSignal signal = captor.getValue();

        assertThat(signal.id()).startsWith("pay-");
        assertThat(signal.occurredAt()).isNotEmpty();
        assertThat(signal.message()).isEqualTo("stripe timeout after 5000ms");
        assertThat(signal.hints()).containsEntry("provider", "stripe");
        assertThat(signal.hints()).containsEntry("amount", 42.00);
        assertThat(signal.hints()).containsEntry("currency", "USD");
    }

    @Test
    void emit_incrementsIdAcrossCalls() {
        emitter.emit();
        emitter.emit();
        emitter.emit();

        ArgumentCaptor<RawSignal> captor = ArgumentCaptor.forClass(RawSignal.class);
        verify(publisher, org.mockito.Mockito.times(3)).publish(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(RawSignal::id)
                .containsExactly("pay-1", "pay-2", "pay-3");
    }
}

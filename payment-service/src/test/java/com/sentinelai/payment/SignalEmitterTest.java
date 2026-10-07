package com.sentinelai.payment;

import com.sentinelai.payment.signal.RawSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SignalEmitterTest {

    private SidecarClient sidecarClient;
    private SignalEmitter emitter;

    @BeforeEach
    void setUp() {
        sidecarClient = mock(SidecarClient.class);
        emitter = new SignalEmitter(sidecarClient);
    }

    @Test
    void emit_publishesPayloadWithExpectedShape() {
        emitter.emit();

        ArgumentCaptor<RawSignal> captor = ArgumentCaptor.forClass(RawSignal.class);
        verify(sidecarClient).send(captor.capture());
        RawSignal signal = captor.getValue();

        assertThat(signal.id()).isNotBlank();
        assertThat(signal.occurredAt()).isNotEmpty();
        assertThat(signal.message()).isEqualTo("stripe timeout after 5000ms");
        assertThat(signal.hints()).containsEntry("provider", "stripe");
        assertThat(signal.hints()).containsEntry("amount", 42.00);
        assertThat(signal.hints()).containsEntry("currency", "USD");
    }

    @Test
    void emit_generatesUniqueIds() {
        emitter.emit();
        emitter.emit();

        ArgumentCaptor<RawSignal> captor = ArgumentCaptor.forClass(RawSignal.class);
        verify(sidecarClient, org.mockito.Mockito.times(2)).send(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(RawSignal::id)
                .doesNotHaveDuplicates();
    }
}

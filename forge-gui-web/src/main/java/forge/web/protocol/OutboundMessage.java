package forge.web.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envelope for all server-to-client WebSocket messages.
 */
public class OutboundMessage {

    private MessageType type;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String inputId;

    private long sequenceNumber;

    private Object payload;

    public OutboundMessage() {
        // Default constructor for Jackson
    }

    public OutboundMessage(final MessageType type, final String inputId,
                           final long sequenceNumber, final Object payload) {
        this.type = type;
        this.inputId = inputId;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(final MessageType type) {
        this.type = type;
    }

    public String getInputId() {
        return inputId;
    }

    public void setInputId(final String inputId) {
        this.inputId = inputId;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(final long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(final Object payload) {
        this.payload = payload;
    }
}

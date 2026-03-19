package forge.web.protocol;

/**
 * Envelope for all client-to-server WebSocket messages.
 */
public class InboundMessage {

    private MessageType type;
    private String inputId;
    private Object payload;

    public InboundMessage() {
        // Default constructor for Jackson deserialization
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

    public Object getPayload() {
        return payload;
    }

    public void setPayload(final Object payload) {
        this.payload = payload;
    }
}

package model.serializing;

import model.message.AbstractMessage;

/**
 * Created by Alexander on 30/09/2017.
 */
public interface MessageSerializer {
    /**
     * @param message model.message to serialize
     * @return StandardCharsets.UTF_8 bytes array of serialized
     * model.message
     */
    byte[] serialize(AbstractMessage message);
}

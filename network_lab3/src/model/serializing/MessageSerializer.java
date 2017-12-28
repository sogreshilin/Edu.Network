package model.serializing;

import model.message.Message;

/**
 * Created by Alexander on 30/09/2017.
 */
public interface MessageSerializer {
    /**
     *
     * @param message message to serialize
     *
     * @return StandardCharsets.UTF_8 bytes array of serialized
     *         message
     */
    byte[] serialize(Message message);
}

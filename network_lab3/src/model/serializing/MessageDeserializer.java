package model.serializing;

import model.message.Message;

import java.io.IOException;

/**
 * Created by Alexander on 30/09/2017.
 */
public interface MessageDeserializer {
    /**
     *
     * @param bytes has to be StandardCharset.UTF_8 bytes array
     *              returned from corresponding model.serializing.MessageSerializer
     *
     * @return message of appropriate type
     */
    Message deserialize(byte[] bytes, int length) throws IOException;
}


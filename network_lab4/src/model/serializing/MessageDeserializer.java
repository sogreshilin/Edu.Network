package model.serializing;

import model.message.AbstractMessage;

import java.io.IOException;

/**
 * Created by Alexander on 30/09/2017.
 */
public interface MessageDeserializer {
    /**
     * @param bytes has to be StandardCharset.UTF_8 bytes array
     *              returned from corresponding model.model.serializing.MessageSerializer
     * @return model.message of appropriate type
     */
    AbstractMessage deserialize(byte[] bytes, int length) throws IOException;
}


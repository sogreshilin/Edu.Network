package model.serializing;

import model.message.AbstractMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Created by Alexander on 25/09/2017.
 */
public class XmlMessageSerializer implements MessageSerializer {
    private static final Logger LOG = LogManager.getLogger(XmlMessageSerializer.class);

    @Override
    public byte[] serialize(AbstractMessage message) {
        try {
            JAXBContext context = JAXBContext.newInstance(message.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            StringWriter stringWriter = new StringWriter();
            marshaller.marshal(message, stringWriter);
            return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JAXBException e) {
            LOG.error("model.serializing error", e);
        }
        return null;
    }
}
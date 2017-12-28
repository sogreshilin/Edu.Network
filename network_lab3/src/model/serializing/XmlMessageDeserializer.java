package model.serializing;

import model.message.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Created by Alexander on 25/09/2017.
 */


public class XmlMessageDeserializer implements MessageDeserializer {

    private byte[] data;
    private DocumentBuilder documentBuilder;
    private static final Logger log = LogManager.getLogger(XmlMessageDeserializer.class);

    public XmlMessageDeserializer() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            log.error("parsing : creating deserializer error", e);
        }
    }

    public Message deserialize(byte[] data, int length) throws IOException {
        this.data = Arrays.copyOf(data, length);
        Document doc = null;
        try {
            doc = documentBuilder.parse(
                    new InputSource(
                            new InputStreamReader(
                                    new ByteArrayInputStream(data, 0, length), StandardCharsets.UTF_8)));
        } catch (SAXException e) {
            log.error("SAX deserializing error", e);
        } catch (IOException e) {
            log.error(e);
        }
        return this.parse(doc);
    }

    private Message unmarshallMessage(Class c) {
        try {
            JAXBContext context = JAXBContext.newInstance(c);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            String sdata = new String(data, StandardCharsets.UTF_8);
            return (Message)unmarshaller.unmarshal(new StringReader(sdata));
        } catch (JAXBException e) {
            log.error("parsing : JAXB deserializing error", e);
        }
        return null;
    }

    private Message parse(Document doc) throws IOException {
        Element root = doc.getDocumentElement();
        switch (root.getNodeName()) {
            case Message.ACK: return unmarshallMessage(AckMessage.class);
            case Message.JOIN: return unmarshallMessage(JoinMessage.class);
            case Message.LEAVE: return unmarshallMessage(LeaveMessage.class);
            case Message.TEXT: return unmarshallMessage(TextMessage.class);
            case Message.CHANGE_PARENT: return unmarshallMessage(ChangeParentMessage.class);
            default: throw new IOException("invalid message type");
        }
    }
}

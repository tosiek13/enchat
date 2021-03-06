package message.generators;

import com.google.inject.Inject;
import controller.utils.cypher.Encryption;
import message.types.Header;
import message.types.Message;
import message.types.UEMessage;
import message.types.UMessage;
import messages.MessageId;
import rsa.exceptions.EncryptionException;

import java.util.Arrays;

/**
 * Creator of the messages.
 * Creates message with all strings (user nicks).
 * Each nick is wrapped in own package.
 *
 * @author Created by tochur on 03.05.15.
 */
public class Logged_List {
    private MessageId messageId = MessageId.CLIENTS_LIST;
    private Encryption encryption;

    /**
     * Creates the util user for message creation.
     * @param encryption Encryption - util for encryption the message.
     */
    @Inject
    public Logged_List(Encryption encryption){
        this.encryption = encryption;
    }

    /**
     * Creates the message with clientsList.
     * @param receiverID - the ActiveUser that request for his blackList
     * @param nicks - nicks of logged users
     * @return - encryptedMessage
     * @throws rsa.exceptions.EncryptionException - When encryption of the message failed
     */
    public UEMessage create(Integer receiverID, String[] nicks) throws EncryptionException {
        Header header = HeaderGenerator.createHeader(messageId, 0, nicks.length);
        Message message = new Message(header, Arrays.asList(nicks));
        UMessage uMessage = new UMessage(receiverID, message);

        return encryption.encryptMessage(uMessage);
    }
}

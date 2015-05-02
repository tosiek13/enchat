package message.generarators;

import message.types.EncryptedMessage;
import message.types.Header;
import messages.IncorrectMessageId;
import messages.MessageId;
import rsa.exceptions.EncryptionException;
import user.ActiveUser;

/**
 * Created by tochur on 01.05.15.
 */
public class Server_error {
    public static EncryptedMessage unableToEncrypt(ActiveUser receiver) {
        MessageId id = MessageId.SERVER_ERROR;
        Header header = null;
        try {
            MessageId.ErrorId errorId = id.createErrorId(1);
            header = new Header(id, errorId);
        } catch (IncorrectMessageId incorrectMessageId) {
            System.out.println("Error inside Messages, wrong error number, repair that.");
        }

        return new EncryptedMessage(header);
    }

    public static EncryptedMessage unableToDecrypt(ActiveUser receiver) {
        MessageId id = MessageId.SERVER_ERROR;
        Header header = null;
        try {
            MessageId.ErrorId errorId = id.createErrorId(2);
            header = new Header(id, errorId);
        } catch (IncorrectMessageId incorrectMessageId) {
            System.out.println("Error inside Messages, wrong error number, repair that.");
        }

        return new EncryptedMessage(header);
    }
}
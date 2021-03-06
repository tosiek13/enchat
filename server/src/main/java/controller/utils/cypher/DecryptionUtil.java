package controller.utils.cypher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import message.types.*;
import rsa.RSA;
import rsa.exceptions.DecryptingException;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;

/**
 * Lower level util, that is used to decrypt messages.
 *
 * @author Created by tochur on 16.05.15.
 */

@Singleton
public class DecryptionUtil {
    PrivateKey privateServerKey;

    /**
     * Creates the Decryption Util.
     * @param privateKey PrivateKey, key used to decrypt message from other users.
     */
    @Inject
    public DecryptionUtil(@Named("Server")PrivateKey privateKey){
        this.privateServerKey = privateKey;
    }

    /**
     * Decrypts message.
     * @param encrypted EncryptedMessage, message to decrypt.
     * @param senderKey PublicKey, public Key of the message sender.
     * @return UMessage, message ready to future processing (with data in Strings).
     * @throws rsa.exceptions.DecryptingException when sth went wrong during decrypting.
     */
    public UMessage decryptMessage(UEMessage encrypted, PublicKey senderKey) throws DecryptingException {
        EncryptedMessage encrypt = encrypted.getEncryptedMessage();
        Message message = decryptMessage(encrypt, senderKey);
        return new UMessage(encrypted.getAuthorID(), message);
    }

    /**
     * Decrypts message.
     * @param encrypted EncryptedMessage, message to decrypt.
     * @param senderKey PublicKey, public Key of the message sender.
     * @return Message, message ready to future processing (with data in Strings).
     * @throws rsa.exceptions.DecryptingException when sth went wrong during decrypting.
     */
    public Message decryptMessage(EncryptedMessage encrypted, PublicKey senderKey) throws DecryptingException {
        if( encrypted.getPackageAmount() == 0 ){
            return new Message(encrypted.getId(), encrypted.getErrorId());
        }else{
            List<Pack> packages = encrypted.getPackages();
            List<String> strings = new LinkedList<>();
            for(Pack pack: packages){
                byte[] decrypted = decrypt(pack.getDataArray());
                checkSign(pack.getSignArray(), decrypted, senderKey);
                strings.add(new String(decrypted));
            }
            return new Message(encrypted.getId(), encrypted.getErrorId(), encrypted.getPackageAmount(), strings);
        }
    }


    /**
     * Decrypts data array
     * @param data byte[], array with data to decrypt
     * @return byte[] byte array with decrypted message.
     * @throws rsa.exceptions.DecryptingException when sth went wrong during decrypting.
     */
    public byte[] decrypt(byte[] data) throws DecryptingException {
        try {
            return RSA.decrypt(data, privateServerKey);
        } catch (Exception e) {
            throw new DecryptingException();
        }
    }

    /**
     * Checks sign correctness, using PublicKey passed as parameter.
     * @param sign byte[], array with sign.
     * @param decrypted byte[], array with decrypted package content (main data).
     * @param publicUserKey PublicKey, publicKey of the user - author of the message.
     * @throws DecryptingException when system was unable to decrypt message.
     */
    public void checkSign(byte[] sign, byte[] decrypted, PublicKey publicUserKey) throws DecryptingException {
        try {
            RSA.checkSign(sign, decrypted, publicUserKey);
        } catch (Exception e) {
            throw new DecryptingException();
        }
    }
}

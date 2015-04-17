package rsa;

import rsa.exceptions.GeneratingPublicKeyException;
import rsa.exceptions.ReadingPublicKeyInfoException;
import rsa.services.PublicKeyReader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;


/**
 *
 * @author mateusz
 * @version 1.0
 */
public final class PublicKeyInfo {
    public PublicKeyInfo(PublicKeyInfo publicKeyInfo) throws NoSuchAlgorithmException, InvalidKeySpecException {
        modulus = publicKeyInfo.getModulus();
        exponent = publicKeyInfo.getExponent();
        publicKey = publicKeyInfo.getPublicKey();
    }
    
    
    public PublicKeyInfo(PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        RSAPublicKeySpec pubKeySpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);
            
        modulus = pubKeySpec.getModulus();
        exponent = pubKeySpec.getPublicExponent();
            
        this.publicKey = keyFactory.generatePublic(pubKeySpec);
    }

    public PublicKeyInfo(DataInputStream in) throws GeneratingPublicKeyException {
        try{
            modulus = PublicKeyReader.scanForModulus(in);
            exponent = PublicKeyReader.scanForExponent(in);
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus, exponent);

            /*Generating public key*/
            publicKey = generatePublicKey(pubKeySpec);

        } catch (Exception e) {
            e.printStackTrace();
            throw new GeneratingPublicKeyException("Cannot generate public key");
        }
    }

    private PublicKey generatePublicKey(RSAPublicKeySpec pubKeySpec) throws GeneratingPublicKeyException {
        KeyFactory keyFactory = null;
        try {
            /*Setting encrypting algorithm*/
            keyFactory = KeyFactory.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new GeneratingPublicKeyException("Unknown algorithm");
        }

        try {
            /*Public key constructing*/
            PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);
            return publicKey;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            throw new GeneratingPublicKeyException("Unknown algorithm");
        }
    }

    public void send(DataOutputStream out) throws IOException {
       
       System.out.println("Send Modulus = " + modulus);
       byte[] array = modulus.toByteArray();
       out.writeInt(array.length);
       out.write(array);
       
       System.out.println("Send Exponent = " + exponent);
       array = exponent.toByteArray();
       out.writeInt(array.length);
       out.write(array);
    }
     
    public PublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        return keyFactory.generatePublic(spec);
    }
    
    public BigInteger getModulus() {
        return new BigInteger(modulus.toString());
    }
    
    public BigInteger getExponent() {
        return new BigInteger(exponent.toString());
    }
    
   /**
     * Są to dwie bardzo duże liczby z których składa się klucz publiczny i prywatny
     * -klucz publiczny składa się z liczby modolus i publicznego exponentu
     * -klucz prywatny składa się z tej samej liczby modulus i prywantego exponentu
     * UWAGA DLA TESTERÓW : jeżeli będziecie chcieli sprawdzić czy klucze zostały
     * wygenerowane poprawnie, wystarczy sprawdzić czy liczby modulus w kluczu
     * publicznym i prywatnym są sobie równe.
     */
    private BigInteger modulus;
    private BigInteger exponent;
    
    private PublicKey publicKey;
    private static final String ALGORITHM = "RSA";

/*
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, ClassNotFoundException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        
        PublicKey key = keyPair.getPublic();
        
        PublicKeyInfo pubKeyInfo = new PublicKeyInfo(key);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(out);
        pubKeyInfo.send(o);
        
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        DataInputStream i = new DataInputStream(in);
        PublicKeyInfo pubKetInfo1 = new PublicKeyInfo(i);
    }
*/
}

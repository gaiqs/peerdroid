
import jxta.security.cipher.Cipher;
import jxta.security.crypto.JxtaCrypto;

import jxta.security.impl.cipher.KeyBuilder;
import jxta.security.impl.cipher.SecretKey;
import jxta.security.impl.random.JRandom;
import jxta.security.impl.crypto.JxtaCryptoSuite;

public class TestRC4Cipher {

    public static void main(String[] args) throws Exception {

        // Step 1: Generate the JxtaCryptoSuite that does only
        // RC4 encryption (so all other arguments are not used)
        JxtaCrypto jc = new JxtaCryptoSuite(JxtaCrypto.MEMBER_RC4,
                                null, (byte) 0, (byte) 0);

        // Step 2: Generate the necessary RC4 key
        SecretKey secretKey = (SecretKey) KeyBuilder.buildKey(
                                KeyBuilder.TYPE_RC4,
                                KeyBuilder.LENGTH_RC4, false);
        JRandom random = new JRandom();
        byte[] keydata = new byte[KeyBuilder.LENGTH_RC4 >>> 3];
        random.nextBytes(keydata);
        secretKey.setKey(keydata, 0);

        // Step 3: Use the RC4 key to initialize the cipher
        Cipher c = jc.getJxtaCipher();
        c.init(secretKey, Cipher.MODE_ENCRYPT);

        // Step 4: Encrypt the data -- since our data is short, we
        //  only use the doFinal() method
        byte[] input = "Hello, JXTA".getBytes();
        byte[] ciphertext = new byte[input.length];
        c.doFinal(input, 0, input.length, ciphertext, 0);

        System.out.println("Got encrypted data " + new String(ciphertext));

        // Now we repeat from step 3 to decrypt the string. Note that
        // we must use the same key to initialize the cipher
        c.init(secretKey, Cipher.MODE_DECRYPT);
        byte[] plaintext = new byte[ciphertext.length];
        c.doFinal(ciphertext, 0, ciphertext.length, plaintext, 0);
        System.out.println("Got unencrypted data " + new String(plaintext));
    }
}

import java.util.*;

import net.jxta.document.*;

import jxta.security.util.URLBase64;
import jxta.security.signature.Signature;
import jxta.security.crypto.JxtaCrypto;
import jxta.security.publickey.PublicKeyAlgorithm;

import jxta.security.impl.publickey.RSAPublickeyData;
import jxta.security.impl.publickey.RSAPrivatekeyData;
import jxta.security.impl.publickey.RSAKey;
import jxta.security.impl.cipher.KeyBuilder;
import jxta.security.impl.crypto.JxtaCryptoSuite;

public class TestSignature {

    static RSAPublickeyData publicKeyData;
    static RSAPrivatekeyData privateKeyData;

    // Get the data to sign into the buffer. The data must always appear in
    // the same order, which is why we can’t just enumerate through all the
    // elements in the document; we must look them up in a specific order.
    // Returns the length of the data to sign
    public static int getDataToSign(StructuredDocument doc, byte[] buf) throws Exception {
        int length = 0;
        Enumeration enum = doc.getChildren("Price");
        while (enum.hasMoreElements()) {
            Element element = (Element) enum.nextElement();
            String value = (String) element.getValue();
            byte[] temp = value.getBytes();
            System.arraycopy(temp, 0, buf, length, temp.length);
            length += temp.length;
        }
        enum = doc.getChildren("Brand");
        while (enum.hasMoreElements()) {
            Element element = (Element) enum.nextElement();
            String value = (String) element.getValue();
            byte[] temp = value.getBytes();
            System.arraycopy(temp, 0, buf, length, temp.length);
            length += temp.length;
        }
        return length;
    }

    public static boolean validateMessage(StructuredDocument doc) throws Exception {
        // Step 1: Generate and initialize the necessary RSA key
        RSAKey rsaKey = (RSAKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA,
                                                KeyBuilder.LENGTH_RSA_512,
                                                false);

        // Step 2: Create a profile that can do signing
        JxtaCrypto jc = new JxtaCryptoSuite(JxtaCrypto.PROFILE_RSA_SHA1,
                                            rsaKey,
                                            Signature.ALG_RSA_SHA_PKCS1,
                                            (byte) 0);

        // Step 3: Initialize the key based on the saved data (e.g.,
        // you'd normally read this data from persistent store
        // Since we're validating the signature, use the public key
        PublicKeyAlgorithm pka = jc.getJxtaPublicKeyAlgorithm();
        pka.setPublicKey(publicKeyData);

        // Step 4: Get the data from the document. In this case, we also
        // need the signature data
        byte[] data = new byte[1024];
        int length = getDataToSign(doc, data);

        Enumeration enum = doc.getChildren("Signature");
        byte[] signature = null;
        while (enum.hasMoreElements()) {
            Element element = (Element) enum.nextElement();
            String value = (String) element.getValue();
            byte[] enc = value.getBytes();
            signature = URLBase64.decode(enc, 0, enc.length);
        }

        // Step 5: Get the signature object, initialize it, and verify
        Signature s = jc.getJxtaSignature();
        s.init(Signature.MODE_VERIFY);
        boolean verified = s.verify(data, 0, length, signature, 0, signature.length);
        return verified;
    }

    public static void signMessage(StructuredDocument doc) throws Exception {

        // Step 1: Generate and initialize the necessary RSA key
        RSAKey rsaKey = (RSAKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA,
                                                KeyBuilder.LENGTH_RSA_512,
                                                false);

        // Step 2: Create a profile that can do signing
        JxtaCrypto jc = new JxtaCryptoSuite(JxtaCrypto.PROFILE_RSA_SHA1,
                                            rsaKey,
                                            Signature.ALG_RSA_SHA_PKCS1,
                                            (byte) 0);

        // Step 3: Initialize the key based on the saved data (e.g.,
        // you'd normally read this data from persistent store
        PublicKeyAlgorithm pka = jc.getJxtaPublicKeyAlgorithm();
        pka.setPublicKey(publicKeyData);
        pka.setPrivateKey(privateKeyData);


        // Step 4: Get the signature object, initialize it, and sign the data
        Signature s = jc.getJxtaSignature();
        s.init(Signature.MODE_SIGN);
        byte[] data = new byte[1024];
        int length = getDataToSign(doc, data);
        byte[] signature = s.sign(data, 0, length);

        // Step 5: Add the signature to the document
        Element el = doc.createElement("Signature", 
                        new String(URLBase64.encode(signature)));
        doc.appendChild(el);
    }

    public static void main(String[] args) throws Exception {
        // Step 1: Generate and initialize the necessary RSA key
        RSAKey rsaKey = (RSAKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA,
                                                KeyBuilder.LENGTH_RSA_512,
                                                false);

        // Step 2: Create a profile that can do signing
        JxtaCrypto jc = new JxtaCryptoSuite(JxtaCrypto.PROFILE_RSA_SHA1,
                                            rsaKey,
                                            Signature.ALG_RSA_SHA_PKCS1, (byte) 0);

        // Step 3: Complete the initialization of the keys
        PublicKeyAlgorithm pka = jc.getJxtaPublicKeyAlgorithm();
        pka.setPublicKey();
        pka.setPrivateKey();

        // Step 4: Save the public/private key data. Normally, these would
        // go into persistent storage, and the HungyPeer would be sent
        // the public key
        publicKeyData = (RSAPublickeyData) pka.getPublickey();
        privateKeyData = (RSAPrivatekeyData) pka.getPrivatekey();

        // Step 5: Create the document, just as we did in the RestoPeer
        StructuredDocument bid =
            StructuredDocumentFactory.newStructuredDocument(
                        new MimeMediaType("text", "xml"),
                        "RestoNet:Bid");
        Element el = bid.createElement("Brand", "Chez JXTA");
        bid.appendChild(el);
        el = bid.createElement("Price", "$1.00");
        bid.appendChild(el);

        // Step 6: Sign the document
        signMessage(bid);

        // Step 7: Validate the document
        // We'd normally transmit the document and the recipient
        // would validate its signature. We'll just do that in place
        boolean valid = validateMessage(bid);

        System.out.println("Signature was " + ((valid) ? "ok" : "invalid"));
    }
}

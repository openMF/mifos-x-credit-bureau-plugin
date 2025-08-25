package org.mifos.creditbureau.service.CirculoDeCredito;

import org.mifos.creditbureau.service.EncryptionService;
import org.mifos.creditbureau.service.registration.CreditBureauRegistrationReadImplService;
import org.springframework.stereotype.Service;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import org.apache.commons.codec.binary.Hex;


@Service
public class SignatureService {

    private final EncryptionService encryptionService;
    private final CreditBureauRegistrationReadImplService creditBureauRegistrationReadService;

    public SignatureService(EncryptionService encryptionService, CreditBureauRegistrationReadImplService creditBureauRegistrationReadService) {
        this.encryptionService = encryptionService;
        this.creditBureauRegistrationReadService = creditBureauRegistrationReadService;
        Security.addProvider(new BouncyCastleProvider());
    }

    //Take the private_key string and turn it into a PrivateKey object.
    //based on the postman code, the private_key is raw hex scalar
    private PrivateKey buildPrivateKey(String hexKey) throws Exception {
        BigInteger d = new BigInteger(hexKey, 16);
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp384r1");
        ECPrivateKeySpec privateSpec = new ECPrivateKeySpec(d, ecSpec);
        KeyFactory kf = KeyFactory.getInstance("EC", "BC");
        return kf.generatePrivate(privateSpec);
    }
    //Sign the request body with SHA256withECDSA and encode the signature
    private String signRequestBody(PrivateKey privateKey, String requestBody) throws Exception {
        Signature ecdsa = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsa.initSign(privateKey);
        ecdsa.update(requestBody.getBytes(StandardCharsets.UTF_8));
        byte[] signature = ecdsa.sign();
        return Hex.encodeHexString(signature);
    }

   //build the headers
   Map<String, String> buildHeaders(Long creditBureauId, String requestBody) throws Exception {
        //1. decrypt the keys from DB
        Map<String, String> keys = creditBureauRegistrationReadService.getRegistrationParamMap(creditBureauId);
        String apiKey = keys.get("x-api-key");

        String privateKey = keys.get("private_key");

        //2. Convert private key to hex
        PrivateKey pk = buildPrivateKey(privateKey);

        //3. Sign request body
        String signature = signRequestBody(pk, requestBody);

        //4. Return heeaders
        return Map.of("x-api-key", apiKey,
                "x-signature", signature,
                "Content-Type", "application/json");
    }


}

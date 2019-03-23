////////////////////////////////////////////////////////////////////
// WARNING - This software uses the real Tezos Betanet blockchain.
//           Use it with caution.
////////////////////////////////////////////////////////////////////

package milfont.com.tezosj.data;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import static milfont.com.tezosj.helper.Encoder.HEX;
import static milfont.com.tezosj.helper.Constants.UTEZ;
import static milfont.com.tezosj.helper.Constants.OPERATION_KIND_TRANSACTION;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.lang.Object;
import milfont.com.tezosj.helper.Base58Check;
import milfont.com.tezosj.helper.Global;
import milfont.com.tezosj.helper.MySodium;
import milfont.com.tezosj.model.EncKeys;
import milfont.com.tezosj.model.SignedOperationGroup;
import milfont.com.tezosj.model.TezosWallet;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class TezosGateway
{
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType textPlainMT = MediaType.parse("text/plain; charset=utf-8");
    private static final int HTTP_TIMEOUT = 20;
    private static MySodium sodium = null;

    public TezosGateway()
    {
        Random rand = new Random();
        int n = rand.nextInt(1000000) + 1;
        TezosGateway.sodium = new MySodium(String.valueOf(n));
    }
    
    public static MySodium getSodium()
    {
        return sodium;
    }

    // Sends request for Tezos node.
    private JSONObject query(String endpoint, String data) throws Exception
    {
        JSONObject result = null;
        Request request = null;
        Proxy proxy=null;
        SSLContext sslcontext = null;
        OkHttpClient client = new OkHttpClient();
        Builder myBuilder = client.newBuilder();
        
        final MediaType MEDIA_PLAIN_TEXT_JSON = MediaType.parse("application/json");
        String DEFAULT_PROVIDER = Global.defaultProvider;

        if (data != null)
        {
            request = new Request.Builder()
                        .url(DEFAULT_PROVIDER + endpoint)
                        .addHeader("Content-Type", "text/plain")
                        .post(RequestBody.create(MEDIA_PLAIN_TEXT_JSON, data.getBytes()))
                        .build();
        } else {
            request = new Request.Builder()
                        .url(DEFAULT_PROVIDER + endpoint)
                        .build();
        }

        // If user specified to ignore invalid certificates.
        if (Global.ignoreInvalidCertificates)
        {
           sslcontext = SSLContext.getInstance("TLS");
           sslcontext.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
           }}, new java.security.SecureRandom());
        
           myBuilder.sslSocketFactory(sslcontext.getSocketFactory());  // To ignore an invalid certificate.
        }

        // If user specified a proxy host.
        if ((Global.proxyHost.length() > 0)&&(Global.proxyPort.length() > 0))
        {
           proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Global.proxyHost, Integer.parseInt(Global.proxyPort)));
           myBuilder.proxy(proxy);  // If behind a firewall/proxy.
        }

        // Constructs the builder;
        myBuilder.connectTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
              .writeTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
              .readTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
              .build();

        try
        {
            Response response = client.newCall(request).execute();
            String strResponse = response.body().string();

            if (isJSONObject(strResponse))
            {
                result = new JSONObject(strResponse);
            }
            else if (isJSONArray(strResponse))
            {
                result = new JSONObject();
                result.put("result", new JSONArray(strResponse));
            }
            else
            {
                // If response is not a JSONObject nor JSONArray...
                // (can be a primitive).
                result = new JSONObject();
                result.put("result", strResponse);
            }

        }
        catch (Exception e)
        {
            // If there is a real error...
            e.printStackTrace();
            result = new JSONObject();
            result.put("result", e.toString());
        }

        return result;
    }

    // RPC methods.

    public JSONObject getHead() throws Exception
    {
        return query("/chains/main/blocks/head", null);
    }

    public JSONObject getAccountManagerForBlock(String blockHash, String accountID) throws Exception
    {
       JSONObject result = query("/chains/main/blocks/" + blockHash + "/context/contracts/" + accountID + "/manager_key", null);
    
       return result;
    }
        
    // Gets the balance for a given address.
    public JSONObject getBalance(String address) throws Exception
    {
        return query("/chains/main/blocks/head/context/contracts/" + address + "/balance", null);
    }

    // Prepares and sends an operation to the Tezos node.
    private JSONObject sendOperation(JSONArray operations, EncKeys encKeys) throws Exception
    {
        JSONObject result = new JSONObject();
        JSONObject head = new JSONObject();
        String forgedOperationGroup = "";

        head = (JSONObject) query("/chains/main/blocks/head/header", null);
        forgedOperationGroup = forgeOperations(head, operations);

        SignedOperationGroup signedOpGroup = signOperationGroup(forgedOperationGroup, encKeys);
        String operationGroupHash = computeOperationHash(signedOpGroup);
        JSONObject appliedOp = applyOperation(head, operations, operationGroupHash, forgedOperationGroup, signedOpGroup);
        JSONObject opResult = checkAppliedOperationResults(appliedOp);

        if (opResult.get("result").toString().length() == 0)
        {
            JSONObject injectedOperation = injectOperation(signedOpGroup);
            if (isJSONArray(injectedOperation.toString()))
            {
                if (((JSONObject)((JSONArray)injectedOperation.get("result")).get(0)).has("error"))
                {
                    String err = (String) ((JSONObject)((JSONArray)injectedOperation.get("result")).get(0)).get("error");
                    String reason = "There were errors: '" + err + "'";
                    
                    result.put("result", reason);
                }
                else
                {
                    result.put("result", "");
                }

            }
            else if (isJSONObject(injectedOperation.toString()))
            {
                if (injectedOperation.has("result"))
                {
                    if (isJSONArray(injectedOperation.get("result").toString()))
                    {
                        if (((JSONObject)((JSONArray)injectedOperation.get("result")).get(0)).has("error"))
                        {
                            String err = (String) ((JSONObject)((JSONArray)injectedOperation.get("result")).get(0)).get("error");
                            String reason = "There were errors: '" + err + "'";
                            
                            result.put("result", reason);
                        }
                        else
                        {
                            result.put("result", "");
                        }
                    }
                    else
                    {
                       result.put("result", injectedOperation.get("result"));
                    }
                }
                else
                {
                    result.put("result", "There were errors.");
                }
            }
        }
        else
        {
            result.put("result", opResult.get("result").toString());
        }

        return result;
    }

    // Sends a transaction to the Tezos node.
    public JSONObject sendTransaction(String from, String to, BigDecimal amount, BigDecimal fee, String gasLimit, String storageLimit, EncKeys encKeys) throws Exception
    {
        JSONObject result = new JSONObject();

        BigDecimal roundedAmount = amount.setScale(5, BigDecimal.ROUND_HALF_UP);
        BigDecimal roundedFee = fee.setScale(5, BigDecimal.ROUND_HALF_UP);
        JSONArray operations = new JSONArray();
        JSONObject revealOperation = new JSONObject();
        JSONObject transaction = new JSONObject();
        JSONObject head = new JSONObject();
        JSONObject account = new JSONObject();
        JSONObject parameters = new JSONObject();
        JSONArray argsArray = new JSONArray();
        int counter = 0;

        // Check if address has enough funds to do the transfer operation.
        JSONObject balance = getBalance(from);
        if (balance.has("result"))
        {
            BigDecimal bdAmount = amount.multiply(BigDecimal.valueOf(UTEZ));
            BigDecimal total = new BigDecimal(((balance.getString("result").replaceAll("\\n", "")).replaceAll("\"", "").replaceAll("'", "")));
            
            if (total.compareTo(bdAmount) < 0) // Returns -1 if value is less than amount.
            {
               // Not enough funds to do the transfer.
                JSONObject returned = new JSONObject();
                returned.put("result", "{ \"result\":\"error\", \"kind\":\"TezosJ_SDK_exception\", \"id\": \"Not enough funds\" }");

                return returned;
            }
        }
        
        if (gasLimit == null || gasLimit.length() == 0 || gasLimit.equals("0"))
        {
            gasLimit = "11000";
        }

        if (storageLimit == null || storageLimit.length() == 0)
        {
            storageLimit = "300";
        }

        head = new JSONObject(query("/chains/main/blocks/head/header", null).toString());
        account = getAccountForBlock(head.get("hash").toString(), from);
        counter = Integer.parseInt(account.get("counter").toString());

        // Append Reveal Operation if needed.
        revealOperation = appendRevealOperation(head, encKeys, from, (counter));

        if (revealOperation != null)
        {
            operations.put(revealOperation);
            counter = counter + 1;
        }
        
        transaction.put("destination", to);
        transaction.put("amount", (String.valueOf(roundedAmount.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
        transaction.put("storage_limit", storageLimit);
        transaction.put("gas_limit", gasLimit);
        transaction.put("counter", String.valueOf(counter + 1));
        transaction.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
        transaction.put("source", from);
        transaction.put("kind", OPERATION_KIND_TRANSACTION);
        parameters.put("prim", "Unit");
        parameters.put("args", argsArray);
        transaction.put("parameters", parameters);

        operations.put(transaction);
               
        result = sendOperation(operations, encKeys);

        return result;
    }

    private SignedOperationGroup signOperationGroup(String forgedOperation, EncKeys encKeys) throws Exception
    {
        JSONObject signed = sign(HEX.decode(forgedOperation), encKeys, "03");

        // Prepares the object to be returned.
        byte[] workBytes = ArrayUtils.addAll(HEX.decode(forgedOperation), HEX.decode((String) signed.get("sig")));
        return new SignedOperationGroup(workBytes, (String) signed.get("edsig"), (String) signed.get("sbytes"));
    }

    private String forgeOperations(JSONObject blockHead, JSONArray operations) throws Exception
    {
        JSONObject result = new JSONObject();
        result.put("branch", blockHead.get("hash"));
        result.put("contents", operations);

        return nodeForgeOperations(result.toString());
    }

    private String nodeForgeOperations(String opGroup) throws Exception
    {
        JSONObject response = (JSONObject) query("/chains/main/blocks/head/helpers/forge/operations", opGroup);
        String forgedOperation = (String) response.get("result");

        return ((forgedOperation.replaceAll("\\n", "")).replaceAll("\"", "").replaceAll("'", ""));
    }

    private JSONObject getAccountForBlock(String blockHash, String accountID) throws Exception
    {
        JSONObject result = new JSONObject();

        result = query("/chains/main/blocks/" + blockHash + "/context/contracts/" + accountID, null);

        return result;
    }

    private String computeOperationHash(SignedOperationGroup signedOpGroup) throws Exception
    {
        byte[] hash = new byte[32];
        int r = sodium.crypto_generichash(hash, hash.length, signedOpGroup.getTheBytes(), signedOpGroup.getTheBytes().length, signedOpGroup.getTheBytes(), 0);

        return Base58Check.encode(hash);
    }

    private JSONObject nodeApplyOperation(JSONArray payload) throws Exception
    {
        return query("/chains/main/blocks/head/helpers/preapply/operations", payload.toString());
    }

    private JSONObject applyOperation(JSONObject head, JSONArray operations, String operationGroupHash, String forgedOperationGroup, SignedOperationGroup signedOpGroup) throws Exception
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("protocol", head.get("protocol"));
        jsonObject.put("branch", head.get("hash"));
        jsonObject.put("contents", operations);
        jsonObject.put("signature", signedOpGroup.getSignature());

        JSONArray payload = new JSONArray();
        payload.put(jsonObject);

        return nodeApplyOperation(payload);
    }

    private JSONObject checkAppliedOperationResults(JSONObject appliedOp) throws Exception
    {
        JSONObject returned = new JSONObject();
        boolean errors = false;
        String reason = "";

        String[] validAppliedKinds = new String[]{"activate_account", "reveal", "transaction", "origination", "delegation"};

        String firstApplied = appliedOp.toString().replaceAll("\\\\n", "").replaceAll("\\\\", "");
        JSONArray result = new JSONArray(new JSONObject(firstApplied).get("result").toString());
        JSONObject first = (JSONObject) result.get(0);
        
        if (isJSONObject(first.toString()))
        {
            // Check for error.
            if (first.has("kind") && first.has("id"))
            {
                errors = true;
                reason = "There were errors: kind '" + first.getString("kind") + "' id '" + first.getString("id") + "'";
            }
        }
        else if (isJSONArray(first.toString()))
        {
            // Loop through contents and check for errors.
            int elements = ((JSONArray)first.get("contents")).length();
            String element = "";
            for(int i = 0; i < elements; i++)
            {
                JSONObject operation_result = ( (JSONObject) ((JSONObject) (((JSONObject) (((JSONArray) first.get("contents")).get(i))).get("metadata"))).get("operation_result"));
                element = ((JSONObject)operation_result).getString("status");
                if (element.equals("failed") == true)
                {
                    errors = true;
                    if (operation_result.has("errors"))
                    {
                       JSONObject err = (JSONObject) ((JSONArray) operation_result.get("errors")).get(0);
                       reason = "There were errors: kind '" + err.getString("kind") + "' id '" + err.getString("id") + "'";
                    }
                    break;
                }
            }
        }
 
        if (errors)
        {
            returned.put("result", reason);
        }
        else
        {
            returned.put("result", "");
        }
        return returned;
    }
 
    private JSONObject appendRevealOperation(JSONObject blockHead, EncKeys encKeys, String pkh, int counter) throws Exception
    {
        // Create new JSON object for the reveal operation.
        JSONObject revealOp = new JSONObject();
        
        // Get public key from encKeys.
        byte[] bytePk = encKeys.getEncPublicKey();
        byte[] decPkBytes = decryptBytes(bytePk, TezosWallet.getEncryptionKey(encKeys));
		String publicKey = new String(decPkBytes);
        // If Manager key is not revealed for account...
        if(!isManagerKeyRevealedForAccount(blockHead, pkh))
        {
            BigDecimal fee = new BigDecimal("0.001267");
            BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
    		revealOp.put("kind", "reveal");
    		revealOp.put("source", pkh);
    		revealOp.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));  
    		revealOp.put("counter", String.valueOf(counter + 1));
    		revealOp.put("gas_limit", "11000");
    		revealOp.put("storage_limit", "300");
    		revealOp.put("public_key", publicKey);
        } else {
            revealOp = null;
        }
        
        return revealOp;
    }
    
    private boolean isManagerKeyRevealedForAccount(JSONObject blockHead, String pkh) throws Exception
    {
       String blockHeadHash = blockHead.getString("hash");
       
       return getAccountManagerForBlock(blockHeadHash, pkh).has("key");
    }
    
    private JSONObject injectOperation(SignedOperationGroup signedOpGroup) throws Exception
    {
        String payload = signedOpGroup.getSbytes();
        return nodeInjectOperation("\"" + payload + "\"");
    }

    private JSONObject nodeInjectOperation(String payload) throws Exception
    {
        JSONObject result = query("/injection/operation?chain=main", payload);

        return result;
    }

    public JSONObject sign(byte[] bytes, EncKeys keys, String watermark) throws Exception
    {
        // Access wallet keys to have authorization to perform the operation.
        byte[] byteSk = keys.getEncPrivateKey();
        byte[] decSkBytes = decryptBytes(byteSk, TezosWallet.getEncryptionKey(keys));

        // First, we remove the edsk prefix from the decoded private key bytes.
        byte[] edskPrefix = {(byte) 43, (byte) 246, (byte) 78, (byte) 7};
        byte[] decodedSk = Base58Check.decode(new String(decSkBytes));
        byte[] privateKeyBytes = Arrays.copyOfRange(decodedSk, edskPrefix.length, decodedSk.length);

        // Then we create a work array and check if the watermark parameter has been passed.
        byte[] workBytes = ArrayUtils.addAll(bytes);

        if (watermark != null) {
            byte[] wmBytes = HEX.decode(watermark);
            workBytes = ArrayUtils.addAll(wmBytes, workBytes);
        }

        // Now we hash the combination of: watermark (if exists) + the bytes passed in parameters.
        // The result will end up in the sig variable.
        byte[] hashedWorkBytes = new byte[32];
        int rc = sodium.crypto_generichash(hashedWorkBytes, hashedWorkBytes.length, workBytes, workBytes.length, workBytes, 0);

        byte[] sig = new byte[64];
        int r = sodium.crypto_sign_detached(sig, null, hashedWorkBytes, hashedWorkBytes.length, privateKeyBytes);

        // To create the edsig, we need to concatenate the edsig prefix with the sig and then encode it.
        // The sbytes will be the concatenation of bytes (in hex) + sig (in hex).
        byte[] edsigPrefix = {9, (byte) 245, (byte) 205, (byte) 134, 18};
        byte[] edsigPrefixedSig = new byte[edsigPrefix.length + sig.length];
        edsigPrefixedSig = ArrayUtils.addAll(edsigPrefix, sig);
        String edsig = Base58Check.encode(edsigPrefixedSig);
        String sbytes = HEX.encode(bytes) + HEX.encode(sig);

        // Now, with all needed values ready, we create and deliver the response.
        JSONObject response = new JSONObject();
        response.put("bytes", HEX.encode(bytes));
        response.put("sig", HEX.encode(sig));
        response.put("edsig", edsig);
        response.put("sbytes", sbytes);

        return response;
    }

    // Tests if a string is a valid JSON.
    private boolean isJSONObject(String myStr)
    {
        try {
            JSONObject testJSON = new JSONObject(myStr);
            return testJSON != null;
        } catch (JSONException e) {
            return false;
        }
    }

    // Tests if s string is a valid JSON Array.
    private boolean isJSONArray(String myStr)
    {
        try
        {
            JSONArray testJSONArray = new JSONArray(myStr);
            return testJSONArray != null;
        }
        catch (JSONException e)
        {
            return false;
        }
    }

    // Decryption routine.
    private static byte[] decryptBytes(byte[] encrypted, byte[] key)
    {
        try
        {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            return cipher.doFinal(encrypted);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}

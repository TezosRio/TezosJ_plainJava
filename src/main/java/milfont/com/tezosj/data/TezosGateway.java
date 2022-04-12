package milfont.com.tezosj.data;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static milfont.com.tezosj.helper.Encoder.HEX;
import static milfont.com.tezosj.helper.Constants.UTEZ;
import static milfont.com.tezosj.helper.Constants.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Object;

import milfont.com.tezosj.helper.Base58Check;
import milfont.com.tezosj.helper.Global;
import milfont.com.tezosj.helper.MySodium;
import milfont.com.tezosj.model.BatchTransactionItem;
import milfont.com.tezosj.model.BatchTransactionItemIndexSorter;
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
   private static final Integer HTTP_TIMEOUT = 20;
   private static MySodium sodium = null;

   public TezosGateway()
   {
      SecureRandom rand = new SecureRandom();
      int n = rand.nextInt(1000000) + 1;
      TezosGateway.sodium = new MySodium(String.valueOf(n));
   }

   public static MySodium getSodium()
   {
      return sodium;
   }

   // Sends request for Tezos node.
   private Object query(String endpoint, String data) throws Exception
   {
      JSONObject result = null;
      Boolean methodPost = false;
      Request request = null;
      Proxy proxy = null;
      SSLContext sslcontext = null;

      // Initializes a single shared instance of okHttp client (and builder).
      Global.initOkhttp();
      OkHttpClient client = Global.myOkhttpClient;
      Builder myBuilder = Global.myOkhttpBuilder;

      final MediaType MEDIA_PLAIN_TEXT_JSON = MediaType.parse("application/json");
      String DEFAULT_PROVIDER = Global.defaultProvider;
      RequestBody body = RequestBody.create(textPlainMT, DEFAULT_PROVIDER + endpoint);

      if(data != null)
      {
         methodPost = true;
         body = RequestBody.create(MEDIA_PLAIN_TEXT_JSON, data.getBytes());
      }

      if(methodPost == false)
      {
         request = new Request.Builder().url(DEFAULT_PROVIDER + endpoint).build();
      }
      else
      {
         request = new Request.Builder().url(DEFAULT_PROVIDER + endpoint).addHeader("Content-Type", "text/plain")
               .post(body).build();
      }

      // If user specified to ignore invalid certificates.
      if(Global.ignoreInvalidCertificates)
      {
         sslcontext = SSLContext.getInstance("TLS");
         sslcontext.init(null, new TrustManager[]
         { new X509TrustManager()
         {
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
            {
            }

            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
            {
            }

            public X509Certificate[] getAcceptedIssuers()
            {
               return new X509Certificate[0];
            }
         } }, new java.security.SecureRandom());

         myBuilder.sslSocketFactory(sslcontext.getSocketFactory()); // To ignore an invalid certificate.
      }

      // If user specified a proxy host.
      if((Global.proxyHost.length() > 0) && (Global.proxyPort.length() > 0))
      {
         proxy = new Proxy(Proxy.Type.HTTP,
               new InetSocketAddress(Global.proxyHost, Integer.parseInt(Global.proxyPort)));
         myBuilder.proxy(proxy); // If behind a firewall/proxy.
      }

      // Constructs the builder;
      myBuilder.connectTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS).writeTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS).build();

      try
      {
         Response response = client.newCall(request).execute();
         String strResponse = response.body().string();

         if(isJSONObject(strResponse))
         {
            result = new JSONObject(strResponse);
         }
         else
         {
            if(isJSONArray(strResponse))
            {
               JSONArray myJSONArray = new JSONArray(strResponse);
               result = new JSONObject();
               result.put("result", myJSONArray);
            }
            else
            {
               // If response is not a JSONObject nor JSONArray...
               // (can be a primitive).
               result = new JSONObject();
               result.put("result", strResponse);
            }
         }
      } catch (Exception e)
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
      return (JSONObject) query("/chains/main/blocks/head~2", null);
   }

   public JSONObject getAccountManagerForBlock(String blockHash, String accountID) throws Exception
   {
      JSONObject result = (JSONObject) query(
            "/chains/main/blocks/" + blockHash + "/context/contracts/" + accountID + "/manager_key", null);

      return result;
   }

   // Gets the balance for a given address.
   public JSONObject getBalance(String address) throws Exception
   {
      return (JSONObject) query("/chains/main/blocks/head~2/context/contracts/" + address + "/balance", null);
   }

   // Prepares and sends an operation to the Tezos node.
   private JSONObject sendOperation(JSONArray operations, EncKeys encKeys) throws Exception
   {
      JSONObject result = new JSONObject();
      JSONObject head = new JSONObject();
      String forgedOperationGroup = "";

      head = (JSONObject) query("/chains/main/blocks/head~2/header", null);
      forgedOperationGroup = forgeOperations(head, operations);

      // Check for errors.
      if(forgedOperationGroup.toLowerCase().contains("failed") || forgedOperationGroup.toLowerCase().contains("unexpected")
            || forgedOperationGroup.toLowerCase().contains("missing") || forgedOperationGroup.toLowerCase().contains("error"))
      {
         throw new Exception("Error while forging operation : " + forgedOperationGroup);
      }
      
      SignedOperationGroup signedOpGroup = signOperationGroup(forgedOperationGroup, encKeys);
      
      if (signedOpGroup == null) // User cancelled the operation.
      {
         result.put("result", "There were errors: 'User has cancelled the operation'");
         return result;
      }
      else
      {
         
         String operationGroupHash = computeOperationHash(signedOpGroup);
         JSONObject appliedOp = applyOperation(head, operations, operationGroupHash, forgedOperationGroup, signedOpGroup);
         JSONObject opResult = checkAppliedOperationResults(appliedOp);
   
         if(opResult.get("result").toString().length() == 0)
         {
            JSONObject injectedOperation = injectOperation(signedOpGroup);
            if(isJSONArray(injectedOperation.toString()))
            {
               if(((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).has("error"))
               {
                  String err = (String) ((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).get("error");
                  String reason = "There were errors: '" + err + "'";
   
                  result.put("result", reason);
               }
               else
               {
                  result.put("result", "");
               }
               if(((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).has("Error"))
               {
                  String err = (String) ((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).get("Error");
                  String reason = "There were errors: '" + err + "'";
   
                  result.put("result", reason);
               }
               else
               {
                  result.put("result", "");
               }
   
            }
            else if(isJSONObject(injectedOperation.toString()))
            {
               if(injectedOperation.has("result"))
               {
                  if(isJSONArray(injectedOperation.get("result").toString()))
                  {
                     if(((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).has("error"))
                     {
                        String err = (String) ((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0))
                              .get("error");
                        String reason = "There were errors: '" + err + "'";
   
                        result.put("result", reason);
                     }
                     else if(((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).has("kind"))
                     {
                        if(((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).has("msg"))
                        {
                           String err = (String) ((JSONObject) ((JSONArray) injectedOperation.get("result")).get(0)).get("msg");
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
      }
      return result;
   }

   // Call Tezos RUN_OPERATION.
   private JSONObject callRunOperation(JSONArray operations, EncKeys encKeys) throws Exception
   {
      JSONObject result = new JSONObject();
      JSONObject head = new JSONObject();
      String forgedOperationGroup = "";

      head = (JSONObject) query("/chains/main/blocks/head~2/header", null);
      forgedOperationGroup = forgeOperations(head, operations);

      // Check for errors.
      if(forgedOperationGroup.toLowerCase().contains("failed") || forgedOperationGroup.toLowerCase().contains("unexpected")
            || forgedOperationGroup.toLowerCase().contains("missing") || forgedOperationGroup.toLowerCase().contains("error"))
      {
         throw new Exception("Error while forging operation : " + forgedOperationGroup);
      }
      
      SignedOperationGroup signedOpGroup = signOperationGroupSimulation(forgedOperationGroup, null);
      
      if (signedOpGroup == null) // User cancelled the operation.
      {
         result.put("result", "There were errors: 'User has cancelled the operation'");
         return result;
      }
      else
      {
         
         String operationGroupHash = computeOperationHash(signedOpGroup);
         
         // Call RUN_OPERATIONS.
         JSONObject runOp = runOperation(head, operations, operationGroupHash, forgedOperationGroup, signedOpGroup);
            
         result = runOp;
      }
      return result;
   }

   
   // Sends a transaction to the Tezos node.
   public JSONObject sendTransaction(String from, String to, BigDecimal amount, BigDecimal fee, String gasLimit,
                                     String storageLimit, EncKeys encKeys)
         throws Exception
   {
      JSONObject result = new JSONObject();

      BigDecimal roundedAmount = amount.setScale(6, BigDecimal.ROUND_HALF_UP);
      BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
      JSONArray operations = new JSONArray();
      JSONObject revealOperation = new JSONObject();
      JSONObject transaction = new JSONObject();
      JSONObject head = new JSONObject();
      JSONObject account = new JSONObject();
      JSONObject parameters = new JSONObject();
      JSONArray argsArray = new JSONArray();
      Integer counter = 0;

      // Check if address has enough funds to do the transfer operation.
      JSONObject balance = getBalance(from);

      if(balance.has("result"))
      {
         BigDecimal bdAmount = amount.multiply(BigDecimal.valueOf(UTEZ));
         BigDecimal total = new BigDecimal(
               ((balance.getString("result").replaceAll("\\n", "")).replaceAll("\"", "").replaceAll("'", "")));

         if(total.compareTo(bdAmount) < 0) // Returns -1 if value is less than amount.
         {
            // Not enough funds to do the transfer.
            JSONObject returned = new JSONObject();
            returned.put("result",
                  "{ \"result\":\"error\", \"kind\":\"TezosJ_SDK_exception\", \"id\": \"Not enough funds\" }");

            return returned;
         }
      }

      if(gasLimit == null)
      {
         gasLimit = "15400";
      }
      else
      {
         if((gasLimit.length() == 0) || (gasLimit.equals("0")))
         {
            gasLimit = "15400";
         }
      }

      if(storageLimit == null)
      {
         storageLimit = "300";
      }
      else
      {
         if(storageLimit.length() == 0)
         {
            storageLimit = "300";
         }
      }

      head = new JSONObject(query("/chains/main/blocks/head~2/header", null).toString());
      account = getAccountForBlock(head.get("hash").toString(), from);
      counter = Integer.parseInt(account.get("counter").toString());

      // Append Reveal Operation if needed.
      revealOperation = appendRevealOperation(head, encKeys, from, (counter));

      if(revealOperation != null)
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

      operations.put(transaction);

      result = (JSONObject) sendOperation(operations, encKeys);

      return result;
   }

   // Sends an activate operation to the Tezos node.
   public JSONObject activate(String addressToActivate, String secret, EncKeys encKeys) throws Exception
   {
      // This method will activate a Tezos address. Either through the use of a given "secret"
      // (like from a faucet or from the fundraiser), or (if an empty "secret" is provided) through 
      // sending a tiny amount of tez to the destination address (addressToActivate).
      
      JSONObject result = new JSONObject();

      if (secret.isEmpty() == false)
      {
      
         JSONArray operations = new JSONArray();
         JSONObject transaction = new JSONObject();
         
         transaction.put("kind", "activate_account");
         transaction.put("pkh", addressToActivate);
         transaction.put("secret", secret);
         
         operations.put(transaction);
   
         result = (JSONObject) sendOperation(operations, encKeys);
      }
      else
      {
         BigDecimal amount = new BigDecimal("0.002490");
         BigDecimal fee = new BigDecimal("0.002490");
     
         // Get public key hash from encKeys.
         byte[] bytePk = encKeys.getEncPublicKeyHash();
         byte[] decPkBytes = decryptBytes(bytePk, TezosWallet.getEncryptionKey(encKeys));

         StringBuilder builder2 = new StringBuilder();
         for(byte decPkByte : decPkBytes)
         {
            builder2.append((char) (decPkByte));
         }
         String from = builder2.toString();
         
         result = (JSONObject) sendTransaction(from, addressToActivate, amount, fee, "", "", encKeys);         
      }
      
      return result;
   }

   // Sends a reveal operation to the Tezos node.
   public JSONObject reveal(String publicKeyHash, String publicKey, EncKeys encKeys) throws Exception
   {
      JSONObject result = new JSONObject(); 
      JSONArray operations = new JSONArray();
      JSONObject transaction = new JSONObject();
      JSONObject head = new JSONObject();
      JSONObject account = new JSONObject();
      Integer counter = 0;

      head = new JSONObject(query("/chains/main/blocks/head~2/header", null).toString());
      account = getAccountForBlock(head.get("hash").toString(), publicKeyHash);
      counter = Integer.parseInt(account.get("counter").toString());
      
      BigDecimal fee = new BigDecimal("0.002490");
      BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
      
      transaction.put("kind", "reveal");
      transaction.put("source", publicKeyHash);
      transaction.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
      transaction.put("counter", String.valueOf(counter + 1));
      transaction.put("gas_limit", "15400");
      transaction.put("storage_limit", "300");
      transaction.put("public_key", publicKey);
      
      operations.put(transaction);

      result = (JSONObject) sendOperation(operations, encKeys);

      return result;
   }

   
   private SignedOperationGroup signOperationGroup(String forgedOperation, EncKeys encKeys) throws Exception
   {
      JSONObject signed =null;
      
      if((Global.ledgerDerivationPath.isEmpty()==false)&&(Global.ledgerTezosFolderPath.isEmpty()==false))
      {
         System.out.println(Global.CONFIRM_WITH_LEDGER_MESSAGE);
         signed = signWithLedger(HEX.decode(forgedOperation), "03");
      }
      else
      {
         // Traditional signing.
         signed = sign(HEX.decode(forgedOperation), encKeys, "03");   
      }
      
      if (signed == null) // User cancelled the operation.
      {
         return null;
      }
      else
      {
         // Prepares the object to be returned.
         byte[] workBytes = ArrayUtils.addAll(HEX.decode(forgedOperation), HEX.decode((String) signed.get("sig")));
         return new SignedOperationGroup(workBytes, (String) signed.get("edsig"), (String) signed.get("sbytes"));
      }
   }

   private SignedOperationGroup signOperationGroupSimulation(String forgedOperation, EncKeys encKeys) throws Exception
   {
      JSONObject signed = new JSONObject();

      byte[] bytes = HEX.decode(forgedOperation);
      byte[] sig = new byte[64];

      byte[] edsigPrefix = { 9, (byte) 245, (byte) 205, (byte) 134, 18 };
      byte[] edsigPrefixedSig = new byte[edsigPrefix.length + sig.length];
      edsigPrefixedSig = ArrayUtils.addAll(edsigPrefix, sig);
      String edsig = Base58Check.encode(edsigPrefixedSig);
      String sbytes = HEX.encode(bytes) + HEX.encode(sig);

      signed.put("bytes", HEX.encode(bytes));
      signed.put("sig", HEX.encode(sig));
      signed.put("edsig", edsig);
      signed.put("sbytes", sbytes);
      
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
      JSONObject response = (JSONObject) query("/chains/main/blocks/head~2/helpers/forge/operations", opGroup);
      String forgedOperation = (String) response.get("result");

      return ((forgedOperation.replaceAll("\\n", "")).replaceAll("\"", "").replaceAll("'", ""));

   }

   private JSONObject getAccountForBlock(String blockHash, String accountID) throws Exception
   {
      JSONObject result = new JSONObject();

      result = (JSONObject) query("/chains/main/blocks/" + blockHash + "/context/contracts/" + accountID, null);

      return result;
   }

   private String computeOperationHash(SignedOperationGroup signedOpGroup) throws Exception
   {
      byte[] hash = new byte[32];
      int r = sodium.crypto_generichash(hash, hash.length, signedOpGroup.getTheBytes(),
            signedOpGroup.getTheBytes().length, signedOpGroup.getTheBytes(), 0);

      return Base58Check.encode(hash);
   }

   private JSONObject nodeApplyOperation(JSONArray payload) throws Exception
   {
      return (JSONObject) query("/chains/main/blocks/head~2/helpers/preapply/operations", payload.toString());
   }

   private JSONObject nodeRunOperation(JSONArray payload, String chainId) throws Exception
   {
      JSONObject operation = new JSONObject();      
      operation.put("operation", payload.get(0));
      operation.put("chain_id", chainId);
      
      return (JSONObject) query("/chains/main/blocks/head~2/helpers/scripts/run_operation", operation.toString());
   }
   
   private JSONObject applyOperation(JSONObject head, JSONArray operations, String operationGroupHash,
                                     String forgedOperationGroup, SignedOperationGroup signedOpGroup)
         throws Exception
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

   private JSONObject runOperation(JSONObject head, JSONArray operations, String operationGroupHash,
                                   String forgedOperationGroup, SignedOperationGroup signedOpGroup)
         throws Exception
   {
      JSONObject jsonObject = new JSONObject();
      String chainId = head.get("chain_id").toString();
      jsonObject.put("branch", head.get("hash"));
      jsonObject.put("contents", operations);
      jsonObject.put("signature", signedOpGroup.getSignature());

      JSONArray payload = new JSONArray();
      payload.put(jsonObject);

      return nodeRunOperation(payload, chainId);
   }

   
   private JSONObject checkAppliedOperationResults(JSONObject appliedOp) throws Exception
   {
      JSONObject returned = new JSONObject();
      Boolean errors = false;
      String reason = "";

      String[] validAppliedKinds = new String[]
      { "activate_account", "reveal", "transaction", "origination", "delegation" };

      String firstApplied = appliedOp.toString().replaceAll("\\\\n", "").replaceAll("\\\\", "");
      JSONArray result = new JSONArray(new JSONObject(firstApplied).get("result").toString());
      JSONObject first = (JSONObject) result.get(0);

      if(isJSONObject(first.toString()))
      {
         // Check for error.
         if(first.has("kind") && first.has("id"))
         {
            errors = true;
            reason = "There were errors: kind '" + first.getString("kind") + "' id '" + first.getString("id") + "'";
         }
      }
      else if(isJSONArray(first.toString()))
      {
         // Loop through contents and check for errors.
         Integer elements = ((JSONArray) first.get("contents")).length();
         String element = "";
         for(Integer i = 0; i < elements; i++)
         {
            JSONObject operation_result = ((JSONObject) ((JSONObject) (((JSONObject) (((JSONArray) first
                  .get("contents")).get(i))).get("metadata"))).get("operation_result"));
            element = ((JSONObject) operation_result).getString("status");
            if(element.equals("failed") == true)
            {
               errors = true;
               if(operation_result.has("errors"))
               {
                  JSONObject err = (JSONObject) ((JSONArray) operation_result.get("errors")).get(0);
                  reason = "There were errors: kind '" + err.getString("kind") + "' id '" + err.getString("id") + "'";
               }
               break;
            }
         }
      }

      if(errors)
      {
         returned.put("result", reason);
      }
      else
      {
         returned.put("result", "");
      }
      return returned;
   }

   private JSONObject appendRevealOperation(JSONObject blockHead, EncKeys encKeys, String pkh, Integer counter)
         throws Exception
   {
      // Create new JSON object for the reveal operation.
      JSONObject revealOp = new JSONObject();

      // Get public key from encKeys.
      byte[] bytePk = encKeys.getEncPublicKey();
      byte[] decPkBytes = decryptBytes(bytePk, TezosWallet.getEncryptionKey(encKeys));

      StringBuilder builder2 = new StringBuilder();
      for(byte decPkByte : decPkBytes)
      {
         builder2.append((char) (decPkByte));
      }
      String publicKey = builder2.toString();
      // If Manager key is not revealed for account...
      if(!isManagerKeyRevealedForAccount(blockHead, pkh))
      {
         BigDecimal fee = new BigDecimal("0.002490");
         BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
         revealOp.put("kind", "reveal");
         revealOp.put("source", pkh);
         revealOp.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
         revealOp.put("counter", String.valueOf(counter + 1));
         revealOp.put("gas_limit", "15400");
         revealOp.put("storage_limit", "300");
         revealOp.put("public_key", publicKey);
      }
      else
      {
         revealOp = null;
      }

      return revealOp;
   }

   private boolean isManagerKeyRevealedForAccount(JSONObject blockHead, String pkh) throws Exception
   {
      Boolean result = false;
      String blockHeadHash = blockHead.getString("hash");
      String r = "";

      Boolean hasResult = getAccountManagerForBlock(blockHeadHash, pkh).has("result");

      if(hasResult)
      {
         r = (String) getAccountManagerForBlock(blockHeadHash, pkh).get("result");

         // Do some cleaning.
         r = r.replace("\"", "");
         r = r.replace("\n", "");
         r = r.trim();

         if(r.equals("null") == true)
         {
            result = false;
         }
         else
         {
            // Account already revealed.
            result = true;
         }
      }

      return result;
   }

   private JSONObject injectOperation(SignedOperationGroup signedOpGroup) throws Exception
   {
      String payload = signedOpGroup.getSbytes();
      return nodeInjectOperation("\"" + payload + "\"");
   }

   private JSONObject nodeInjectOperation(String payload) throws Exception
   {
      JSONObject result = (JSONObject) query("/injection/operation?chain=main", payload);

      return result;
   }

   public JSONObject sign(byte[] bytes, EncKeys keys, String watermark) throws Exception
   {
      JSONObject response = new JSONObject();

      // Access wallet keys to have authorization to perform the operation.
      byte[] byteSk = keys.getEncPrivateKey();     
      byte[] decSkBytes = decryptBytes(byteSk, TezosWallet.getEncryptionKey(keys));

      // First, we remove the edsk prefix from the decoded private key bytes.
      byte[] edskPrefix =
      { (byte) 43, (byte) 246, (byte) 78, (byte) 7 };
      byte[] decodedSk = Base58Check.decode(new String(decSkBytes));
      byte[] privateKeyBytes = Arrays.copyOfRange(decodedSk, edskPrefix.length, decodedSk.length);

      // Then we create a work array and check if the watermark parameter has been
      // passed.
      byte[] workBytes = ArrayUtils.addAll(bytes);

      if(watermark != null)
      {
         byte[] wmBytes = HEX.decode(watermark);
         workBytes = ArrayUtils.addAll(wmBytes, workBytes);
      }

      // Now we hash the combination of: watermark (if exists) + the bytes passed in
      // parameters.
      // The result will end up in the sig variable.
      byte[] hashedWorkBytes = new byte[32];
      int rc = sodium.crypto_generichash(hashedWorkBytes, hashedWorkBytes.length, workBytes, workBytes.length,
            workBytes, 0);

      byte[] sig = new byte[64];
      int r = sodium.crypto_sign_detached(sig, null, hashedWorkBytes, hashedWorkBytes.length, privateKeyBytes);

      // To create the edsig, we need to concatenate the edsig prefix with the sig and
      // then encode it.
      // The sbytes will be the concatenation of bytes (in hex) + sig (in hex).
      byte[] edsigPrefix =
      { 9, (byte) 245, (byte) 205, (byte) 134, 18 };
      byte[] edsigPrefixedSig = new byte[edsigPrefix.length + sig.length];
      edsigPrefixedSig = ArrayUtils.addAll(edsigPrefix, sig);
      String edsig = Base58Check.encode(edsigPrefixedSig);
      String sbytes = HEX.encode(bytes) + HEX.encode(sig);

      // Now, with all needed values ready, we create and deliver the response.
      response.put("bytes", HEX.encode(bytes));
      response.put("sig", HEX.encode(sig));
      response.put("edsig", edsig);
      response.put("sbytes", sbytes);
      
      return response;
      
   }

   public JSONObject signWithLedger(byte[] bytes, String watermark) throws Exception
   {
      JSONObject response = new JSONObject();
      String watermarkedForgedOperationBytesHex = "";

      byte[] workBytes = ArrayUtils.addAll(bytes);

      if(watermark != null)
      {
         byte[] wmBytes = HEX.decode(watermark);
         workBytes = ArrayUtils.addAll(wmBytes, workBytes);
      }

      watermarkedForgedOperationBytesHex = HEX.encode(workBytes);
      
      // There is a Ledger hardware wallet configured. Signing will be done with it.
      Runtime rt = Runtime.getRuntime();
      String[] commands = { Global.ledgerTezosFolderPath + Global.ledgerTezosFilePath, Global.ledgerDerivationPath, watermarkedForgedOperationBytesHex };

      try
      {
         Process proc = rt.exec(commands);
   
         BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
         BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
   
         // read the output from the command
         String s = "", signature="", error = "";
         while ((s = stdInput.readLine()) != null)
         {
          signature=signature + s;
         }
            
         JSONObject jsonObject = new JSONObject(signature);
         String ledgerSig = jsonObject.getString("signature"); 
         
         String r = "";
         while ((r = stdError.readLine()) != null)
         {
            error = error + r;
         }
   
         byte[] sig = new byte[64];
         sig = HEX.decode(ledgerSig);
   
   
         // To create the edsig, we need to concatenate the edsig prefix with the sig and
         // then encode it.
         byte[] edsigPrefix =
         { 9, (byte) 245, (byte) 205, (byte) 134, 18 };
         byte[] edsigPrefixedSig = new byte[edsigPrefix.length + sig.length];
         edsigPrefixedSig = ArrayUtils.addAll(edsigPrefix, sig);
         String edsig = Base58Check.encode(edsigPrefixedSig);
   
         // The sbytes will be the concatenation of bytes (in hex) + sig (in hex).
         String sbytes = HEX.encode(bytes) + HEX.encode(sig);
         
         // Now, with all needed values ready, we create and deliver the response.
         response.put("bytes", HEX.encode(bytes));
         response.put("sig", HEX.encode(sig));
         response.put("edsig", edsig);
         response.put("sbytes", sbytes);
      }
      catch(Exception e)
      {
         response = null;   
      }
      
      return response;
   }
   
   // Tests if a string is a valid JSON.
   private Boolean isJSONObject(String myStr)
   {
      try
      {
         JSONObject testJSON = new JSONObject(myStr);
         return testJSON != null;
      } catch (JSONException e)
      {
         return false;
      }
   }

   // Tests if s string is a valid JSON Array.
   private Boolean isJSONArray(String myStr)
   {
      try
      {
         JSONArray testJSONArray = new JSONArray(myStr);
         return testJSONArray != null;
      } catch (JSONException e)
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
      } catch (Exception e)
      {
         e.printStackTrace();
      }
      return null;
   }

   public JSONObject sendDelegationOperation(String delegator, String delegate, BigDecimal fee, String gasLimit,
                                             String storageLimit, EncKeys encKeys)
         throws Exception
   {
      JSONObject result = new JSONObject();

      BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
      JSONArray operations = new JSONArray();
      JSONObject revealOperation = new JSONObject();
      JSONObject transaction = new JSONObject();
      JSONObject head = new JSONObject();
      JSONObject account = new JSONObject();
      Integer counter = 0;

      if(gasLimit == null)
      {
         gasLimit = "10100";
      }
      else
      {
         if((gasLimit.length() == 0) || (gasLimit.equals("0")))
         {
            gasLimit = "10100";
         }
      }

      if(storageLimit == null)
      {
         storageLimit = "0";
      }
      else
      {
         if(storageLimit.length() == 0)
         {
            storageLimit = "0";
         }
      }

      head = new JSONObject(query("/chains/main/blocks/head~2/header", null).toString());
      account = getAccountForBlock(head.get("hash").toString(), delegator);
      counter = Integer.parseInt(account.get("counter").toString());

      // Append Reveal Operation if needed.
      revealOperation = appendRevealOperation(head, encKeys, delegator, (counter));

      if(revealOperation != null)
      {
         operations.put(revealOperation);
         counter = counter + 1;
      }

      transaction.put("kind", OPERATION_KIND_DELEGATION);
      transaction.put("source", delegator);
      transaction.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
      transaction.put("counter", String.valueOf(counter + 1));
      transaction.put("storage_limit", storageLimit);
      transaction.put("gas_limit", gasLimit);

      if(delegate.equals("undefined") == false)
      {
         transaction.put("delegate", delegate);
      }

      operations.put(transaction);

      result = (JSONObject) sendOperation(operations, encKeys);

      return result;

   }

   public JSONObject sendOriginationOperation(String from, Boolean spendable, Boolean delegatable, BigDecimal fee,
                                              String gasLimit, String storageLimit, BigDecimal amount, String code,
                                              String storage, EncKeys encKeys)
         throws Exception
   {
      JSONObject result = new JSONObject();

      BigDecimal roundedAmount = amount.setScale(6, BigDecimal.ROUND_HALF_UP);
      BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
      JSONArray operations = new JSONArray();
      JSONObject revealOperation = new JSONObject();
      JSONObject transaction = new JSONObject();
      JSONObject head = new JSONObject();
      JSONObject account = new JSONObject();
      Integer counter = 0;

      if(gasLimit == null)
      {
         gasLimit = "15555";
      }
      else
      {
         if((gasLimit.length() == 0) || (gasLimit.equals("0")))
         {
            gasLimit = "15555";
         }
      }

      if(storageLimit == null)
      {
         storageLimit = "489";
      }
      else
      {
         if(storageLimit.length() == 0)
         {
            storageLimit = "489";
         }
      }

      head = new JSONObject(query("/chains/main/blocks/head~2/header", null).toString());
      account = getAccountForBlock(head.get("hash").toString(), from);
      counter = Integer.parseInt(account.get("counter").toString());

      // Append Reveal Operation if needed.
      revealOperation = appendRevealOperation(head, encKeys, from, (counter));

      if(revealOperation != null)
      {
         operations.put(revealOperation);
         counter = counter + 1;
      }

      transaction.put("kind", OPERATION_KIND_ORIGINATION);
      transaction.put("source", from);
      transaction.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
      transaction.put("counter", String.valueOf(counter + 1));
      transaction.put("gas_limit", gasLimit);
      transaction.put("storage_limit", storageLimit);
      transaction.put("balance", (String.valueOf(roundedAmount.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
      transaction.put("spendable", spendable);

         JSONObject jsonScript = new JSONObject();
            jsonScript.put("code", code );
            jsonScript.put("storage", storage );
      
      transaction.put("script", jsonScript);
               
      operations.put(transaction);

      result = (JSONObject) sendOperation(operations, encKeys);

      return result;
   }

   public JSONObject sendUndelegationOperation(String delegator, BigDecimal fee, EncKeys encKeys) throws Exception
   {
      return sendDelegationOperation(delegator, "undefined", fee, "", "", encKeys);
   }

   public JSONObject sendBatchTransactions(ArrayList<BatchTransactionItem> transactions, EncKeys encKeys,
                                           String gasLimit, String storageLimit)
         throws Exception
   {

      JSONObject result = new JSONObject();

      JSONArray operations = new JSONArray();
      JSONObject account = new JSONObject();

      BigDecimal roundedAmount;
      BigDecimal roundedFee;
      JSONObject head;
      JSONObject transaction;
      JSONObject parameters;
      JSONArray argsArray;
      Integer counter = 0;
      BigDecimal fee;
      ArrayList<String> revealOperations = null;
      JSONObject revealOperation = new JSONObject();
      Integer extraCounterOffset = 0;

      if(gasLimit == null)
      {
         gasLimit = "15400";
      }
      else
      {
         if((gasLimit.length() == 0) || (gasLimit.equals("0")))
         {
            gasLimit = "15400";
         }
      }

      if(storageLimit == null)
      {
         storageLimit = "300";
      }
      else
      {
         if(storageLimit.length() == 0)
         {
            storageLimit = "300";
         }
      }

      // Sort transaction batch items by "from" address.
      // (this is necessary to add the transaction count to each address).
      Collections.sort(transactions);

      // Iterates over the transaction batch items, setting the count property of each
      // transaction.
      // (this will be used to find out the correct transaction counter).
      String currentAddress = "";
      Integer batchTransactionCounter = 0;
      for(BatchTransactionItem item : transactions)
      {
         // Sets batchTransactionCounter to 1 every time the address changes.
         if(item.getFrom().equals(currentAddress) == false)
         {
            batchTransactionCounter = 1;
         }

         currentAddress = item.getFrom();
         item.setCount(batchTransactionCounter);
         batchTransactionCounter++;

      }

      // Sort transaction batch items by original "index" order.
      // (this is necessary to maintain the original order in which the user added the
      // transactions).
      Collections.sort(transactions, new BatchTransactionItemIndexSorter());

      revealOperations = new ArrayList<String>();

      // Builds the transaction collection to be sent.
      for(BatchTransactionItem item : transactions)
      {
         roundedAmount = item.getAmount().setScale(6, BigDecimal.ROUND_HALF_UP);
         roundedFee = item.getFee().setScale(6, BigDecimal.ROUND_HALF_UP);

         // Get address counter.
         head = new JSONObject(query("/chains/main/blocks/head~2/header", null).toString());
         account = getAccountForBlock(head.get("hash").toString(), item.getFrom());
         counter = Integer.parseInt(account.get("counter").toString());

         transaction = new JSONObject();
         parameters = new JSONObject();
         argsArray = new JSONArray();

         // Checks if a reveal operation was not yet added to this transaction address.
         if(revealOperations.contains(item.getFrom()) == false)
         {
            // This will guarantee correct use of counter.
            extraCounterOffset = 0;

            // Check if a Reveal Operation is needed for current transaction address.
            revealOperation = appendRevealOperation(head, encKeys, item.getFrom(), (counter));

            if(revealOperation != null)
            {
               // Register that a Reveal Operation was needed to be added for this address.
               revealOperations.add(item.getFrom());

               // Actually ADD the operation.
               operations.put(revealOperation);

               // Guarantee that the counter will be handled correctly.
               extraCounterOffset = 1;
            }
         }
         else
         {
            // If the current address is present in the "control" array list,
            // then we need to consider that an additional operation has been added to that
            // address.
            extraCounterOffset = 1;
         }

         transaction.put("destination", item.getTo());
         transaction.put("amount", (String.valueOf(roundedAmount.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
         transaction.put("storage_limit", storageLimit);
         transaction.put("gas_limit", gasLimit);
         transaction.put("counter", String.valueOf(counter + item.getCount() + extraCounterOffset));
         transaction.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
         transaction.put("source", item.getFrom());
         transaction.put("kind", OPERATION_KIND_TRANSACTION);

         // Adds unique transaction to the collection.
         operations.put(transaction);

      }

      // Sends batch operation.
      result = (JSONObject) sendOperation(operations, encKeys);

      return result;
   }

   public Boolean waitForResult(String operationHash, Integer numberOfBlocksToWait) throws Exception
   {
      Integer currentBlockNumber = 0;
      Integer LimitBlockNumber = currentBlockNumber + numberOfBlocksToWait;
      JSONObject response = null;
      Boolean foundBlockHash = false;
      Boolean result = false;

      try
      {

         while ((result == false) && (currentBlockNumber < LimitBlockNumber))
         {

            // Get blockchain header.
            response = (JSONObject) query("/chains/main/blocks/head~2/header", null);

            // Acquaire current blockchain block (level) if it is zero yet.
            if(currentBlockNumber == 0)
            {
               // Sets the initial block number.
               if (response.has("level"))
               {
                  currentBlockNumber = (Integer) response.get("level");
               }
               
               // Sets the ending block number.
               LimitBlockNumber = currentBlockNumber + numberOfBlocksToWait;
            }

            // Reset control variables.
            foundBlockHash = false;
            String blockHash = "";

            // Wait until current block has a hash.
            while ((foundBlockHash == false)&&(currentBlockNumber>0))
            {
               // Extract block information from current block number.
               response = (JSONObject) query("/chains/main/blocks/" + currentBlockNumber, null);

               // Check if block has a hash.
               if(response.has("hash"))
               {
                  // Block hash has been found!
                  foundBlockHash = true;

                  // Get the block hash information.
                  blockHash = (String) response.get("hash");

                  // Increment the current block number;
                  currentBlockNumber++;

                  // Get the block operations using the block hash.
                  response = (JSONObject) query("/chains/main/blocks/" + blockHash + "/operations/3", null);

                  // Check result to see if desired operation hash is already included in a block.
                  result = checkResult(response, operationHash);

               }

               // If operation hash has not been found yet, give blockchain some time until
               // next fetch.
               if(result == false)
               {
                  // Wait 10 seconds to query blockchain again.
                  TimeUnit.SECONDS.sleep(10);
               }
            }

         }

      } catch (Exception e)
      {
         e.printStackTrace();
      }

      return result;

   }

   public Boolean checkResult(JSONObject blockRead, String operationHash)
   {
      Boolean result = false;
      String hash = "";
      String opHash = "";

      // Do some cleaning.
      opHash = operationHash.replace("\"", "");
      opHash = opHash.replace("\n", "");
      opHash = opHash.trim();

      // Define object we will need inside the loop.
      JSONObject myObject = new JSONObject();

      try
      {
         // Extract the array from the JSONObject "result".
         JSONArray myArray = (JSONArray) blockRead.get("result");

         // Loop through each element of the array.
         for(Integer i = 0; i < myArray.length(); i++)
         {
            // Get current element of the array.
            myObject = ((JSONObject) ((JSONArray) blockRead.get("result")).get(i));

            // What interests us is the element "hash".
            hash = myObject.get("hash").toString();

            // Do some cleaning.
            hash = hash.replace("\"", "");
            hash = hash.replace("\n", "");
            hash = hash.trim();

            // Check if the operation hash we've got from the block is the same we are
            // searching.
            if(hash.equals(opHash))
            {
               // We've found the hash included in this block!
               result = true;
               break;
            }

         }
      } catch (Exception f)
      {
         result = false;
      }

      return result;

   }

   // Calls a contract passing parameters.
   public JSONObject callContractEntryPoint(String from, String contract, BigDecimal amount, BigDecimal fee,
                                            String gasLimit, String storageLimit, EncKeys encKeys, String entrypoint,
                                            String[] parameters, Boolean rawParameter, String smartContractType)
         throws Exception
   {
      JSONObject result = new JSONObject();

      BigDecimal roundedAmount = amount.setScale(6, BigDecimal.ROUND_HALF_UP);
      BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
      JSONArray operations = new JSONArray();
      JSONObject revealOperation = new JSONObject();
      JSONObject transaction = new JSONObject();
      JSONObject head = new JSONObject();
      JSONObject account = new JSONObject();
      Integer counter = 0;

      // Check if address has enough funds to do the transfer operation.
      JSONObject balance = getBalance(from);

      if(balance.has("result"))
      {
         BigDecimal bdAmount = amount.multiply(BigDecimal.valueOf(UTEZ));
         BigDecimal total = new BigDecimal(
               ((balance.getString("result").replaceAll("\\n", "")).replaceAll("\"", "").replaceAll("'", "")));

         if(total.compareTo(bdAmount) < 0) // Returns -1 if value is less than amount.
         {
            // Not enough funds to do the transfer.
            JSONObject returned = new JSONObject();
            returned.put("result",
                  "{ \"result\":\"error\", \"kind\":\"TezosJ_SDK_exception\", \"id\": \"Not enough funds\" }");

            return returned;
         }
      }

      if(gasLimit == null)
      {
         gasLimit = "750000";
      }
      else
      {
         if((gasLimit.length() == 0) || (gasLimit.equals("0")))
         {
            gasLimit = "750000";
         }
      }

      if(storageLimit == null)
      {
         storageLimit = "1000";
      }
      else
      {
         if(storageLimit.length() == 0)
         {
            storageLimit = "1000";
         }
      }

      head = new JSONObject(query("/chains/main/blocks/head~2/header", null).toString());
      account = getAccountForBlock(head.get("hash").toString(), from);
      counter = Integer.parseInt(account.get("counter").toString());

      // Append Reveal Operation if needed.
      revealOperation = appendRevealOperation(head, encKeys, from, (counter));

      if(revealOperation != null)
      {
         operations.put(revealOperation);
         counter = counter + 1;
      }

      transaction.put("destination", contract);
      transaction.put("amount", (String.valueOf(roundedAmount.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
      transaction.put("storage_limit", storageLimit);
      transaction.put("gas_limit", gasLimit);
      transaction.put("counter", String.valueOf(counter + 1));
      transaction.put("fee", (String.valueOf(roundedFee.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger())));
      transaction.put("source", from);
      transaction.put("kind", OPERATION_KIND_TRANSACTION);

      
      JSONObject myParams = null;
      if (rawParameter == false)
      {
         // Builds a Michelson-compatible set of parameters to pass to the smart
         // contract.
   
         JSONObject myparamJson = new JSONObject();
         
         String[] contractEntrypoints = getContractEntryPoints(contract);

         if (Arrays.asList(contractEntrypoints).contains("default") == false)
         {
            if (Arrays.asList(contractEntrypoints).contains(entrypoint) == false)
            {
               throw new Exception("Wrong or missing entrypoint name");
            }
         }
         else
         {
            entrypoint = "default";                 
         }
         
         // Check if smartContractType parameter is null or empty.
         if (smartContractType == null)
         {
            smartContractType = Global.GENERIC_STANDARD;
         }
         else if (smartContractType.isEmpty() == true)
         {
            smartContractType = Global.GENERIC_STANDARD;
         }

         // According to each smart contract standard, get the entrypoint parameters.
         String[] contractEntryPointParameters = null;
         String[] contractEntryPointParametersTypes = null;
         if(smartContractType.equals(Global.GENERIC_STANDARD))
         {
            
            contractEntryPointParameters = getContractEntryPointsParameters(contract, entrypoint, "names");
            contractEntryPointParametersTypes = getContractEntryPointsParameters(contract, entrypoint, "types");
            
         }
         else if(smartContractType.equals(Global.FA12_STANDARD))
         {
            // If the smartContractType standard is FA1.2, then we already know the entrypoint parameters.            
            if (entrypoint.equals(Global.FA12_TRANSFER))
            {
               contractEntryPointParameters = new String[] { "from", "to", "value" };
               contractEntryPointParametersTypes = new String[] { "string", "string", "nat" };
            }
            else if (entrypoint.equals(Global.FA12_APPROVE))
            {  
               contractEntryPointParameters = new String[] { "spender", "value" };
               contractEntryPointParametersTypes = new String[] { "string", "nat" };
            }
            else if (entrypoint.equals(Global.FA12_GET_ALLOWANCE))
            {  
               contractEntryPointParameters = new String[] { "owner", "spender", "callback" };
               contractEntryPointParametersTypes = new String[] { "string", "string", "string" };
            }
            else if (entrypoint.equals(Global.FA12_GET_BALANCE))
            {
               contractEntryPointParameters = new String[] { "owner", "callback" };
               contractEntryPointParametersTypes = new String[] { "string", "string" };
            }
            else if (entrypoint.equals(Global.FA12_GET_TOTAL_SUPPLY))
            {
               contractEntryPointParameters = new String[] { "unit", "callback" };
               contractEntryPointParametersTypes = new String[] { "unit", "string" };
            }
               
         }
         else // fallback.
         {
            smartContractType = Global.GENERIC_STANDARD;
            contractEntryPointParameters = getContractEntryPointsParameters(contract, entrypoint, "names");
            contractEntryPointParametersTypes = getContractEntryPointsParameters(contract, entrypoint, "types");            
         }
            
         myparamJson = paramValueBuilder(entrypoint, contractEntrypoints, parameters,
                                         contractEntryPointParameters,
                                         contractEntryPointParametersTypes,
                                         smartContractType);

         // Adds the smart contract parameters to the transaction.
         myParams = new JSONObject();
         myParams.put("entrypoint", entrypoint);
         myParams.put("value", myparamJson);
         
      }
      else
      {
         // Adds the smart contract parameters to the transaction.
         myParams = new JSONObject();
         myParams.put("entrypoint", entrypoint);
         JSONArray myJsonArray = new JSONArray(parameters[0]);         
         myParams.put("value", myJsonArray);
      }
         
      transaction.put("parameters", myParams);

      operations.put(transaction);

      if ( (smartContractType == Global.FA12_STANDARD) && (entrypoint.equals(Global.FA12_TRANSFER) == false) && (entrypoint.equals(Global.FA12_APPROVE) == false))
      {
         
         JSONObject jsonObj = (JSONObject) callRunOperation(operations, encKeys);
         
         result.put("result", jsonObj);
      }
      else
      {
         result = (JSONObject) sendOperation(operations, encKeys);
      }
      
      return result;
   }

   
   private JSONObject paramValueBuilder(String entrypoint, String[] contractEntrypoints, String[] parameters,
                                        String[] contractEntryPointParameters, String[] datatypes,
                                        String smartContractType) throws Exception
   {
      
      if (smartContractType.equals(Global.GENERIC_STANDARD))
      {
         if (parameters.length != datatypes.length)
         {
            throw new Exception("Wrong number of parameters to contract entrypoint");
         }
      }
      
      // Creates the JSON object that will be returned by the methd.
      JSONObject myJsonObj = new JSONObject();

      List<String> parametersList = Arrays.asList(parameters);
      List<String> typesList = Arrays.asList(datatypes);
      List<String> entrypointsList = Arrays.asList(contractEntrypoints);

      // Corrects typeList.
      for(int i = 0; i < typesList.size(); i++)
      {
         switch (typesList.get(i))
         {
            case "int":
               typesList.set(i, "int");
               break;
            case "string":
               typesList.set(i, "string");
               break;
            case "nat":
               typesList.set(i, "int");
               break;
            case "mutez":
               typesList.set(i, "int");
               break;
            case "tez":
               typesList.set(i, "int");
               break;
            case "bytes":
               typesList.set(i, "int");
               break;
            case "key_hash":
               typesList.set(i, "string");
               break;
            case "bool":
               typesList.set(i, "prim");
               break;
            case "unit":
               typesList.set(i, "unit");
               break;
            case "address":
               typesList.set(i, "string");
               break;
            case "owner":
               typesList.set(i, "string");
               break;
            case "spender":
               typesList.set(i, "string");
               break;
               
            default:
               typesList.set(i, "string");

         }

      }

      Pair<Pair, List> basePair = null;
      Pair pair = buildParameterPairs(myJsonObj, basePair, parametersList, contractEntryPointParameters, false, smartContractType, entrypoint);
      
      // Create JSON from Pair.
      myJsonObj = (JSONObject) solvePair(pair, typesList, smartContractType);
      
      return myJsonObj;
      
   }
    
   private Object solvePair(Object pair, List datatypes, String smartContractType) throws Exception
   {
         
      Object result = null;
      
      // Extract and check contents.
      if (hasPairs((Pair) pair) == false)
      {
         // Here we've got List in both sides. But they might have more than one element.
         Object jsonLeft  = ((Pair) pair).getLeft() == null ? null : toJsonFormat((List)((Pair) pair).getLeft(), datatypes, 0);
         Object jsonRight = ((Pair) pair).getRight() == null ? null : toJsonFormat((List)((Pair) pair).getRight(), datatypes, ((Pair) pair).getLeft() == null ? 0 : ((List)((Pair) pair).getLeft()).size() );
         
         // Test if there is only one parameter.
         if (jsonLeft == null)
         {
            if (jsonRight == null)
            {
               throw new Exception("Pair cannot be (null, null)");
            }
            else
            {
               // FA1.2 handling
               if (smartContractType.equals(Global.FA12_STANDARD))
               {
                  if (((JSONObject) jsonRight).has("unit"))
                  {
                     JSONObject tmpObj = new JSONObject(), tmpItem1 = new JSONObject(), tmpItem2 = new JSONObject();
                     tmpObj.put("prim", "Pair");
                     Iterator<?> keys = ((JSONObject) jsonRight).keys();
                     String key = (String)keys.next();
                     tmpItem1.put("prim", "Unit");
                     tmpItem2.put("string", ((JSONObject) jsonRight).get(key));
                     
                     JSONArray arr= new JSONArray();
                     arr.put(tmpItem1);
                     arr.put(tmpItem2);
                   
                     tmpObj.put("args", arr);
                     
                     return tmpObj;
                  }
                  else
                  {
                     return jsonRight;
                  }

               }
               else
               {
                  return jsonRight;
               }
               
            }
         }
         else if (jsonRight == null)
         {
            return jsonLeft;
         }
         
         // Build json outter pair.
         JSONObject jsonPair = new JSONObject();
         jsonPair.put("prim", "Pair");
         
         // Create pair contents array.
         JSONArray pairContents = new JSONArray();
         pairContents.put(jsonLeft);
         pairContents.put(jsonRight);
         jsonPair.put("args", pairContents);
         
         return jsonPair;
      }
      else
      {
         Object jsonLeft = solvePair(((Pair<Pair, List>) pair).getLeft(), datatypes, smartContractType);
         Object jsonRight = solvePair(((Pair<Pair, List>) pair).getRight(), datatypes.subList( countPairElements((Pair) ((Pair) pair).getLeft()), datatypes.size()), smartContractType );
         
         // Build json outter pair.
         JSONObject jsonPair = new JSONObject();
         jsonPair.put("prim", "Pair");
         
         // Create pair contents array.
         JSONArray pairContents = new JSONArray();
         pairContents.put(jsonLeft);
         pairContents.put(jsonRight);
         jsonPair.put("args", pairContents);
         
         return jsonPair;
      }

   }

   private Integer countPairElements(Pair pair)
   {
      Integer leftCount = 0;
      Integer rightCount = 0;
      
      Object left = pair.getLeft();
      Object right = pair.getRight();

      if(left instanceof Pair)
      {
         leftCount = countPairElements((Pair) left);
      }
      else
      {
         leftCount = ((List)left).size();
      }
      
      if(right instanceof Pair)
      {
         rightCount = countPairElements((Pair) right);
      }
      else
      {
         rightCount = ((List)right).size();
      }

      return leftCount+rightCount;

   }
   
   private Boolean hasPairs(Pair pair)
   {
      Object left = pair.getLeft();
      Object right = pair.getRight();
      
      if( (left instanceof Pair) || (right instanceof Pair) )
      {
         return true;
      }
      else
      {
         return false;
      }
   }
   
   private JSONObject toJsonFormat(List list, List datatypes, Integer firstElement)
   { 
      JSONArray result = new JSONArray();
            
      for(int i=0;i<list.size();i++)
      {
         JSONObject element = new JSONObject();
         element.put((String) datatypes.get(firstElement + i), list.get(i));

         // Add element to array.
         result.put(element);

      }
               
      if (result.length() > 1)
      {
         // Wrap json result in outter pair.
         JSONObject jsonPair = new JSONObject();
         jsonPair.put("prim", "Pair");
         jsonPair.put("args", result);   
         
         return jsonPair;
      }
      else
      {
         return (JSONObject)result.get(0);
      }

   }
   
   private Pair buildParameterPairs(JSONObject jsonObj, Pair pair, List<String> parameters,
                                    String[] contractEntryPointParameters,
                                    Boolean doSolveLeft, String smartContractType, String entrypoint) throws Exception
   {
      
      // Test parameters validity.
      if (parameters.isEmpty())
      {
         throw new Exception("Missing parameters to pass to contract entrypoint");
      }

      List<String> left = new ArrayList<String>();
      List<String> right = new ArrayList<String>();
      Pair newPair = null;
      
      if(parameters.size() == 1)
      {         
         // If number of parameters is only 1.
         newPair = new MutablePair<>(null, new ArrayList<String>(Arrays.asList(parameters.get(0))));
      }
      else 
      {

         if (pair == null)
         {
            if (smartContractType.equals(Global.FA12_STANDARD) && entrypoint.equals(Global.FA12_GET_ALLOWANCE))
            {
               left = parameters.subList(0, 2);
               right = parameters.subList(2, parameters.size());
   
               newPair = new MutablePair<>(left, right);
            }
            else
            {
               Integer half = ( Math.abs(parameters.size() / 2) );
   
               left = parameters.subList(0, half);
               right = parameters.subList(half, parameters.size());
   
               newPair = new MutablePair<>(left, right);
            }           
         }
         else
         {
            List<String> newList;
            
            if (doSolveLeft == true)
            {
               newList = ((List<String>) pair.getLeft());
            }
            else
            {
               newList = ((List<String>) pair.getRight());
            }
            
            Integer half = ( Math.abs( newList.size() / 2) );

            left = newList.subList(0, half);
            right = newList.subList(half, newList.size());

            newPair = new MutablePair<>(left, right);
            
         }

         
         if (  (((List)newPair.getRight()).size() > 2) || (((List)newPair.getLeft()).size() > 2)  )
         {

               newPair = new MutablePair<>(buildParameterPairs(jsonObj, newPair, parameters, contractEntryPointParameters, true, smartContractType, entrypoint),
                                           buildParameterPairs(jsonObj, newPair, parameters, contractEntryPointParameters, false, smartContractType, entrypoint));

         }
         else
         {
            return newPair;
         }

      }

      return newPair;

   }   
   
   private String[] getContractEntryPoints(String contractAddress) throws Exception
   {
      JSONObject response = (JSONObject) query(
            "/chains/main/blocks/head~2/context/contracts/" + contractAddress + "/entrypoints", null);

      JSONObject entryPointsJson = (JSONObject) response.get("entrypoints");

      String[] entrypoints = JSONObject.getNames(entryPointsJson);

      // If there is a list of entrypoints, then sort its elements.
      // This is fundamental for the call to work, as the order of the entrypoints
      // matter.
      if(entrypoints != null)
      {
         if(entrypoints.length > 1)
         {
            Arrays.sort(entrypoints);
         }
      }
      else
      {
         // If there are no entrypoints declared in the contract, consider only the
         // "default" entrypoint.
         entrypoints = new String[]
         { "default" };
      }

      return entrypoints;
   }

   private String[] getContractEntryPointsParameters(String contractAddress, String entrypoint, String namesOrTypes)
         throws Exception
   {
      ArrayList<String> parameters = new ArrayList<String>();

      // If no desired entrypoint was specified, use the "default" entrypoint.
      if((entrypoint == null) || (entrypoint.length() == 0))
      {
         entrypoint = "default";
      }

      JSONObject response = (JSONObject) query(
            "/chains/main/blocks/head~2/context/contracts/" + contractAddress + "/entrypoints/" + entrypoint, null);

      JSONArray paramArray = decodeParameters(response, null);

      JSONObject jsonObj = new JSONObject();

      parameters = new ArrayList<String>();

      if(namesOrTypes.equals("names"))
      {
         for(int i = 0; i < paramArray.length(); i++)
         {
            jsonObj = (JSONObject) paramArray.get(i);
            if(jsonObj.has("annots"))
            {
               JSONArray annotsArray = (JSONArray) jsonObj.get("annots");
               parameters.add(((String) annotsArray.getString(0).replace("%", "")).replace(":", ""));
            }
         }
      }
      else if(namesOrTypes.equals("types"))
      {
         for(int i = 0;i < paramArray.length(); i++)
         {
            jsonObj = (JSONObject) paramArray.get(i);
            if(jsonObj.has("prim"))
            {
               if ( (jsonObj.get("prim").toString()).contains("contract") == false)
               {
                  parameters.add((String) jsonObj.get("prim"));
               }
            }
         }
      }

      String[] tempArray = new String[parameters.size()];
      tempArray = parameters.toArray(tempArray);

      return tempArray;
   }

   private JSONArray decodeParameters(JSONObject jsonObj, JSONArray builtArray)
   {
      
      JSONObject left = new JSONObject();
      JSONObject right = new JSONObject();
      
      if((jsonObj.has("args") == true) || (jsonObj.has("prim") == true))
      {
         if(builtArray == null)
         {
            builtArray = new JSONArray();
         }

         if(jsonObj.has("args"))
         {                        
            JSONArray myArr = jsonObj.getJSONArray("args");  
            left = myArr.getJSONObject(0);            
            builtArray = decodeParameters(left, builtArray);

            if (myArr.length() > 1)
            {
               right = myArr.getJSONObject(1);
               builtArray = decodeParameters(right, builtArray);
            }
         }
         else
         {
            
            // Now we can extract a single element.
            builtArray.put(jsonObj);
            
            return builtArray;
         }

      }
      
      return builtArray;
   }

   public ArrayList<Map> getContractStorage(String contractAddress) throws Exception
   {

      ArrayList<Map> items = new ArrayList<Map>();
      
       JSONObject response = (JSONObject) query("/chains/main/blocks/head~2/context/contracts/" + contractAddress + "/storage/", null);
        
       JSONArray storageArray = decodeParameters(response, null);
        
        JSONObject jsonObj = new JSONObject();
        
        for(int i = 0; i < storageArray.length(); i++)
        {
           jsonObj = (JSONObject) storageArray.get(i);
           Map<String, Object> map = new HashMap<String, Object>();
           map = toMap(jsonObj); items.add(map);
        }
       
       return items;
   }

   public static Map<String, Object> toMap(JSONObject object) throws JSONException
   {
      Map<String, Object> map = new HashMap<String, Object>();

      Iterator<String> keysItr = object.keys();
      while (keysItr.hasNext())
      {
         String key = keysItr.next();
         Object value = object.get(key);

         if(value instanceof JSONArray)
         {
            value = toList((JSONArray) value);
         }

         else if(value instanceof JSONObject)
         {
            value = toMap((JSONObject) value);
         }
         map.put(key, value);
      }
      return map;
   }

   public static List<Object> toList(JSONArray array) throws JSONException
   {
      List<Object> list = new ArrayList<Object>();
      for(int i = 0; i < array.length(); i++)
      {
         Object value = array.get(i);
         if(value instanceof JSONArray)
         {
            value = toList((JSONArray) value);
         }

         else if(value instanceof JSONObject)
         {
            value = toMap((JSONObject) value);
         }
         list.add(value);
      }
      return list;
   }

   public Boolean waitForAndCheckResult(String operationHash, Integer numberOfBlocksToWait) throws Exception
   {
      Integer currentBlockNumber = 0;
      Integer LimitBlockNumber = currentBlockNumber + numberOfBlocksToWait;
      JSONObject response = null;
      Boolean foundBlockHash = false;
      String result = "";

      try
      {

         while ((result.equals("")) && (currentBlockNumber < LimitBlockNumber))
         {

            // Get blockchain header.
            response = (JSONObject) query("/chains/main/blocks/head~2/header", null);

            // Acquaire current blockchain block (level) if it is zero yet.
            if(currentBlockNumber == 0)
            {
               // Sets the initial block number.
               if (response.has("level"))
               {
                  currentBlockNumber = (Integer) response.get("level");
               }

               // Sets the ending block number.
               LimitBlockNumber = currentBlockNumber + numberOfBlocksToWait;
            }
            
            // Reset control variables.
            foundBlockHash = false;
            String blockHash = "";

            // Wait until current block has a hash.
            while ((foundBlockHash == false)&&(currentBlockNumber>0))
            {
               // Extract block information from current block number.
               response = (JSONObject) query("/chains/main/blocks/" + currentBlockNumber, null);

               // Check if block has a hash.
               if(response.has("hash"))
               {
                  // Block hash has been found!
                  foundBlockHash = true;

                  // Get the block hash information.
                  blockHash = (String) response.get("hash");

                  // Increment the current block number;
                  currentBlockNumber++;

                  // Get the block operations using the block hash.
                  response = (JSONObject) query("/chains/main/blocks/" + blockHash + "/operations/3", null);

                  // Check result to see if desired operation hash is already included in a block.
                  result = checkOperationResult(response, operationHash);

                  
                  
               }

               // If operation hash has not been found yet, give blockchain some time until
               // next fetch.
               if(result.equals(""))
               {
                  // Wait 5 seconds to query blockchain again.
                  TimeUnit.SECONDS.sleep(5);
               }
            }

         }

      } catch (Exception e)
      {
         e.printStackTrace();
      }

      return result.equals("true") ? true : false;

   }
   
   public String checkOperationResult(JSONObject blockRead, String operationHash)
   {
      String result = "";
      String hash = "";
      String opHash = "";

      // Do some cleaning.
      opHash = operationHash.replace("\"", "");
      opHash = opHash.replace("\n", "");
      opHash = opHash.trim();

      // Define object we will need inside the loop.
      JSONObject myObject = new JSONObject();

      try
      {
         // Extract the array from the JSONObject "result".
         JSONArray myArray = (JSONArray) blockRead.get("result");

         // Loop through each element of the array.
         for(Integer i = 0; i < myArray.length(); i++)
         {
            // Get current element of the array.
            myObject = ((JSONObject) ((JSONArray) blockRead.get("result")).get(i));

            // What interests us is the element "hash".
            hash = myObject.get("hash").toString();

            // Do some cleaning.
            hash = hash.replace("\"", "");
            hash = hash.replace("\n", "");
            hash = hash.trim();

            // Check if the operation hash we've got from the block is the same we are
            // searching.
            if(hash.equals(opHash))
            {
               
               // We've found the hash included in this block. Now let's extract the operation
               // result: applied or failed.
               
               JSONArray contentsArray = ((JSONArray) myObject.get("contents"));

               for(int w=0;w<contentsArray.length();w++)
               {
                  JSONObject opRes = (JSONObject) ((JSONObject)((JSONObject)contentsArray.get(w)).get("metadata")).get("operation_result"); 
                  
                  if(opRes.get("status").equals("failed"))
                  {
                     result = "false";
                     break;                     
                  }
                  else if(opRes.get("status").equals("applied"))
                  {
                     result = "true";                    
                  }
               
                  if(opRes.has("errors"))
                  {
                     result = "false";
                     break;
                  }
               }
            
               if(result.equals("false") == true)
               {
                  break;
               }
               
            }

         }
         
      }
      catch (Exception f)
      {
         result = "false";
      }

      return result;

   }

   public JSONObject waitForAndCheckResultByDestinationAddress(String address, Integer numberOfBlocksToWait) throws Exception
   {
      Integer currentBlockNumber = 0;
      Integer LimitBlockNumber = currentBlockNumber + numberOfBlocksToWait;
      JSONObject response = null;
      Boolean foundBlockHash = false;
      JSONObject result = null;

      try
      {

         while ((result == null)  && (currentBlockNumber < LimitBlockNumber))
         {

            // Get blockchain header.
            response = (JSONObject) query("/chains/main/blocks/head~2/header", null);

            // Acquaire current blockchain block (level) if it is zero yet.
            if(currentBlockNumber == 0)
            {
               // Sets the initial block number.
               if (response.has("level"))
               {
                  currentBlockNumber = (Integer) response.get("level");
               }

               // Sets the ending block number.
               LimitBlockNumber = currentBlockNumber + numberOfBlocksToWait;
            }
            
            // Reset control variables.
            foundBlockHash = false;
            String blockHash = "";

            // Wait until current block has a hash.
            while ((foundBlockHash == false)&&(currentBlockNumber>0)&&(result == null))
            {
               // Extract block information from current block number.
               response = (JSONObject) query("/chains/main/blocks/" + currentBlockNumber, null);

               // Check if block has a hash.
               if(response.has("hash"))
               {
                  // Block hash has been found!
                  foundBlockHash = true;

                  // Get the block hash information.
                  blockHash = (String) response.get("hash");

                  // Increment the current block number;
                  currentBlockNumber++;

                  // Get the block operations using the block hash.
                  response = (JSONObject) query("/chains/main/blocks/" + blockHash + "/operations/3", null);

                  // Check result to see if desired operation hash is already included in a block.
                  result = checkOperationResultByAddress(response, address);
                  
               }

               // If operation hash has not been found yet, give blockchain some time until
               // next fetch.
               if(result == null)
               {
                  // Wait 5 seconds to query blockchain again.
                  TimeUnit.SECONDS.sleep(5);
               }
            }

         }

      } catch (Exception e)
      {
         e.printStackTrace();
      }

      return result;

   }

   public JSONObject checkOperationResultByAddress(JSONObject blockRead, String address)
   {
      JSONObject result = null;
      String myAddress = "";

      // Do some cleaning.
      myAddress = address.replace("\"", "");
      myAddress = myAddress.replace("\n", "");
      myAddress = myAddress.trim();

      // Define object we will need inside the loop.
      JSONObject myObject = new JSONObject();

      try
      {
         // Extract the array from the JSONObject "result".
         JSONArray myArray = (JSONArray) blockRead.get("result");

         // Loop through each element of the array.
         for(Integer i = 0; i < myArray.length(); i++)
         {
            // Get current element of the array.
            myObject = ((JSONObject) ((JSONArray) blockRead.get("result")).get(i));

            // What interests us is the element "contents".
            JSONArray contentsArray = ((JSONArray)myObject.get("contents"));
          
            for(int w=0;w<contentsArray.length();w++)
            {
               JSONObject jsonObj = (JSONObject)contentsArray.get(w); 
                              
               if(jsonObj.has("destination"))
               {
                  if(jsonObj.get("destination").equals(address) == true)
                  {
                     
                     JSONObject opRes = (JSONObject) ((JSONObject)jsonObj.get("metadata")).get("operation_result"); 
                     
                     if(opRes.get("status").equals("failed"))
                     {
                        result = null;
                        break;                     
                     }
                     else if(opRes.get("status").equals("applied"))
                     {
                        result = new JSONObject();
                        result.put("amount", jsonObj.get("amount"));
                        result.put("address", jsonObj.get("source"));
                        result.put("hash", myObject.get("hash"));
                        break;
                     }
                     
                     if(opRes.has("errors"))
                     {
                        result = null;
                        break;
                     }
                  }
               
                  if(result != null)
                  {
                     break;
                  }
                   
               }
            }

         }
         
      }
      catch (Exception f)
      {
         result = null;
      }

      return result;

   }

   
   // Sends a transfer from KT to TZ operation to the Tezos node.
   public JSONObject transferImplicit(String contract, String implicitAddress, String managerAddress, BigDecimal amount, EncKeys encKeys)
         throws Exception
   {
      JSONObject result = new JSONObject();

      BigDecimal fee = new BigDecimal(Global.KT_TO_TZ_FEE);
      BigDecimal roundedAmount = amount.setScale(6, BigDecimal.ROUND_HALF_UP);
      BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
      String gasLimit = Global.KT_TO_TZ_GAS_LIMIT;
      String storageLimit = Global.KT_TO_TZ_STORAGE_LIMIT;
      
      String mutez = (String.valueOf(roundedAmount.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger()));
      String michelson = "[{ \"prim\": \"DROP\" },"
                       + "{ \"prim\": \"NIL\", \"args\": [{ \"prim\": \"operation\" }] },"
                       + "{"
                       + "\"prim\": \"PUSH\","
                       + "\"args\":"
                       + "[{ \"prim\": \"key_hash\" },"
                       + "{ \"string\": \"" + implicitAddress + "\"  }]"
                       + "},"
                       + "{ \"prim\": \"IMPLICIT_ACCOUNT\" },"
                       + "{"
                       + "\"prim\": \"PUSH\","
                       + "\"args\": [{ \"prim\": \"mutez\" }, { \"int\": \"" + mutez + "\" }]"
                       + "},"
                       + "{ \"prim\": \"UNIT\" }, { \"prim\": \"TRANSFER_TOKENS\" },"
                       + "{ \"prim\": \"CONS\" }]";

      result = callContractEntryPoint(managerAddress, contract, BigDecimal.ZERO, roundedFee, gasLimit, storageLimit, encKeys, "do", new String[] { michelson }, true, Global.GENERIC_STANDARD );
      
      return result;
      
   }

   // Sends a transfer from KT to KT operation to the Tezos node.
   public JSONObject transferToContract(String contract, String destinationKT, String managerAddress, BigDecimal amount, EncKeys encKeys)
         throws Exception
   {
      JSONObject result = new JSONObject();

      BigDecimal fee = new BigDecimal(Global.KT_TO_KT_FEE);
      BigDecimal roundedAmount = amount.setScale(6, BigDecimal.ROUND_HALF_UP);
      BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
      String gasLimit = Global.KT_TO_KT_GAS_LIMIT;
      String storageLimit = Global.KT_TO_KT_STORAGE_LIMIT;
      
      String mutez = (String.valueOf(roundedAmount.multiply(BigDecimal.valueOf(UTEZ)).toBigInteger()));
      String michelson = "[{ \"prim\": \"DROP\" },"
                       + "{ \"prim\": \"NIL\", \"args\": [{ \"prim\": \"operation\" }] },"
                       + "{"
                       + "\"prim\": \"PUSH\","
                       + "\"args\":"
                       + "[{ \"prim\": \"address\" },"
                       + "{ \"string\": \"" + destinationKT + "\" }]"
                       + "},"
                       + "{ \"prim\": \"CONTRACT\", \"args\": [{ \"prim\": \"unit\" }] },"
                       + "[{"
                       + "\"prim\": \"IF_NONE\","
                       + "\"args\":"
                       + "[[[{ \"prim\": \"UNIT\" }, { \"prim\": \"FAILWITH\" }]],"
                       + "[]]"
                       + "}],"
                       + "{"
                       + "\"prim\": \"PUSH\","
                       + "\"args\": [{ \"prim\": \"mutez\" }, { \"int\": \"" + mutez + "\"  }]"
                       + "},"
                       + "{ \"prim\": \"UNIT\" }, { \"prim\": \"TRANSFER_TOKENS\" },"
                       + "{ \"prim\": \"CONS\" }]";


      result = callContractEntryPoint(managerAddress, contract, BigDecimal.ZERO, roundedFee, gasLimit, storageLimit, encKeys,"do", new String[] { michelson }, true, Global.GENERIC_STANDARD);

      return result;
      
   }

   // Delegate from KT.
   public JSONObject sendDelegationFromContract(String delegator, String delegate, String managerAddress, EncKeys encKeys) throws Exception
   {
      JSONObject result = new JSONObject();

      BigDecimal fee = new BigDecimal(Global.KT_TO_TZ_FEE);
      BigDecimal roundedFee = fee.setScale(6, BigDecimal.ROUND_HALF_UP);
      String gasLimit = Global.KT_TO_TZ_GAS_LIMIT;
      String storageLimit = Global.KT_TO_TZ_STORAGE_LIMIT;
      
      String michelson = "[{ \"prim\": \"DROP\" },"
                       + "{ \"prim\": \"NIL\", args: [{ \"prim\": \"operation\" }] },"
                       + "{"
                       + "  \"prim\": \"PUSH\","
                       + "  args: [{ \"prim\": \"key_hash\" }, { \"string\": \"" + delegate + "\" }],"
                       + "},"
                       + "{ \"prim\": \"SOME\" },"
                       + "{ \"prim\": \"SET_DELEGATE\" },"
                       + "{ \"prim\": \"CONS\" }]";

      result = callContractEntryPoint(managerAddress, delegator, BigDecimal.ZERO, roundedFee, gasLimit, storageLimit, encKeys, "do", new String[] { michelson }, true , Global.GENERIC_STANDARD);
      
      return result;
      
   }


   static int countOccurences(String str, String word)  
   { 
     
       int count = 0; 
       for (int i = 0; i < str.length(); i++)  
       { 

          int pos = str.indexOf(word); 
          if ( (pos > 0) && (pos > i) )
          {
             count++;
             i = pos + 1;
          }
          
       } 
     
       return count; 
   } 

   String replaceLast(String string, String substring, String replacement)
   {
      int index = string.lastIndexOf(substring);
      
      if (index == -1)
      {
        return string;
      }
     
      return string.substring(0, index) + replacement + string.substring(index+substring.length());
   
   }
   
}

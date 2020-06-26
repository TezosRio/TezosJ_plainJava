package milfont.com.tezosj.model;

import org.bitcoinj.crypto.MnemonicCode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import milfont.com.tezosj.domain.Crypto;
import milfont.com.tezosj.domain.Rpc;
import milfont.com.tezosj.helper.Base58;
import milfont.com.tezosj.helper.Global;
import milfont.com.tezosj.helper.MySodium;
import milfont.com.tezosj.helper.Sha256Hash;

import static milfont.com.tezosj.helper.Constants.TEZOS_SYMBOL;
import static milfont.com.tezosj.helper.Constants.TZJ_KEY_ALIAS;
import static milfont.com.tezosj.helper.Constants.UTEZ;
import static milfont.com.tezosj.helper.Encoder.HEX;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Milfont on 21/07/2018.
 */

public class TezosWallet implements FA12_Interface
{

   private String alias = "";
   private byte[] publicKey;
   private byte[] publicKeyHash;
   private byte[] privateKey;
   private byte[] mnemonicWords;
   private String balance = "";
   private ArrayList<Transaction> transactions = null;

   private String encPass, encIv;

   private Rpc rpc = null;
   private Crypto crypto = null;
   private MySodium sodium = null;
   private int myRandomID;

   private ArrayList<BatchTransactionItem> transactionBatch = null;

   // Constructor with passPhrase.
   // This will create a new wallet and generate new keys and mnemonic words.
   public TezosWallet(String passPhrase) throws Exception
   {
      if(passPhrase != null)
      {
         if(passPhrase.length() > 0)
         {

            // Creates a unique copy and initializes libsodium native library.
            Random rand = new Random();
            int n = rand.nextInt(1000000) + 1;
            this.myRandomID = n;
            this.sodium = new MySodium(String.valueOf(n));

            // Converts passPhrase String to a byte array, respecting char values.
            byte[] c = new byte[passPhrase.length()];
            for(int i = 0; i < passPhrase.length(); i++)
            {
               c[i] = (byte) passPhrase.charAt(i);
            }

            initStore(c);

            initDomainClasses();

            generateMnemonic();
            generateKeys(passPhrase);

         }
         else
         {
            throw new java.lang.RuntimeException("A passphrase is mandatory.");
         }
      }
      else
      {
         throw new java.lang.RuntimeException("Null passphrase.");
      }
   }

   // Constructor with previously owned mnemonic words and passPhrase.
   // This will import an existing wallet from blockchain.
   public TezosWallet(String mnemonicWords, String passPhrase) throws Exception
   {
      if(mnemonicWords != null)
      {
         if(mnemonicWords.length() > 0)
         {
            if(passPhrase != null)
            {
               if(passPhrase.length() > 0)
               {

                  // Creates a unique copy and initializes libsodium native library.
                  Random rand = new Random();
                  int n = rand.nextInt(1000000) + 1;
                  this.myRandomID = n;
                  this.sodium = new MySodium(String.valueOf(n));

                  // Converts passPhrase String to a byte array, respecting char values.
                  byte[] c = new byte[passPhrase.length()];
                  for(int i = 0; i < passPhrase.length(); i++)
                  {
                     c[i] = (byte) passPhrase.charAt(i);
                  }

                  initStore(c);

                  // Cleans undesired characters from mnemonic words.
                  String cleanMnemonic = mnemonicWords.replace("[", "");
                  cleanMnemonic = cleanMnemonic.replace("]", "");
                  cleanMnemonic = cleanMnemonic.replace(",", " ");
                  cleanMnemonic = cleanMnemonic.replace("  ", " ");

                  // Converts mnemonicWords String to a byte array, respecting char values.
                  byte[] b = new byte[cleanMnemonic.length()];
                  for(int i = 0; i < cleanMnemonic.length(); i++)
                  {
                     b[i] = (byte) cleanMnemonic.charAt(i);
                  }

                  // Stores encypted mnemonic words into wallet's field.
                  this.mnemonicWords = encryptBytes(b, getEncryptionKey());

                  initDomainClasses();

                  generateKeys(passPhrase);

               }
               else
               {
                  throw new java.lang.RuntimeException("A passphrase is mandatory.");
               }
            }
            else
            {
               throw new java.lang.RuntimeException("Null passphrase.");
            }
         }
         else
         {
            throw new java.lang.RuntimeException("Mnemonic words are mandatory.");
         }
      }
      else
      {
         throw new java.lang.RuntimeException("Null mnemonic words.");
      }
   }

   // Constructor for previously media persisted (saved) wallet.
   // This will load an existing wallet from media.
   public TezosWallet(Boolean loadFromFile, String pathToFile, String p)
   {
      // Creates a unique copy and initializes libsodium native library.
      Random rand = new Random();
      int n = rand.nextInt(1000000) + 1;
      this.myRandomID = n;
      this.sodium = new MySodium(String.valueOf(n));

      load(pathToFile, p);

   }

   // Constructor for previously created wallet from saved encoded byte array.
   // This will load an existing wallet from an encoded byte array.
   public TezosWallet(byte[] encData, byte[] p)
   {
      // Creates a unique copy and initializes libsodium native library.
      Random rand = new Random();
      int n = rand.nextInt(1000000) + 1;
      this.myRandomID = n;
      this.sodium = new MySodium(String.valueOf(n));

      loadFromBytes(encData, p);
      
   }

   
   // v1.0.0
   public TezosWallet(String privateKey, String publicKey, String publicKeyHash, String passPhrase) throws Exception
   {
      // Imports an existing wallet from its keys.

      resetWallet();
      this.alias = "";
      this.mnemonicWords = null;

      // Creates a unique copy and initializes libsodium native library.
      Random rand = new Random();
      int n = rand.nextInt(1000000) + 1;
      this.myRandomID = n;
      this.sodium = new MySodium(String.valueOf(n));

      // Converts passPhrase String to a byte array, respecting char values.
      byte[] z = new byte[passPhrase.length()];
      for(int i = 0; i < passPhrase.length(); i++)
      {
         z[i] = (byte) passPhrase.charAt(i);
      }

      initStore(z);
      initDomainClasses();

      // Converts privateKey String to a byte array, respecting char values.
      byte[] c = new byte[privateKey.length()];
      for(int i = 0; i < privateKey.length(); i++)
      {
         c[i] = (byte) privateKey.charAt(i);
      }
      this.privateKey = encryptBytes(c, getEncryptionKey());

      // Converts publicKey String to a byte array, respecting char values.
      byte[] d = new byte[publicKey.length()];
      for(int i = 0; i < publicKey.length(); i++)
      {
         d[i] = (byte) publicKey.charAt(i);
      }
      this.publicKey = encryptBytes(d, getEncryptionKey());

      // Converts publicKeyHash String to a byte array, respecting char values.
      byte[] e = new byte[publicKeyHash.length()];
      for(int i = 0; i < publicKeyHash.length(); i++)
      {
         e[i] = (byte) publicKeyHash.charAt(i);
      }
      this.publicKeyHash = encryptBytes(e, getEncryptionKey());

   }
   // v1.0.0

   public TezosWallet(String[] publicKey) throws Exception
   {
      resetWallet();
      this.alias = "";
      this.mnemonicWords = null;

      // Creates a unique copy and initializes libsodium native library.
      Random rand = new Random();
      int n = rand.nextInt(1000000) + 1;
      this.myRandomID = n;
      this.sodium = new MySodium(String.valueOf(n));

      // Converts passPhrase String to a byte array, respecting char values.
      String passPhrase = "";
      byte[] z = new byte[passPhrase.length()];
      for(int i = 0; i < passPhrase.length(); i++)
      {
         z[i] = (byte) passPhrase.charAt(i);
      }

      initStore(z);
      initDomainClasses();

      //---------------
      
      // These are our prefixes.
      byte[] edpkPrefix =
      { (byte) 13, (byte) 15, (byte) 37, (byte) 217 };
      byte[] edskPrefix =
      { (byte) 43, (byte) 246, (byte) 78, (byte) 7 };
      byte[] tz1Prefix =
      { (byte) 6, (byte) 161, (byte) 159 };
      
      // Converts publicKey String to a byte array, respecting char values.
      byte[] d = new byte[publicKey[0].length()];
      for(int i = 0; i < publicKey[0].length(); i++)
      {
         d[i] = (byte) publicKey[0].charAt(i);
      }
      
      // Creates Tezos Public Key.
      byte[] prefixedPubKey = new byte[36];
      System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);
      System.arraycopy(d, 0, prefixedPubKey, 4, 32);

      byte[] firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
      byte[] prefixedPubKeyWithChecksum = new byte[40];
      System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);
      System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);

      // Encrypts and stores Public Key into wallet's class property.
      this.publicKey = encryptBytes(Base58.encode(prefixedPubKeyWithChecksum).getBytes(), getEncryptionKey());

      //---------------
      
      
      
  
      String publicKeyHash = getPublicKeyHash(publicKey[0]);
      // Converts publicKeyHash String to a byte array, respecting char values.
      byte[] e = new byte[publicKeyHash.length()];
      for(int i = 0; i < publicKeyHash.length(); i++)
      {
         e[i] = (byte) publicKeyHash.charAt(i);
      }
      
      this.publicKeyHash = encryptBytes(e, getEncryptionKey());
      
   }

   
   private void initDomainClasses()
   {
      this.rpc = new Rpc();
      this.crypto = new Crypto();
   }

   // This method generates the Private Key, Public Key and Public Key hash (Tezos
   // address).
   private void generateKeys(String passphrase) throws Exception
   {

      // Decrypts the mnemonic words stored in class properties.
      byte[] input = decryptBytes(this.mnemonicWords, getEncryptionKey());

      // Converts mnemonics back into String.
      StringBuilder builder = new StringBuilder();
      for(byte anInput : input)
      {
         builder.append((char) (anInput));
      }

      MnemonicCode mc = new MnemonicCode();
      List<String> items = Arrays.asList((builder.toString()).split(" "));
      byte[] src_seed = mc.toSeed(items, passphrase);
      byte[] seed = Arrays.copyOfRange(src_seed, 0, 32);

      byte[] sodiumPrivateKey = zeros(32 * 2);
      byte[] sodiumPublicKey = zeros(32);
      int r = sodium.crypto_sign_seed_keypair(sodiumPublicKey, sodiumPrivateKey, seed);

      // These are our prefixes.
      byte[] edpkPrefix =
      { (byte) 13, (byte) 15, (byte) 37, (byte) 217 };
      byte[] edskPrefix =
      { (byte) 43, (byte) 246, (byte) 78, (byte) 7 };
      byte[] tz1Prefix =
      { (byte) 6, (byte) 161, (byte) 159 };

      // Creates Tezos Public Key.
      byte[] prefixedPubKey = new byte[36];
      System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);
      System.arraycopy(sodiumPublicKey, 0, prefixedPubKey, 4, 32);

      byte[] firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
      byte[] prefixedPubKeyWithChecksum = new byte[40];
      System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);
      System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);

      // Encrypts and stores Public Key into wallet's class property.
      this.publicKey = encryptBytes(Base58.encode(prefixedPubKeyWithChecksum).getBytes(), getEncryptionKey());

      // Creates Tezos Private (secret) Key.
      byte[] prefixedSecKey = new byte[68];
      System.arraycopy(edskPrefix, 0, prefixedSecKey, 0, 4);
      System.arraycopy(sodiumPrivateKey, 0, prefixedSecKey, 4, 64);

      firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedSecKey);
      byte[] prefixedSecKeyWithChecksum = new byte[72];
      System.arraycopy(prefixedSecKey, 0, prefixedSecKeyWithChecksum, 0, 68);
      System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedSecKeyWithChecksum, 68, 4);

      // Encrypts and stores Private Key into wallet's class property.
      this.privateKey = encryptBytes(Base58.encode(prefixedSecKeyWithChecksum).getBytes(), getEncryptionKey());

      // Creates Tezos Public Key Hash (Tezos address).
      byte[] genericHash = new byte[20];
      int s = sodium.crypto_generichash(genericHash, genericHash.length, sodiumPublicKey, sodiumPublicKey.length,
            sodiumPublicKey, 0);

      byte[] prefixedGenericHash = new byte[23];
      System.arraycopy(tz1Prefix, 0, prefixedGenericHash, 0, 3);
      System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

      firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
      byte[] prefixedPKhashWithChecksum = new byte[27];
      System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
      System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

      String pkHash = Base58.encode(prefixedPKhashWithChecksum);

      // Encrypts and stores Public Key Hash into wallet's class property.
      this.publicKeyHash = encryptBytes(Base58.encode(prefixedPKhashWithChecksum).getBytes(), getEncryptionKey());

   }

   // Generates the mnemonic words.
   private void generateMnemonic() throws Exception
   {
      String result = "";

      MnemonicCode mc = new MnemonicCode();
      byte[] bytes = new byte[20];
      new java.util.Random().nextBytes(bytes);
      ArrayList<String> code = (ArrayList<String>) mc.toMnemonic(bytes);
      result = code.toString();

      // Converts the string with the words to a byte array, respecting char values.
      String strMessage = "";
      strMessage = (String) code.toString();

      // Cleans undesired characters from mnemonic words.
      String cleanMnemonic = strMessage.replace("[", "");
      cleanMnemonic = cleanMnemonic.replace("]", "");
      cleanMnemonic = cleanMnemonic.replace(",", " ");
      cleanMnemonic = cleanMnemonic.replace("  ", " ");

      byte[] b = new byte[cleanMnemonic.length()];
      for(int i = 0; i < cleanMnemonic.length(); i++)
      {
         b[i] = (byte) cleanMnemonic.charAt(i);
      }

      // Stores encypted mnemonic words into wallet's field.
      this.mnemonicWords = encryptBytes(b, getEncryptionKey());

   }

   // Encryption routine.
   // Uses AES encryption.
   private static byte[] encryptBytes(byte[] original, byte[] key)
   {
      try
      {
         SecretKeySpec keySpec = null;
         Cipher cipher = null;
         keySpec = new SecretKeySpec(key, "AES");
         cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
         cipher.init(Cipher.ENCRYPT_MODE, keySpec);

         return cipher.doFinal(original);
      } catch (Exception e)
      {
         e.printStackTrace();
      }
      return null;
   }

   // Decryption routine.
   private static byte[] decryptBytes(byte[] encrypted, byte[] key)
   {
      try
      {
         SecretKeySpec keySpec = null;
         Cipher cipher = null;
         keySpec = new SecretKeySpec(key, "AES");
         cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
         cipher.init(Cipher.DECRYPT_MODE, keySpec);

         return cipher.doFinal(encrypted);
      } catch (Exception e)
      {
         e.printStackTrace();
      }
      return null;
   }

   // Retrieves mnemonic words upon user request.
   public String getMnemonicWords()
   {
      StringBuilder builder = new StringBuilder();

      if(this.mnemonicWords != null)
      {
         byte[] decrypted = decryptBytes(this.mnemonicWords, getEncryptionKey());

         for(byte aDecrypted : decrypted)
         {
            builder.append((char) (aDecrypted));
         }
      }

      return builder.toString();
   }

   // Retrieves the Public Key Hash (Tezos user address) upon user request.
   public String getPublicKeyHash()
   {
      if(this.publicKeyHash != null)
      {
         if(this.publicKeyHash.length > 0)
         {

            byte[] decrypted = decryptBytes(this.publicKeyHash, getEncryptionKey());

            StringBuilder builder = new StringBuilder();
            for(byte aDecrypted : decrypted)
            {
               builder.append((char) (aDecrypted));
            }
            return builder.toString();
         }
         else
         {
            throw new java.lang.RuntimeException("Error getting public key hash.");
         }
      }
      else
      {
         throw new java.lang.RuntimeException("Error getting public key hash.");
      }

   }

   // Retrieves the account balance.
   public String getBalance() throws Exception
   {
      if(this.publicKeyHash != null)
      {
         if(this.publicKeyHash.length > 0)
         {
            if(this.crypto.checkAddress(this.getPublicKeyHash()))
            {

               BigDecimal tezBalance = new BigDecimal(String.valueOf(BigDecimal.ZERO));

               byte[] decrypted = decryptBytes(this.publicKeyHash, getEncryptionKey());

               StringBuilder builder = new StringBuilder();
               for(byte aDecrypted : decrypted)
               {
                  builder.append((char) (aDecrypted));
               }

               // Get balance from Tezos blockchain.
               String strBalance = (String) rpc.getBalance(builder.toString()).get("result");

               // Test if is numeric;
               if(isNumeric(strBalance.replaceAll("[^\\d.]", "")))
               {
                  // Test if greater then zero.
                  if(Long.parseLong(strBalance.replaceAll("[^\\d.]", "")) > 0)
                  {
                     tezBalance = new BigDecimal(strBalance.replaceAll("[^\\d.]", "")).divide(BigDecimal.valueOf(UTEZ));
                  }

                  // Updates wallet´s balance property for retrieval.
                  this.balance = String.valueOf(tezBalance) + " " + TEZOS_SYMBOL;
               }
               else
               {
                  throw new java.lang.RuntimeException(strBalance);
               }

               return this.balance;
            }
            else
            {
               throw new java.lang.RuntimeException("Invalid address.");
            }
         }
         else
         {
            throw new java.lang.RuntimeException("A valid Tezos address is mandatory.");
         }
      }
      else
      {
         throw new java.lang.RuntimeException("No wallet found to get balance from.");
      }

   }

   // Retrieves wallet alias.
   public String getAlias()
   {
      return this.alias;
   }

   // Sets wallet alias.
   public void setAlias(String newAlias)
   {
      this.alias = newAlias;
   }

   // Transfers funds (XTZ) from this wallet to another one.
   // Returns to the user the operation results from Tezos node.
   public JSONObject send(String from, String to, BigDecimal amount, BigDecimal fee, String gasLimit,
                          String storageLimit)
         throws Exception
   {
      JSONObject result = new JSONObject();

      if((from != null) && (to != null) && (amount != null))
      {
         if((this.crypto.checkAddress(from) == true) && (this.crypto.checkAddress(to) == true))
         {

            if(from.length() > 0)
            {
               if(to.length() > 0)
               {
                  if(amount.compareTo(BigDecimal.ZERO) > 0)
                  {
                     if(fee.compareTo(BigDecimal.ZERO) > 0)
                     {

                        // Prepares keys.
                        EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash,
                              this.myRandomID);
                        encKeys.setEncIv(this.encIv);
                        encKeys.setEncP(this.encPass);

                        result = rpc.transfer(from, to, amount, fee, gasLimit, storageLimit, encKeys);
                     }
                     else
                     {
                        throw new java.lang.RuntimeException("Fee must be greater than zero.");
                     }

                  }
                  else
                  {
                     throw new java.lang.RuntimeException("Amount must be greater than zero.");
                  }
               }
               else
               {
                  throw new java.lang.RuntimeException("Recipient (To field) is mandatory.");
               }
            }
            else
            {
               throw new java.lang.RuntimeException("Sender (From field) is mandatory.");
            }
         }
         else
         {
            throw new java.lang.RuntimeException("Valid Tezos addresses are required in From and To fields.");
         }
      }
      else
      {
         throw new java.lang.RuntimeException("The fields: From, To and Amount are required.");
      }

      return result;

   }

   private void initStore(byte[] toHash)
   {
      try
      {
         String pString = new String(toHash, "UTF-8");

         int hashedP = pString.hashCode();
         String strHash = String.valueOf(hashedP);
         while (strHash.length() < 16)
         {
            strHash = strHash + strHash;
         }
         strHash = strHash.substring(0, 16); // 16 bytes needed.
         pString = strHash;

         SecretKey secretKey = createKey();
         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         cipher.init(Cipher.ENCRYPT_MODE, secretKey);

         byte[] encryptionIv = cipher.getIV();
         byte[] pBytes = pString.getBytes("UTF-8");
         byte[] encPBytes = cipher.doFinal(pBytes);
         String encP = Base64.getEncoder().encodeToString(encPBytes);
         String encryptedIv = Base64.getEncoder().encodeToString(encryptionIv);

         this.encPass = encP;
         this.encIv = encryptedIv;

         Global.initKeyStore();
         Global.myKeyStore.load(null, encP.toCharArray());
         KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);

         KeyStore.ProtectionParameter entryPassword = new KeyStore.PasswordProtection(encP.toCharArray());
         Global.myKeyStore.setEntry(TZJ_KEY_ALIAS + this.myRandomID, secretKeyEntry, entryPassword);

      } catch (Exception e)
      {
         throw new RuntimeException("Could not initialize Android KeyStore.", e);
      }
   }

   private SecretKey createKey()
   {

      try
      {

         KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
         keyGenerator.init(128);

         return keyGenerator.generateKey();

      } catch (Exception e)
      {
         throw new RuntimeException("Failed to create a symetric key", e);
      }

   }

   private byte[] getEncryptionKey()
   {
      try
      {
         String base64EncryptedPassword = this.encPass;
         String base64EncryptionIv = this.encIv;

         byte[] encryptionIv = Base64.getDecoder().decode(base64EncryptionIv);
         byte[] encryptionPassword = Base64.getDecoder().decode(base64EncryptedPassword);

         KeyStore.ProtectionParameter entryPassword = new KeyStore.PasswordProtection(
               base64EncryptedPassword.toCharArray());
         KeyStore.SecretKeyEntry entry = (SecretKeyEntry) Global.myKeyStore.getEntry(TZJ_KEY_ALIAS + this.myRandomID,
               entryPassword);

         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         cipher.init(Cipher.DECRYPT_MODE, entry.getSecretKey(), new IvParameterSpec(encryptionIv));
         byte[] passwordBytes = cipher.doFinal(encryptionPassword);
         String password = new String(passwordBytes, "UTF-8");

         return passwordBytes;

      } catch (Exception e)
      {
         return null;
      }

   }

   public static byte[] getEncryptionKey(EncKeys keys)
   {
      try
      {
         String base64EncryptedPassword = keys.getEncP();
         String base64EncryptionIv = keys.getEncIv();

         byte[] encryptionIv = Base64.getDecoder().decode(base64EncryptionIv);
         byte[] encryptionPassword = Base64.getDecoder().decode(base64EncryptedPassword);

         KeyStore.ProtectionParameter entryPassword = new KeyStore.PasswordProtection(
               base64EncryptedPassword.toCharArray());
         KeyStore.SecretKeyEntry entry = (SecretKeyEntry) Global.myKeyStore
               .getEntry(TZJ_KEY_ALIAS + keys.getMyRandomID(), entryPassword);

         Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
         cipher.init(Cipher.DECRYPT_MODE, entry.getSecretKey(), new IvParameterSpec(encryptionIv));
         byte[] passwordBytes = cipher.doFinal(encryptionPassword);
         String password = new String(passwordBytes, "UTF-8");

         return passwordBytes;

      } catch (Exception e)
      {
         return null;
      }

   }

   public void save(String pathToFile)
   {

      if(pathToFile.isEmpty() == false)
      {
         // Persists the wallet to media from memory.
         FileOutputStream fop = null;
         File file;

         try
         {
            file = new File(pathToFile);
            fop = new FileOutputStream(file);

            // Checks if file exists, then creates it.
            if(!file.exists())
            {
               file.createNewFile();
            }

            // Creates a text version of the wallet.
            String myWalletData = Base64.getEncoder().encodeToString(this.alias.getBytes()) + ";"
                  + Base64.getEncoder().encodeToString(this.publicKey) + ";"
                  + Base64.getEncoder().encodeToString(this.publicKeyHash) + ";"
                  + Base64.getEncoder().encodeToString(this.privateKey) + ";"
                  + Base64.getEncoder().encodeToString(this.balance.getBytes()) + ";"
                  + Base64.getEncoder().encodeToString(this.mnemonicWords) + ";";

            byte[] contentInBytes = myWalletData.getBytes();

            fop.write(contentInBytes);
            fop.flush();
            fop.close();

         } catch (IOException e)
         {
            e.printStackTrace();
            throw new java.lang.RuntimeException("Error when trying to save the wallet to media.");
         } finally
         {
            try
            {
               if(fop != null)
               {
                  fop.close();
               }
            } catch (IOException e)
            {
               e.printStackTrace();
               throw new java.lang.RuntimeException("Error when trying to save the wallet to media.");
            }
         }
      }
      else
      {
         throw new java.lang.RuntimeException("A filename and path are required to save the wallet.");
      }
   }

   public byte[] saveToBytes()
   {

      String myWalletData = Base64.getEncoder().encodeToString(this.alias.getBytes() != null ? this.alias.getBytes() : " ".getBytes()) + ";"
            + Base64.getEncoder().encodeToString(this.publicKey != null ? this.publicKey : " ".getBytes()  ) + ";"
            + Base64.getEncoder().encodeToString(this.publicKeyHash != null ? this.publicKeyHash : " ".getBytes()) + ";"
            + Base64.getEncoder().encodeToString(this.privateKey != null ? this.privateKey : " ".getBytes() ) + ";"
            + Base64.getEncoder().encodeToString(this.balance.getBytes() != null ? this.balance.getBytes() : " ".getBytes() ) + ";"
            + Base64.getEncoder().encodeToString(this.mnemonicWords != null ? this.mnemonicWords : " ".getBytes() ) + ";";
   
      return myWalletData.getBytes();
   
   }

   
   public void load(String pathToFile, String p)
   {
      // Loads a wallet from media to memory.

      if(pathToFile.isEmpty() == false)
      {
         File file = new File(pathToFile);
         FileInputStream fis = null;
         String myWalletString = "";

         try
         {
            fis = new FileInputStream(file);

            int content;
            while ((content = fis.read()) != -1)
            {
               myWalletString = myWalletString + (char) content;
            }

            if(myWalletString.length() > 0)
            {
               resetWallet();

               String[] fields = myWalletString.split("\\;", -1);
               this.alias = new String(Base64.getDecoder().decode(fields[0]), "UTF-8");
               this.publicKey = Base64.getDecoder().decode(fields[1]);
               this.publicKeyHash = Base64.getDecoder().decode(fields[2]);
               this.privateKey = Base64.getDecoder().decode(fields[3]);
               this.balance = new String(Base64.getDecoder().decode(fields[4]), "UTF-8");
               this.mnemonicWords = Base64.getDecoder().decode(fields[5]);

               // Converts passPhrase String to a byte array, respecting char values.
               byte[] c = new byte[p.length()];
               for(int i = 0; i < p.length(); i++)
               {
                  c[i] = (byte) p.charAt(i);
               }

               initStore(c);
               initDomainClasses();
            }

         } catch (IOException e)
         {
            e.printStackTrace();
            throw new java.lang.RuntimeException("Error when trying to load wallet from media.");
         } finally
         {
            try
            {
               if(fis != null)
               {
                  fis.close();
               }
            } catch (IOException ex)
            {
               ex.printStackTrace();
               throw new java.lang.RuntimeException("Error when trying to load wallet from media.");
            }
         }
      }
      else
      {
         throw new java.lang.RuntimeException("A filename and path are required to load a wallet.");
      }
   }

   public void loadFromBytes(byte[] encData, byte[] p)
   {
      try
      {
         
         // Convert bytes back to string.
         StringBuilder builder = new StringBuilder();
         for(byte anInput : encData)
         {
            builder.append((char) (anInput));
         }
       
         resetWallet();
     
         String[] fields = builder.toString().split("\\;", -1);
         this.alias = new String(Base64.getDecoder().decode(fields[0]), "UTF-8");
         this.publicKey = Base64.getDecoder().decode(fields[1]);
         this.publicKeyHash = Base64.getDecoder().decode(fields[2]);
         this.privateKey = Base64.getDecoder().decode(fields[3]);
         this.balance = new String(Base64.getDecoder().decode(fields[4]), "UTF-8");
         this.mnemonicWords = Base64.getDecoder().decode(fields[5]);
              
         initStore(p);
         initDomainClasses();
         }
         catch(Exception e)
         {}     
   }

   private String buildStringFromByte(byte[] input)
   {
      StringBuilder builder = new StringBuilder();
      for(byte anInput : input)
      {
         builder.append((char) (anInput));
      }
      return builder.toString();
   }

   private byte[] buildByteFromString(String input)
   {
      byte[] d = new byte[input.length()];
      for(int i = 0; i < input.length(); i++)
      {
         d[i] = (byte) input.charAt(i);
      }

      return d;
   }

   // Removes the wallet data from memory.
   private void resetWallet()
   {
      this.privateKey = null;
      this.mnemonicWords = null;
      this.encPass = null;
      this.encIv = null;
      this.publicKeyHash = null;
      this.publicKey = null;
      this.balance = "";
      this.transactions = null;
      this.rpc = null;
   }

   // Checks if a give phrase is the correct wallet passphrase.
   public Boolean checkPhrase(String phrase)
   {
      Boolean result;

      try
      {
         MnemonicCode mc = new MnemonicCode();
         List<String> items = Arrays.asList((this.getMnemonicWords()).split(" "));
         byte[] src_seed = mc.toSeed(items, phrase);
         byte[] seed = Arrays.copyOfRange(src_seed, 0, 32);

         byte[] sodiumPrivateKey = zeros(32 * 2);
         byte[] sodiumPublicKey = zeros(32);
         int r = sodium.crypto_sign_seed_keypair(sodiumPublicKey, sodiumPrivateKey, seed);

         // These are our prefixes.
         byte[] edpkPrefix =
         { (byte) 13, (byte) 15, (byte) 37, (byte) 217 };

         // Creates Tezos Public Key.
         byte[] prefixedPubKey = new byte[36];
         System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);
         System.arraycopy(sodiumPublicKey, 0, prefixedPubKey, 4, 32);

         byte[] firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
         byte[] prefixedPubKeyWithChecksum = new byte[40];
         System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);
         System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);

         String publicKey = Base58.encode(prefixedPubKeyWithChecksum);

         // Converts this.publicKey into String.
         StringBuilder builder = new StringBuilder();
         byte[] input = decryptBytes(this.publicKey, getEncryptionKey());
         for(byte anInput : input)
         {
            builder.append((char) (anInput));
         }

         if(publicKey.equals(builder.toString()))
         {
            result = true;
         }
         else
         {
            result = false;
         }

      } catch (Exception e)
      {
         result = false;
      }

      return result;
   }

   public static byte[] zeros(int n)
   {
      return new byte[n];
   }

   public void setProxy(String proxyHost, String proxyPort)
   {
      Global.proxyHost = proxyHost;
      Global.proxyPort = proxyPort;
   }

   public void setIgnoreInvalidCertificates(Boolean ignore)
   {
      Global.ignoreInvalidCertificates = ignore;
   }

   public void setProvider(String provider)
   {
      Global.defaultProvider = provider;
   }

   public void setLedgerDerivationPath(String derivationPath)
   {
      Global.ledgerDerivationPath = derivationPath;
   }

   public void setLedgerTezosFolderPath(String ledgerTezosFolderPath)
   {
      Global.ledgerTezosFolderPath = ledgerTezosFolderPath;
   }

   public void setLedgerTezosFilePath(String ledgerTezosFilePath)
   {
      Global.ledgerTezosFilePath = ledgerTezosFilePath;
   }

   public void setKTtoTZFee(String strBigDecimalFee)
   {
      Global.KT_TO_TZ_FEE = strBigDecimalFee;
   }

   public void setKTtoTZGasLimit(String gasLimit)
   {
      Global.KT_TO_TZ_GAS_LIMIT = gasLimit;
   }

   public void setKTtoTZStorageLimit(String storageLimit)
   {
      Global.KT_TO_TZ_STORAGE_LIMIT = storageLimit;
   }
   
   // v0.9.9

   // Delegate to.
   // Returns to the user the operation results from Tezos node.
   public JSONObject delegate(String delegateFrom, String delegateTo, BigDecimal fee, String gasLimit,
                              String storageLimit)
         throws Exception
   {
      JSONObject result = new JSONObject();

      if((delegateFrom != null) && (delegateTo != null))
      {
         if((this.crypto.checkAddress(delegateFrom) == true) && (this.crypto.checkAddress(delegateTo) == true))
         {

            if(delegateFrom.length() > 0)
            {
               if(delegateTo.length() > 0)
               {
                  if(fee.compareTo(BigDecimal.ZERO) > 0)
                  {

                     // Prepares keys.
                     EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash,
                           this.myRandomID);
                     encKeys.setEncIv(this.encIv);
                     encKeys.setEncP(this.encPass);

                     result = rpc.delegate(delegateFrom, delegateTo, fee, gasLimit, storageLimit, encKeys);
                  }
                  else
                  {
                     throw new java.lang.RuntimeException("Fee must be greater than zero.");
                  }

               }
               else
               {
                  throw new java.lang.RuntimeException("Delegate (delegateTo field) is mandatory.");
               }
            }
            else
            {
               throw new java.lang.RuntimeException("Delegator (delegateFrom field) is mandatory.");
            }
         }
         else
         {
            throw new java.lang.RuntimeException(
                  "Valid Tezos addresses are required in delegateFrom and delegateTo fields.");
         }
      }
      else
      {
         throw new java.lang.RuntimeException("The fields: delegateFrom, delegateTo are required.");
      }

      return result;

   }

   // Originate.
   // Returns to the user the operation results from Tezos node.
   public JSONObject originate(String from, Boolean spendable, Boolean delegatable, BigDecimal fee, String gasLimit,
                               String storageLimit, BigDecimal amount, String code, String storage)
         throws Exception
   {
      JSONObject result = new JSONObject();

      if((from != null))
      {
         if(this.crypto.checkAddress(from) == true)
         {

            if(from.length() > 0)
            {
               if(fee.compareTo(BigDecimal.ZERO) > 0)
               {

                  // Prepares keys.
                  EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash, this.myRandomID);
                  encKeys.setEncIv(this.encIv);
                  encKeys.setEncP(this.encPass);

                  result = rpc.originate(from, spendable, delegatable, fee, gasLimit, storageLimit, amount, code,
                        storage, encKeys);
               }
               else
               {
                  throw new java.lang.RuntimeException("Fee must be greater than zero.");
               }

            }
            else
            {
               throw new java.lang.RuntimeException("Delegator (delegateFrom field) is mandatory.");
            }
         }
         else
         {
            throw new java.lang.RuntimeException("Valid Tezos address is required in delegate field.");
         }
      }
      else
      {
         throw new java.lang.RuntimeException("The field: delegate is required.");
      }

      return result;

   }

   // Undelegate.
   // Returns to the user the operation results from Tezos node.
   public JSONObject undelegate(String delegateFrom, BigDecimal fee) throws Exception
   {
      JSONObject result = new JSONObject();

      if(delegateFrom != null)
      {
         if(this.crypto.checkAddress(delegateFrom) == true)
         {

            if(delegateFrom.length() > 0)
            {

               if(fee.compareTo(BigDecimal.ZERO) > 0)
               {

                  // Prepares keys.
                  EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash, this.myRandomID);
                  encKeys.setEncIv(this.encIv);
                  encKeys.setEncP(this.encPass);

                  result = rpc.undelegate(delegateFrom, fee, encKeys);
               }
               else
               {
                  throw new java.lang.RuntimeException("Fee must be greater than zero.");
               }

            }
            else
            {
               throw new java.lang.RuntimeException("Delegator (delegateFrom field) is mandatory.");
            }
         }
         else
         {
            throw new java.lang.RuntimeException("Valid Tezos address is required in delegateFrom field.");
         }
      }
      else
      {
         throw new java.lang.RuntimeException("The field: delegateFrom is required.");
      }

      return result;

   }

   // Clears the transaction batch.
   public void clearTransactionBatch()
   {
      this.transactionBatch = null;

      ArrayList<BatchTransactionItem> transactions = new ArrayList<BatchTransactionItem>();
      this.transactionBatch = transactions;
   }

   // Adds a transaction to the batch.
   public void addTransactionToBatch(String from, String to, BigDecimal amount, BigDecimal fee) throws Exception
   {
      if(this.transactionBatch != null)
      {
         Integer index = 0;

         index = this.transactionBatch.size() + 1;
         BatchTransactionItem item = new BatchTransactionItem(from, to, amount, fee, index);
         this.transactionBatch.add(item);
      }
      else
      {
         throw new java.lang.RuntimeException(
               "Cannot add transaction as batch has not been initialized. Call clearTransactionBatch() first.");
      }
   }

   // Adds a transaction to the batch.
   public ArrayList<BatchTransactionItem> getTransactionList()
   {
      return this.transactionBatch;
   }

   // Sends all transactions in the batch to the blockchain and clears the batch.
   public JSONObject flushTransactionBatch(String gasLimit, String storageLimit) throws Exception
   {
      JSONObject result = new JSONObject();

      if(this.transactionBatch != null)
      {
         if(this.transactionBatch.isEmpty() == false)
         {
            // Prepares keys.
            EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash, this.myRandomID);
            encKeys.setEncIv(this.encIv);
            encKeys.setEncP(this.encPass);

            result = rpc.sendBatchTransactions(this.transactionBatch, encKeys, gasLimit, storageLimit);

            // Clears transaction batch.
            this.transactionBatch = null;

         }
         else
         {
            throw new java.lang.RuntimeException(
                  "Cannot send as batch has no transactions. Add some with addTransactionToBatch() first.");
         }

      }
      else
      {
         throw new java.lang.RuntimeException(
               "Cannot send as batch has not been initialized. Call clearTransactionBatch() first.");
      }

      return result;
   }

   // Sends all transactions in the batch to the blockchain and clears the batch.
   public JSONObject flushTransactionBatch() throws Exception
   {
      JSONObject result = new JSONObject();

      if(this.transactionBatch != null)
      {
         if(this.transactionBatch.isEmpty() == false)
         {
            // Prepares keys.
            EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash, this.myRandomID);
            encKeys.setEncIv(this.encIv);
            encKeys.setEncP(this.encPass);

            result = rpc.sendBatchTransactions(this.transactionBatch, encKeys, null, null);

            // Clears transaction batch.
            this.transactionBatch = null;

         }
         else
         {
            throw new java.lang.RuntimeException(
                  "Cannot send as batch has no transactions. Add some with addTransactionToBatch() first.");
         }

      }
      else
      {
         throw new java.lang.RuntimeException(
               "Cannot send as batch has not been initialized. Call clearTransactionBatch() first.");
      }

      return result;
   }

   public Boolean waitForResult(String operationHash, Integer numberOfBlocksToWait) throws Exception
   {
      return rpc.waitForResult(operationHash, numberOfBlocksToWait);
   }

   // Calls a smart contract entrypoint passing parameters.
   // Returns to the user the operation results from Tezos node.
   public JSONObject callContractEntryPoint(String from, String contract, BigDecimal amount, BigDecimal fee,
                                            String gasLimit, String storageLimit, String entrypoint,
                                            String[] parameters, Boolean rawParameter, String smartContractType)
         throws Exception
   {
      JSONObject result = new JSONObject();

      if((from != null) && (contract != null) && (amount != null) && (entrypoint != null) && (parameters != null))
      {
         if((this.crypto.checkAddress(from) == true) && (this.crypto.checkAddress(contract) == true))
         {

            if(from.length() > 0)
            {
               if(contract.length() > 0)
               {
                  if(amount.compareTo(BigDecimal.ZERO) >= 0)
                  {
                     if(fee.compareTo(BigDecimal.ZERO) > 0)
                     {

                        // Prepares keys.
                        EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash,
                              this.myRandomID);
                        encKeys.setEncIv(this.encIv);
                        encKeys.setEncP(this.encPass);

                        result = rpc.callContractEntryPoint(from, contract, amount, fee, gasLimit, storageLimit,
                              encKeys, entrypoint, parameters, rawParameter, smartContractType);
                     }
                     else
                     {
                        throw new java.lang.RuntimeException("Fee must be greater than zero.");
                     }

                  }
                  else
                  {
                     throw new java.lang.RuntimeException("Amount must be greater than or equal to zero.");
                  }
               }
               else
               {
                  throw new java.lang.RuntimeException("Contract field is mandatory.");
               }
            }
            else
            {
               throw new java.lang.RuntimeException("Sender (From field) is mandatory.");
            }
         }
         else
         {
            throw new java.lang.RuntimeException("Valid Tezos addresses are required in From and To fields.");
         }
      }
      else
      {
         throw new java.lang.RuntimeException(
               "The fields: From, Contract, Amount, Entrypoint and Parameters are required.");
      }

      return result;

   }

   public JSONObject waitForAndCheckResultByDestinationAddress(String address, Integer numberOfBlocksToWait) throws Exception
   {
      return rpc.waitForAndCheckResultByDestinationAddress(address, numberOfBlocksToWait);
   }

   public Boolean waitForAndCheckResult(String operationHash, Integer numberOfBlocksToWait) throws Exception
   {
      return rpc.waitForAndCheckResult(operationHash, numberOfBlocksToWait);
   }

   // Retrieves the Public Key Hash (Tezos user address) upon user request, given a public key.
   public String getPublicKeyHash(String publicKey)
   {
      if(publicKey != null)
      {
         if(publicKey.length() > 0)
         {
             // These are our prefixes.
             byte[] edpkPrefix =
             { (byte) 13, (byte) 15, (byte) 37, (byte) 217 };
             byte[] edskPrefix =
             { (byte) 43, (byte) 246, (byte) 78, (byte) 7 };
             byte[] tz1Prefix =
             { (byte) 6, (byte) 161, (byte) 159 };

             
             byte[] sodiumPublicKey = zeros(32);
     	       sodiumPublicKey = HEX.decode(publicKey);   	      
             
             // Creates Tezos Public Key.
             byte[] prefixedPubKey = new byte[36];
             System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);
             System.arraycopy(sodiumPublicKey, 0, prefixedPubKey, 4, 32);
             
             byte[] firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
             byte[] prefixedPubKeyWithChecksum = new byte[40];
             System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);
             System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);
 
             // Encrypts and stores Public Key into wallet's class property.
             this.publicKey = encryptBytes(Base58.encode(prefixedPubKeyWithChecksum).getBytes(), getEncryptionKey());
             
             // Creates Tezos Public Key Hash (Tezos address).
             byte[] genericHash = new byte[20];
             int s = sodium.crypto_generichash(genericHash, genericHash.length, sodiumPublicKey, sodiumPublicKey.length,
                   sodiumPublicKey, 0);

             byte[] prefixedGenericHash = new byte[23];
             System.arraycopy(tz1Prefix, 0, prefixedGenericHash, 0, 3);
             System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

             firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
             byte[] prefixedPKhashWithChecksum = new byte[27];
             System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
             System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

             String pkHash = Base58.encode(prefixedPKhashWithChecksum);
        	     
             // Encrypts and stores Public Key Hash into wallet's class property.
             this.publicKeyHash = encryptBytes(Base58.encode(prefixedPKhashWithChecksum).getBytes(), getEncryptionKey());
             
             return pkHash;
         }
         else
         {
            throw new java.lang.RuntimeException("publicKey argument is mandatory.");
         }
      }
      else
      {
         throw new java.lang.RuntimeException("publicKey argument is mandatory");
      }

   }

   public Boolean hasPrivateKey()
   {
      return ( this.privateKey == null ? false : true);
   }

   public JSONObject transferImplicit(String contract, String implicitAddress, String managerAddress, BigDecimal amount) throws Exception
   {
      JSONObject result = new JSONObject();

      if((contract != null) && (implicitAddress != null) && (managerAddress != null) && (amount != null))
      {
         if(contract.length() > 0)
         {
            if(implicitAddress.length() > 0)
            {
               if(managerAddress.length() > 0)
               {

                  if((this.crypto.checkAddress(contract) == true) && (this.crypto.checkAddress(implicitAddress) == true) && (this.crypto.checkAddress(managerAddress) == true))
                  {
                     if((contract.substring(0, 2).toLowerCase().equals("kt") == true) && (implicitAddress.substring(0, 2).toLowerCase().equals("tz") == true) && (managerAddress.substring(0, 2).toLowerCase().equals("tz") == true))
                     {   
                        if(amount.compareTo(BigDecimal.ZERO) > 0)
                        {
                           // Prepares keys.
                           EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash,
                                 this.myRandomID);
                           encKeys.setEncIv(this.encIv);
                           encKeys.setEncP(this.encPass);
      
                           result = rpc.transferImplicit(contract, implicitAddress, managerAddress, amount, encKeys);
      
                        }
                        else
                        {
                           throw new java.lang.RuntimeException("Amount must be greater than zero.");
                        }
                     }
                     else
                     {
                        throw new java.lang.RuntimeException("From address must be KT, To address must be TZ and Manager address must be TZ.");
                     }
                     
                  }
                  else
                  {
                     throw new java.lang.RuntimeException("Valid Tezos addresses are required in From, To and Manager fields.");
                  }

               }
               else
               {
                  throw new java.lang.RuntimeException("Manager address is mandatory.");
               }

            }
            else
            {
               throw new java.lang.RuntimeException("Recipient (To field) is mandatory.");
            }
            
         }
         else
         {
            throw new java.lang.RuntimeException("Sender (From field) is mandatory.");
         }

      }
      else
      {
         throw new java.lang.RuntimeException("The fields: From, To, Manager and Amount are required.");
      }

      return result;

   }

   public JSONObject transferToContract(String contract, String destinationKT, String managerAddress, BigDecimal amount) throws Exception
   {
      JSONObject result = new JSONObject();

      if((contract != null) && (destinationKT != null)  && (managerAddress != null) && (amount != null))
      {
         if(contract.length() > 0)
         {
            if(destinationKT.length() > 0)
            {
               if(managerAddress.length() > 0)
               {
                  if((this.crypto.checkAddress(contract) == true) && (this.crypto.checkAddress(destinationKT) == true) && (this.crypto.checkAddress(managerAddress) == true))
                  {
                     if((contract.substring(0, 2).toLowerCase().equals("kt") == true) && (destinationKT.substring(0, 2).toLowerCase().equals("kt") == true)  && (managerAddress.substring(0, 2).toLowerCase().equals("tz") == true) )
                     {
      
                        if(amount.compareTo(BigDecimal.ZERO) > 0)
                        {
                           // Prepares keys.
                           EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash,
                                 this.myRandomID);
                           encKeys.setEncIv(this.encIv);
                           encKeys.setEncP(this.encPass);
      
                           result = rpc.transferToContract(contract, destinationKT, managerAddress, amount, encKeys);
      
                        }
                        else
                        {
                           throw new java.lang.RuntimeException("Amount must be greater than zero.");
                        }
                        
                     }
                     else
                     {
                        throw new java.lang.RuntimeException("From address must be KT, To address must be KT and Manager address must be TZ.");
                     }
                  }
                  else
                  {
                     throw new java.lang.RuntimeException("Valid Tezos addresses are required in From, To and Manager fields.");
                  }
                  
               }
               else
               {
                  throw new java.lang.RuntimeException("Manager address is mandatory.");
               }
               
            }
            else
            {
               throw new java.lang.RuntimeException("Recipient (To field) is mandatory.");
            }
            
         }
         else
         {
            throw new java.lang.RuntimeException("Sender (From field) is mandatory.");
         }
      
      }
      else
      {
         throw new java.lang.RuntimeException("The fields: From, To, Manager and Amount are required.");
      }

      return result;

   }
   
   public JSONObject sendDelegationFromContract(String delegator, String delegate, String managerAddress) throws Exception
   {
      JSONObject result = new JSONObject();

      if((delegator != null) && (delegate != null)  && (managerAddress != null))
      {
         if(delegator.length() > 0)
         {
            if(delegate.length() > 0)
            {
               if(managerAddress.length() > 0)
               {
                  if((this.crypto.checkAddress(delegator) == true) && (this.crypto.checkAddress(delegate) == true) && (this.crypto.checkAddress(managerAddress) == true))
                  {
                     if((delegator.substring(0, 2).toLowerCase().equals("kt") == true))
                     {
                        // Prepares keys.
                        EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash,
                              this.myRandomID);
                        encKeys.setEncIv(this.encIv);
                        encKeys.setEncP(this.encPass);
   
                        result = rpc.sendDelegationFromContract(delegator, delegate, managerAddress, encKeys);
                           
                     }
                     else
                     {
                        throw new java.lang.RuntimeException("Delegator address must be KT.");
                     }
                  }
                  else
                  {
                     throw new java.lang.RuntimeException("Valid Tezos addresses are required in Delegator, Delegate and Manager fields.");
                  }
                  
               }
               else
               {
                  throw new java.lang.RuntimeException("Manager address is mandatory.");
               }
               
            }
            else
            {
               throw new java.lang.RuntimeException("Delegate is mandatory.");
            }
            
         }
         else
         {
            throw new java.lang.RuntimeException("Delegator is mandatory.");
         }
      
      }
      else
      {
         throw new java.lang.RuntimeException("The fields: Delegator, Delegate and Manager are required.");
      }

      return result;

   }

   public ArrayList<Map> getContractStorage(String contractAddress) throws Exception
   {
      ArrayList<Map> items = new ArrayList<Map>();

      items = (ArrayList<Map>) rpc.getContractStorage(contractAddress);

      return items;
   }

   
   // FA1.2
   
   @Override
   public JSONObject FA12_transfer(String targetContract, String from, String to, BigInteger value) throws Exception
   {
      JSONObject result = null;
      
      JSONObject jsonObject = callContractEntryPoint(this.getPublicKeyHash(),
            targetContract,
            new BigDecimal("0"),
            new BigDecimal("0.1"),
            "", "",
            "transfer",
            new String[]{ from, to, String.valueOf(value) },
            false,
            Global.FA12_STANDARD);
      
      return jsonObject;
      
   }

   @Override
   public JSONObject FA12_approve(String targetContract, String spender, BigInteger value) throws Exception
   {
      JSONObject result = null;
      
      JSONObject jsonObject = callContractEntryPoint(this.getPublicKeyHash(),
            targetContract,
            new BigDecimal("0"),
            new BigDecimal("0.1"),
            "", "",
            "approve",
            new String[]{ spender, String.valueOf(value) },
            false,
            Global.FA12_STANDARD);
      
      return jsonObject;
   }

   @Override
   public JSONObject FA12_getAllowance(String targetContract, String owner, String spender) throws Exception
   {
         JSONObject jsonObject = callContractEntryPoint(this.getPublicKeyHash(),
               targetContract,
               new BigDecimal("0"),
               new BigDecimal("0.1"),
               "", "",
               "getAllowance",
               new String[]{ owner, spender, Global.NAT_STORAGE_ADDRESS },
               false,
               Global.FA12_STANDARD);
   
      JSONObject result = new JSONObject();
      result.put("result", extractBacktrackedResult(jsonObject.getJSONObject("result")));
   
      return result;
   }

   @Override
   public JSONObject FA12_getBalance(String targetContract, String owner) throws Exception
   {

      JSONObject jsonObject = callContractEntryPoint(this.getPublicKeyHash(),
                                                     targetContract,
                                                     new BigDecimal("0"),
                                                     new BigDecimal("0.1"),
                                                     "", "",
                                                     "getBalance",
                                                     new String[]{ owner, Global.NAT_STORAGE_ADDRESS },
                                                     false,
                                                     Global.FA12_STANDARD);
      
      JSONObject result = new JSONObject();
      result.put("result", extractBacktrackedResult(jsonObject.getJSONObject("result")));
      
      return result;
   }
   
   @Override
   public JSONObject FA12_getTotalSupply(String targetContract) throws Exception
   {

      JSONObject jsonObject = callContractEntryPoint(this.getPublicKeyHash(),
                                                     targetContract,
                                                     new BigDecimal("0"),
                                                     new BigDecimal("0.1"),
                                                     "", "",
                                                     "getTotalSupply",
                                                     new String[]{ Global.NAT_STORAGE_ADDRESS },
                                                     false,
                                                     Global.FA12_STANDARD);
      
      JSONObject result = new JSONObject();
      result.put("result", extractBacktrackedResult(jsonObject.getJSONObject("result")));
      
      return result;
   }

   private String extractBacktrackedResult(JSONObject jsonObject)
   {
      String result = "";
      
      if (jsonObject.has("contents"))
      {
         JSONArray contentsArray = ((JSONArray) jsonObject.get("contents"));
   
         for(int w=0;w<contentsArray.length();w++)
         {
            JSONObject opRes = (JSONObject)((JSONObject)contentsArray.get(w)).get("metadata"); 
            
            if(opRes.has("internal_operation_results"))
            {
               JSONObject backtrack = (JSONObject) ((JSONObject)((JSONObject)((JSONArray)opRes.get("internal_operation_results")).get(0)).get("parameters")).get("value");
               
               if (backtrack.has("int"))
               {
                  result = backtrack.getString("int");
               }
               
               break;
               
            }
   
            if(opRes.has("operation_result"))
            {
               JSONObject opResult = (JSONObject)opRes.get("operation_result");
               
               if (opResult.has("errors"))
               {
                  JSONObject errors = new JSONObject();
                  errors = (JSONObject) ((JSONArray) opResult.get("errors")).get(1);
                  
                  String kind = errors.getString("kind");
                  String id = errors.getString("id");
                  String description = "";
                  
                  if (errors.has("with"))
                  {
                     description = ((JSONObject)errors.get("with")).get("args").toString();
                  }
                  
                  if (errors.has("wrongExpression"))
                  {
                     description = ((JSONObject)errors.get("wrongExpression")).toString();
                  }
                  
                  result = "Error: " + kind + " " + id + " " + description;
                  
               }
               
               if (opResult.has("status"))
               {
                  result = result + " - Status : " + opResult.get("status").toString();
               }
               
               break;
               
            }
         }
         
      }
      else if (jsonObject.has("result"))
      {
         result = jsonObject.get("result").toString();
      }      
      
      return result;
   }

   
}

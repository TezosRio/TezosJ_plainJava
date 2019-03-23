package milfont.com.tezosj.model;

import org.bitcoinj.crypto.MnemonicCode;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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
import milfont.com.tezosj.exceptions.*;

import static milfont.com.tezosj.helper.Constants.TEZOS_SYMBOL;
import static milfont.com.tezosj.helper.Constants.TZJ_KEY_ALIAS;
import static milfont.com.tezosj.helper.Constants.UTEZ;
import static org.apache.commons.lang3.StringUtils.isNumeric;

/**
 * Created by Milfont on 21/07/2018.
 */

public class TezosWallet
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
    private MySodium sodium = null;
    private int myRandomID;
    
    // Constructor with passPhrase.
    // This will create a new wallet and generate new keys and mnemonic words.
    public TezosWallet(String passPhrase) throws Exception
    {
        if (passPhrase != null)
        {
            if (passPhrase.length() > 0)
            {

            	// Creates a unique copy and initializes libsodium native library.
            	Random rand = new Random();
            	int  n = rand.nextInt(1000000) + 1;
            	this.myRandomID = n;
            	this.sodium = new MySodium(String.valueOf(n));
            	
                // Converts passPhrase String to a byte array, respecting char values.
                byte[] c = new byte[passPhrase.length()];
                for (int i = 0; i < passPhrase.length(); i++)
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
        if (mnemonicWords != null)
        {
            if (mnemonicWords.length() > 0)
            {
                if (passPhrase != null)
                {
                    if (passPhrase.length() > 0)
                    {

                    	// Creates a unique copy and initializes libsodium native library.
                    	Random rand = new Random();
                    	int  n = rand.nextInt(1000000) + 1;
                    	this.myRandomID = n;
                    	this.sodium = new MySodium(String.valueOf(n));
                        
                    	// Converts passPhrase String to a byte array, respecting char values.
                        byte[] c = new byte[passPhrase.length()];
                        for (int i = 0; i < passPhrase.length(); i++)
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
                        for (int i = 0; i < cleanMnemonic.length(); i++)
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

    private void initDomainClasses()
    {
        this.rpc = new Rpc();
    }

    // This method generates the Private Key, Public Key and Public Key hash (Tezos address).
    private void generateKeys(String passphrase) throws Exception
    {
        
        // Decrypts the mnemonic words stored in class properties.
        byte[] input = decryptBytes(this.mnemonicWords, getEncryptionKey());

        // Converts mnemonics back into String.
        StringBuilder builder = new StringBuilder();
        for (byte anInput : input)
        {
            builder.append((char) (anInput));
        }

        List<String> items = Arrays.asList((builder.toString()).split(" "));
        byte[] src_seed = MnemonicCode.toSeed(items, passphrase);
        byte[] seed = Arrays.copyOfRange(src_seed, 0, 32);

        byte[] sodiumPrivateKey = zeros(32 * 2);
        byte[] sodiumPublicKey = zeros(32);
        int r = sodium.crypto_sign_seed_keypair(sodiumPublicKey, sodiumPrivateKey, seed);
        
        // These are our prefixes.
        byte[] edpkPrefix = {(byte) 13, (byte) 15, (byte) 37, (byte) 217};
        byte[] edskPrefix = {(byte) 43, (byte) 246, (byte) 78, (byte) 7};
        byte[] tz1Prefix = {(byte) 6, (byte) 161, (byte) 159};

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
        int s = sodium.crypto_generichash(genericHash, genericHash.length, sodiumPublicKey, sodiumPublicKey.length, sodiumPublicKey, 0);

        byte[] prefixedGenericHash = new byte[23];
        System.arraycopy(tz1Prefix, 0, prefixedGenericHash, 0, 3);
        System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

        firstFourOfDoubleChecksum = Sha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
        byte[] prefixedPKhashWithChecksum = new byte[27];
        System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

        // Encrypts and stores Public Key Hash into wallet's class property.
        this.publicKeyHash = encryptBytes(Base58.encode(prefixedPKhashWithChecksum).getBytes(), getEncryptionKey());

    }

    // Generates the mnemonic words.
    private void generateMnemonic() throws Exception
    {
        String result = "";

        MnemonicCode mc = new MnemonicCode();
        byte[] bytes = new byte[20];
        (new java.util.Random()).nextBytes(bytes);
        ArrayList<String> code = (ArrayList<String>) mc.toMnemonic(bytes);
        result = code.toString();

        // Converts the string with the words to a byte array, respecting char values.
        String strMessage = code.toString();

        // Cleans undesired characters from mnemonic words.
        String cleanMnemonic = strMessage.replace("[", "");
        cleanMnemonic = cleanMnemonic.replace("]", "");
        cleanMnemonic = cleanMnemonic.replace(",", " ");
        cleanMnemonic = cleanMnemonic.replace("  ", " ");

        byte[] b = new byte[cleanMnemonic.length()];
        for (int i = 0; i < cleanMnemonic.length(); i++)
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
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            return cipher.doFinal(original);
        }
        catch (Exception e)
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

    // Retrieves mnemonic words upon user request.
    public String getMnemonicWords()
    {
        byte[] decrypted = decryptBytes(this.mnemonicWords, getEncryptionKey());

        return new String(decrypted);
    }

    // Retrieves the Public Key Hash (Tezos user address) upon user request.
    public String getPublicKeyHash()
    {
        if (this.publicKeyHash != null || this.publicKeyHash.length > 0)
        {
            byte[] decrypted = decryptBytes(this.publicKeyHash, getEncryptionKey());

            return new String(decrypted);   
        }
        else
        {
            throw new java.lang.RuntimeException("Error getting public key hash.");
        }
    }

    // Retrieves the account balance.
    public String getBalance() throws Exception
    {
        if (this.publicKeyHash == null || this.publicKeyHash.length == 0)
        {
            throw new RequestValidationException("Wallet is missing the public key hash");
        }

        if (Crypto.checkAddress(this.getPublicKeyHash()))
        {
            throw new RequestValidationException(this.getPublicKeyHash() + " is not a valid public key hash");
        }

        BigDecimal tezBalance = new BigDecimal(String.valueOf(BigDecimal.ZERO));

        byte[] decrypted = decryptBytes(this.publicKeyHash, getEncryptionKey());

        // Get balance from Tezos blockchain.
        String strBalance = (String) rpc.getBalance(new String(decrypted)).getString("result");

        Integer balance = Global.tryParseInt(strBalance);
        if (balance == null)
        {
            throw new java.lang.RuntimeException("Invalid balance from the node: " + strBalance);
        }

        tezBalance = new BigDecimal(balance).divide(BigDecimal.valueOf(UTEZ));
        // Updates wallet balance 
        this.balance = String.valueOf(tezBalance) + " " + TEZOS_SYMBOL;

        return this.balance;    
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
    public JSONObject send(String from, String to, BigDecimal amount, BigDecimal fee, String gasLimit, String storageLimit) throws Exception
    {
        JSONObject result = new JSONObject();

        if ((from != null) && (to != null) && (amount != null))
        {
            if ((Crypto.checkAddress(from) == true) && (Crypto.checkAddress(to) == true))
            {
                if (from.length() > 0)
                {
                    if (to.length() > 0)
                    {
                        if (amount.compareTo(BigDecimal.ZERO) > 0)
                        {
                        	if (fee.compareTo(BigDecimal.ZERO) > 0)
                        	{
                               // Prepare keys
                               EncKeys encKeys = new EncKeys(this.publicKey, this.privateKey, this.publicKeyHash, this.myRandomID);
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
            throw new java.lang.RuntimeException("The fields: From, To and Amount are required."); // RequestValidationException
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

        }
        catch (Exception e)
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

        }
        catch (Exception e)
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
                      
            KeyStore.ProtectionParameter entryPassword = new KeyStore.PasswordProtection(base64EncryptedPassword.toCharArray());
            KeyStore.SecretKeyEntry entry = (SecretKeyEntry) Global.myKeyStore.getEntry(TZJ_KEY_ALIAS + this.myRandomID, entryPassword);            
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, entry.getSecretKey(), new IvParameterSpec(encryptionIv));
            byte[] passwordBytes = cipher.doFinal(encryptionPassword);
            String password = new String(passwordBytes, "UTF-8");

            return passwordBytes;

        }
        catch (Exception e)
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
           
            KeyStore.ProtectionParameter entryPassword = new KeyStore.PasswordProtection(base64EncryptedPassword.toCharArray());
            KeyStore.SecretKeyEntry entry = (SecretKeyEntry) Global.myKeyStore.getEntry(TZJ_KEY_ALIAS + keys.getMyRandomID(), entryPassword);           
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, entry.getSecretKey(), new IvParameterSpec(encryptionIv));
            byte[] passwordBytes = cipher.doFinal(encryptionPassword);
            String password = new String(passwordBytes, "UTF-8");

            return passwordBytes;

        }
        catch (Exception e)
        {
            return null;
        }

    }

    public void save(String pathToFile)
    {

    	if (pathToFile.isEmpty() == false)
    	{
	    	// Persists the wallet to media from memory.
	    	FileOutputStream fop = null;
			File file;
	
			try
			{
			   file = new File(pathToFile);
			   fop = new FileOutputStream(file);
	
				// Checks if file exists, then creates it.
				if (!file.exists())
				{
					file.createNewFile();
				}
	
				// Creates a text version of the wallet.
	            String myWalletData = Base64.getEncoder().encodeToString(this.alias.getBytes()) + ";" +
	                    Base64.getEncoder().encodeToString(this.publicKey) + ";" +
	                    Base64.getEncoder().encodeToString(this.publicKeyHash) + ";" +
	                    Base64.getEncoder().encodeToString(this.privateKey) + ";" +
	                    Base64.getEncoder().encodeToString(this.balance.getBytes()) + ";" +
	                    Base64.getEncoder().encodeToString(this.mnemonicWords) + ";";
	
				byte[] contentInBytes = myWalletData.getBytes();
	
				fop.write(contentInBytes);
				fop.flush();
				fop.close();
				
			}
			catch (IOException e)
			{
				e.printStackTrace();
				throw new java.lang.RuntimeException("Error when trying to save the wallet to media.");
			}
			finally
			{
			   try
			   {
			      if (fop != null)
			      {
				     fop.close();
				  }
				}
			   catch (IOException e)
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

            

    public void load(String pathToFile, String p)
    {
        // Loads a wallet from media to memory.
    	
    	if (pathToFile.isEmpty() == false)
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
				
				if (myWalletString.length() > 0)
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
		            for (int i = 0; i < p.length(); i++)
		            {
		                c[i] = (byte) p.charAt(i);
		            }
		
		            initStore(c);
		            initDomainClasses();
		         }
				
	
			}
			catch (IOException e)
			{
				e.printStackTrace();
	            throw new java.lang.RuntimeException("Error when trying to load wallet from media.");
			}
			finally
			{
				try
				{
					if (fis != null)
					{
						fis.close();
					}
				 }
				catch (IOException ex)
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

    private String buildStringFromByte(byte[] input)
    {
        StringBuilder builder = new StringBuilder();
        for (byte anInput : input)
        {
            builder.append((char) (anInput));
        }
        return builder.toString();
    }

    private byte[] buildByteFromString(String input)
    {
        byte[] d = new byte[input.length()];
        for (int i = 0; i < input.length(); i++)
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
            byte[] edpkPrefix = {(byte) 13, (byte) 15, (byte) 37, (byte) 217};

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
            for (byte anInput : input)
            {
                builder.append((char) (anInput));
            }

            if (publicKey.equals(builder.toString()))
            {
                result = true;
            }
            else
            {
                result = false;
            }
        }
        catch (Exception e)
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

    
}


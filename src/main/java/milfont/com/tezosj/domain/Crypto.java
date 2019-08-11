package milfont.com.tezosj.domain;

import java.util.Random;

import milfont.com.tezosj.helper.Base58;
import milfont.com.tezosj.helper.Base58Check;
import milfont.com.tezosj.helper.MySodium;
import milfont.com.tezosj.helper.Sha256Hash;

public class Crypto
{
   	
    public Boolean checkAddress(String address) throws Exception
    {
        try
        {
            Base58Check base58Check = new Base58Check();

            byte[] result = base58Check.decode(address);
            return true;
        }
        catch (Exception e)
        {
            return  false;
        }

    }
    
    // v1.0.2
    
    // This method was requested by TezosJ_SDK users and allows to derive a publicKey from a privateKey.
    // Nevertheless, it is not recommended to pass private keys as String parameters in production environments,
    // so use it just in development environments or at least convert the String to Byte[] before passing as parameter
    // and convert back to String inside the method.
    public static String getPkFromSk(String s)
    {
       	MySodium sodium = null;
        int myRandomID;

    	// Creates a unique copy and initializes libsodium native library.
    	Random rand = new Random();
    	int  n = rand.nextInt(1000000) + 1;
    	myRandomID = n;
    	sodium = new MySodium(String.valueOf(n));
    	
    	
    	String publicKey="";

        byte[] sodiumPublicKey = zeros(32);
        
        // Converts PrivateKey String to a byte array, respecting char values.
        byte[] sodiumPrivateKey = new byte[s.length()];
        for (int i = 0; i < s.length(); i++)
        {
        	sodiumPrivateKey[i] = (byte) s.charAt(i);
        }
        
        int r = sodium.crypto_sign_keypair(sodiumPublicKey, sodiumPrivateKey);
        
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

        // Get public key as byte array.
        byte[] publicKeyBytes  = Base58.encode(prefixedPubKeyWithChecksum).getBytes();
        
        StringBuilder builder = new StringBuilder();
        for (byte anInput : publicKeyBytes)
        {
            builder.append((char) (anInput));
        }       
        
        publicKey = builder.toString();
               
    	return publicKey;
    }

    public static byte[] zeros(int n)
    {
        return new byte[n];
    }
        
    // v1.0.2
    

}

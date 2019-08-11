import java.math.BigDecimal;
import java.net.URL;

import org.json.JSONObject;

import milfont.com.tezosj.data.ConseilGateway;
import milfont.com.tezosj.domain.Crypto;
import milfont.com.tezosj.helper.Global;
import milfont.com.tezosj.model.TezosWallet;

public class Main
{
   public static void main(String[] args) throws Exception
   {
       // Creates a new wallet with a passphrase.
       TezosWallet wallet = new TezosWallet("myPassphrase");

       // Or... creates (imports) a new wallet with its keys.
       // TezosWallet wallet = new TezosWallet(privateKey, publicKey, publicKeyHash, passPhrase);

       // Or... imports a previously owned wallet with mnemonic words and passphrase.
       // TezosWallet wallet = new TezosWallet("word1, word2, ..., word15", "passphrase");
       
       // Some environment configuration.
       // wallet.setIgnoreInvalidCertificates(false);
       // wallet.setProxy("", "");

       // Shows some wallet data output. 
       System.out.println(wallet.getMnemonicWords());
       System.out.println(wallet.getPublicKeyHash());
       System.out.println(wallet.getBalance());  

       // Saves the current wallet from memory to file.
       wallet.save("c:\\temp\\mySavedWallet.txt");

       System.out.println("Saved the wallet to disk.");

       // Creates a new wallet by reading from file.
       TezosWallet myLoadedWallet = new TezosWallet(true, "c:\\temp\\mySavedWallet.txt", "myPassphrase");

       System.out.println("Loaded the wallet from disk:");
       
       // Shows loaded wallet data. 
       System.out.println(myLoadedWallet.getMnemonicWords());
       System.out.println(myLoadedWallet.getPublicKeyHash());
       System.out.println(myLoadedWallet.getBalance());  
       
       // Example of Sending funds.
       // BigDecimal amount = new BigDecimal("0.123456");
       // BigDecimal fee = new BigDecimal("0.00142");
       // JSONObject jsonObject = wallet.send("tz1FromAddress", "tz1ToAddress", amount, fee, "", "");
       // System.out.println(jsonObject.get("result"));
       
       // Using Conseil Gateway, from Cryptonomic.
       // ConseilGateway cg = new ConseilGateway(new URL("<URL>"), "<APIKEY>", "alphanet");

       // Example of origination operation.   
       // BigDecimal fee = new BigDecimal("0.001300"); // Needed fee for origination.
       // BigDecimal amount = new BigDecimal("2"); // Starting new kt1_delegator address balance.
       // JSONObject jsonObject = wallet.originate(wallet.getPublicKeyHash(), true, true, fee, "", "", amount, "", "");
       // System.out.println(jsonObject.get("result"));

       // Example of delegation operation.
       // BigDecimal fee = new BigDecimal("0.001300");
       // JSONObject jsonObject = wallet.delegate("kt1_delegatorAddress", "tz1_delegate_address", fee, "", "");
       // System.out.println(jsonObject.get("result"));
       
       // Example of undelegation operation.
       // BigDecimal fee = new BigDecimal("0.001300");
       // JSONObject jsonObject = wallet.undelegate("kt1_delegatorAddress", fee);
       // System.out.println(jsonObject.get("result"));       

       // Tools
       
       // Routine to extract the publicKey from a privateKey.
       // String mySecretKey = "edsk...";
       // String publicKey = Crypto.getPkFromSk(mySecretKey);
       // System.out.println(publicKey);

   }
}
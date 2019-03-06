import java.math.BigDecimal;

import org.json.JSONObject;

import milfont.com.tezosj.helper.Global;
import milfont.com.tezosj.model.TezosWallet;

public class Main
{

   public static void main(String[] args) throws Exception
   {
   	   
	   // Creates a new wallet with a passphrase.
	   TezosWallet wallet = new TezosWallet("myPassphrase");

	   wallet.setIgnoreInvalidCertificates(false);
	   wallet.setProxy("", "");

	   
	   // Shows some wallet data output. 
	   System.out.println(wallet.getMnemonicWords());
	   System.out.println(wallet.getPublicKeyHash());
	   System.out.println(wallet.getBalance());  

	   // Imports a previously owned wallet with mnemonic words and passphrase.
	   // TezosWallet wallet2 = new TezosWallet("word1 word2 ... word15", "myPassphrase");

	   // Shows some wallet data output. 
	   // System.out.println(wallet2.getMnemonicWords());
	   // System.out.println(wallet2.getPublicKeyHash());
	   // System.out.println(wallet2.getBalance());  

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
	   // BigDecimal amount = new BigDecimal("1");
	   // BigDecimal fee = new BigDecimal("0.05");
	   // JSONObject jsonObject = wallet2.send("tz1FromAddress", "tz1ToAddress", amount, fee, "", "");
	   // System.out.println(jsonObject.get("result"));
	   
   }

}


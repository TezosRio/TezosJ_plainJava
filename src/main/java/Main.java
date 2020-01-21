import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;


import milfont.com.tezosj.data.ConseilGateway;
import milfont.com.tezosj.domain.Crypto;
import milfont.com.tezosj.helper.Global;
import milfont.com.tezosj.model.TezosWallet;
import milfont.com.tezosj.model.BatchTransactionItem;

public class Main
{
   public static void main(String[] args) throws Exception
   {
       // Creates a new wallet with a passphrase.
       TezosWallet wallet = new TezosWallet("myPassphrase");

       // Or... creates (imports) a new wallet with its keys.
       // TezosWallet wallet = new TezosWallet(privateKey, publicKey, publicKeyHash, myPassphrase);

       // Or... creates (imports) a new wallet with its encrypted private key (which has 'edesk' prefix) and the corresponding private key password.
       // TezosWallet wallet = new TezosWallet(encryptedPrivateKey, privateKeyPassword, publicKey, publicKeyHash, myPassphrase);

       // Or... imports a previously owned wallet with mnemonic words and passphrase.
       // TezosWallet wallet = new TezosWallet("word1, word2, word3, ... word15 ", "myPassphrase");
       
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
       // BigDecimal fee = new BigDecimal("0.00294");
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

       
       // Batch transactions.
       
       // Example of sending batch transactions.
       
       // Clears the transactions batch.
       // wallet.clearTransactionBatch(); 
       
       // Adds a first transaction to the batch.
       // wallet.addTransactionToBatch("from_address", "to_address", new BigDecimal("1"), new BigDecimal("0.00294"));

       // Adds a second transaction to the batch.
       // wallet.addTransactionToBatch("from_address", "to_address", new BigDecimal("2"), new BigDecimal("0.00294"));
       
       // Adds a third transaction to the batch.
       // wallet.addTransactionToBatch("from_address", "to_address", new BigDecimal("3"), new BigDecimal("0.00294"));

       // Note that "from_address" above maybe the manager address or its originated kt1 addresses.
       
       // Gets a list of wallet's current (pending) batch transactions.
       // ArrayList<BatchTransactionItem> myBatchTransactionsList = new ArrayList<BatchTransactionItem>();
       // myBatchTransactionsList = wallet.getTransactionList();

       // Sends all transactions in the batch to the blockchain and clears the batch.
       // JSONObject jsonObject = wallet.flushTransactionBatch();
       // Or... Specifying gasLimit and storageLimit:
       // JSONObject jsonObject = wallet.flushTransactionBatch("15400","300");       
       // System.out.println("Batch transaction sent! Returned operation hash is: ");
       // System.out.println(jsonObject.get("result"));

       
       // Synchronously waits for previous operation to be included in a block after sending another one.
       // (this is to be used if you need to send a sequence of single transactions, having to wait first for each one to be included).
       
       // BigDecimal amount = new BigDecimal("0.02");
       // BigDecimal fee = new BigDecimal("0.00294");
       // JSONObject jsonObject = wallet.send("tz1FromAddress", "tz1ToAddress", amount, fee, "", "");
	    // String opHash = (String) jsonObject.get("result");
	    // Boolean opHashIncluded = wallet.waitForResult(opHash, numberOfBlocksToWait);
       // System.out.println(opHashIncluded);
       // Now it is safe to send another transaction at this point.

       
       // Smart Contract calls.

       // Call a smart contract in testnet.
       // Basically you need to provide the contract KT address, the name of the entrypoint you are calling and a "new String[]" array with the parameters.
       // IMPORTANT: Before calling the contract, check the name of the called entrypoint and the order of your parameters.
       // You don't need to create the Micheline parameters. TezosJ will create them for you on-the-fly.
       // See an example:
       //    JSONObject jsonObject = wallet.callContractEntryPoint("TZ1_FromAddress",
       //                                                          "KT1_SmartContractAddress",
       //                                                          amount,
       //                                                          fee, 
       //                                                          gasLimit,
       //                                                          storageLimit,
       //                                                          entryPoint,
       //                                                          new String[]{"param_1", "param_2", "...", "param_n"});
       //
       //
       
       // Now a functional example (remember that your wallet must be funded and revealed for this to work).

       // Change wallet provider to use testnet.
       wallet.setProvider("https://tezos-dev.cryptonomic-infra.tech");       
         
       // Sets amount and fee for the transaction.
       BigDecimal amount = new BigDecimal("0");
       BigDecimal fee = new BigDecimal("0.1");
       
       System.out.println("Calling the contract (inserting customer 1, please wait a minute)...");
       
       // Calls the contract.
       JSONObject jsonObject = wallet.callContractEntryPoint(wallet.getPublicKeyHash(), "KT18pK2MGrnTZqyTafUe1sWp2ubJ75eYT86t", amount, fee, "", "", "addCustomer", new String[]{"1000000", "123456789","Bob","98769985"});

       // Waits for the transaction to be included, so that we can call the contract once more.
       String opHash = (String) jsonObject.get("result");
       Boolean opHashIncluded = wallet.waitForResult(opHash, 5);
       System.out.println(opHashIncluded + " " + opHash);

       System.out.println("Calling the contract (insert customer 2, please wait a minute)...");

       // Calls the contract again.
       jsonObject = wallet.callContractEntryPoint(wallet.getPublicKeyHash(), "KT18pK2MGrnTZqyTafUe1sWp2ubJ75eYT86t", amount, fee, "", "", "addCustomer", new String[]{"2000000", "987654321","Alice","97788657"});

       // Waits for the transaction to be included, so that we may call the contract once more.
       opHash = (String) jsonObject.get("result");
       opHashIncluded = wallet.waitForResult(opHash, 5);
       System.out.println(opHashIncluded + " " + opHash);
	   
   }
}

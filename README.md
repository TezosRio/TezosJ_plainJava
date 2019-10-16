# TezosJ_plainjava
A Java SDK for Tezos node interactions with [Conseil](https://cryptonomic.github.io/Conseil/#/) support.

The TezosJ SDK library enables plain Java developers to create applications that communicates with Tezos blockchain.

The library is written in Java and is based on Gradle framework. This repository contains the library source code and a Main class to test some features.

## Requirements

- Java 8
- Windows / Linux (not tested yet) / Mac
- Eclipse or another Java IDE.

## Getting started

- Clone the repository, import as a Gradle Project into your Java IDE and run the Main class.
- Or download the JAR (https://github.com/tezosRio/TezosJ_plainJava/blob/master/tezosj-sdk-plain-java-1.0.8.jar) and add to your project's classpath.
- Or (soon)... Download the JAR file from JCENTER (bintray.com/milfont/tezos/tezosj_plainjava/1.0.8/tezosj-sdk-plain-java-1.0.8.jar) and put in your project's classpath.
- Or (soon)... Add to your build.gradle dependencies: compile 'com.milfont.tezos:tezosj_plainjava:1.0.8'  

## Usage

```java

       // Creates a new wallet with a passphrase.
       TezosWallet wallet = new TezosWallet("myPassphrase");

       // Or... creates (imports) a new wallet with its keys.
       // TezosWallet wallet = new TezosWallet(privateKey, publicKey, publicKeyHash, myPassPhrase);

       // Or... imports a previously owned wallet with mnemonic words and passphrase.
       // TezosWallet wallet = new TezosWallet("word1, word2, word3, ... word15 ", "myPassPhrase");
       
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

       
       // Batch transactions.
       
       // Example of sending batch transactions.
       
       // Clears the transactions batch.
       // wallet.clearTransactionBatch(); 
       
       // Adds a first transaction to the batch.
       // wallet.addTransactionToBatch("from_address", "to_address", new BigDecimal("1"), new BigDecimal("0.00142"));

       // Adds a second transaction to the batch.
       // wallet.addTransactionToBatch("from_address", "to_address", new BigDecimal("2"), new BigDecimal("0.00142"));
       
       // Adds a third transaction to the batch.
       // wallet.addTransactionToBatch("from_address", "to_address", new BigDecimal("3"), new BigDecimal("0.00142"));

       // Note that "from_address" above maybe the manager address or its originated kt1 addresses.
       
       // Gets a list of wallet's current (pending) batch transactions.
       // ArrayList<BatchTransactionItem> myBatchTransactionsList = new ArrayList<BatchTransactionItem>();
       // myBatchTransactionsList = wallet.getTransactionList();

       // Sends all transactions in the batch to the blockchain and clears the batch.
       // JSONObject jsonObject = wallet.flushTransactionBatch();
       // Or... Specifying gasLimit and storageLimit:
       // JSONObject jsonObject = wallet.flushTransactionBatch("11000","300");       
       // System.out.println("Batch transaction sent! Returned operation hash is: ");
       // System.out.println(jsonObject.get("result"));

       
       // Synchronously waits for previous operation to be included in a block after sending another one.
       // (this is to be used if you need to send a sequence of single transactions, having to wait first for each one to be included).
       
       // BigDecimal amount = new BigDecimal("0.02");
       // BigDecimal fee = new BigDecimal("0.00142");
       // JSONObject jsonObject = wallet.send("tz1FromAddress", "tz1ToAddress", amount, fee, "", "");
	   // String opHash = (String) jsonObject.get("result");
	   // Boolean opHashIncluded = wallet.waitForResult(opHash, numberOfBlocksToWait);
	   // System.out.println(opHashIncluded);
	   // Now it is safe to send another transaction at this point.
 

```

## Disclaimer

This software is at Beta stage. It is currently experimental and still under development. Many features are not fully tested/implemented yet. This version uses Tezos Mainnet (!)

## Features

- Create valid Tezos wallet address
- Import Tezos wallet
- Check if an address is valid
- Get account balance
- Send funds
- Retrieve account information and transactions via Conseil.
- Originate a KT address.
- Delegate to a known baker.
- Undelegate from a known baker.
- Retrieve a publicKey from a known privateKey.
- Batch transactions.
- Synchronously check (wait until) an operation hash has been included in a block.
- Now allows (1.0.8) specifying gasLimit and storageLimit to batch transactions through flushTransactions method.

The main purpose of TezosJ SDK library is to foster development of applications in plain Java that interacts with Tezos ecosystem. This might open Tezos to a whole world of software producers, ready to collaborate with the platform. TezosJ is to play the role of a layer that will translate default Java method calls to Tezos network real operations (create_account, transfer_token, etc.)

## Credits

- TezosJ is based on Stephen Andrews' EZTZ Javascript library https://github.com/stephenandrews/eztz.
- TezosJ is also based on ConseilJS from Cryptonomic https://github.com/Cryptonomic/ConseilJS
- TezosJ uses LazySodium https://github.com/terl/lazysodium-java
- TezosJ uses BitcoinJ Java Library https://github.com/bitcoinj/bitcoinj.
- Special thanks to Tezzigator (https://twitter.com/@tezzigator) for providing the code for Tezos Key Generation in Java.
- Special thanks to Klassare who helped us to implement Batch Transactions.

## License

The TezosJ SDK library is available under the MIT License. Check out the license file for more information.

## See also

TezosJ SDK for Android.

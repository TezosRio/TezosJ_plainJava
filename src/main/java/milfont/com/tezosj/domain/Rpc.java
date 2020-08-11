package milfont.com.tezosj.domain;

import org.json.JSONObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

import milfont.com.tezosj.data.TezosGateway;
import milfont.com.tezosj.model.BatchTransactionItem;
import milfont.com.tezosj.model.EncKeys;

/**
 * 
 */
public class Rpc
{
   private TezosGateway tezosGateway = null;

   public Rpc()
   {
      this.tezosGateway = new TezosGateway();
   }

   public TezosGateway getTezosGateway()
   {
      return tezosGateway;
   }

   public String getHead()
   {
      String response = "";

      try
      {
         response = (String) tezosGateway.getHead().get("result");
      } catch (Exception e)
      {
         e.printStackTrace();
      }

      return response;
   }

   public JSONObject getBalance(String address)
   {
      JSONObject result = new JSONObject();

      try
      {
         String response = (String) tezosGateway.getBalance(address).get("result");
         result.put("result", response);
      } catch (Exception e)
      {
         e.printStackTrace();
         try
         {
            result.put("result", e.toString());
         } catch (Exception f)
         {
            f.printStackTrace();
         }
      }

      return result;
   }

   public JSONObject transfer(String from, String to, BigDecimal amount, BigDecimal fee, String gasLimit,
                              String storageLimit, EncKeys encKeys)
   {
      JSONObject result = new JSONObject();

      try
      {
         result = (JSONObject) tezosGateway.sendTransaction(from, to, amount, fee, gasLimit, storageLimit, encKeys);
      } catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to perform a transfer operation. See stacktrace for more info.");
      }

      return result;

   }

   public JSONObject reveal(String publicKeyHash, String publicKey, EncKeys encKeys)
   {
      JSONObject result = new JSONObject();

      try
      {
         result = (JSONObject) tezosGateway.reveal(publicKeyHash, publicKey, encKeys);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to perform a reveal operation. See stacktrace for more info.");
      }

      return result;

   }

   public JSONObject activate(String addressToActivate, String secret, EncKeys encKeys)
   {
      JSONObject result = new JSONObject();

      try
      {
         result = (JSONObject) tezosGateway.activate(addressToActivate, secret, encKeys);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to perform an activate operation. See stacktrace for more info.");
      }

      return result;

   }

   public JSONObject delegate(String delegateFrom, String delegateTo, BigDecimal fee, String gasLimit,
                              String storageLimit, EncKeys encKeys)
   {
      JSONObject result = new JSONObject();

      try
      {
         result = (JSONObject) tezosGateway.sendDelegationOperation(delegateFrom, delegateTo, fee, gasLimit,
               storageLimit, encKeys);
      } catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to do perform a delegation operation. See stacktrace for more info.");
      }

      return result;

   }

   public JSONObject originate(String from, Boolean spendable, Boolean delegatable, BigDecimal fee, String gasLimit,
                               String storageLimit, BigDecimal amount, String code, String storage, EncKeys encKeys)
   {
      JSONObject result = new JSONObject();

      try
      {
         result = (JSONObject) tezosGateway.sendOriginationOperation(from, spendable, delegatable, fee, gasLimit,
               storageLimit, amount, code, storage, encKeys);
      } catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to do perform an origination operation. See stacktrace for more info.");
      }

      return result;

   }

   public JSONObject undelegate(String delegateFrom, BigDecimal fee, EncKeys encKeys)
   {
      JSONObject result = new JSONObject();

      try
      {
         result = (JSONObject) tezosGateway.sendUndelegationOperation(delegateFrom, fee, encKeys);
      } catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to do perform an undelegation operation. See stacktrace for more info.");
      }

      return result;

   }

   public JSONObject sendBatchTransactions(ArrayList<BatchTransactionItem> transactions, EncKeys encKeys,
                                           String gasLimit, String storageLimit)
   {
      JSONObject result = new JSONObject();

      try
      {
         result = (JSONObject) tezosGateway.sendBatchTransactions(transactions, encKeys, gasLimit, storageLimit);
      } catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to send a batch transactions operation. See stacktrace for more info.");
      }

      return result;

   }

   public Boolean waitForResult(String operationHash, Integer numberOfBlocksToWait)
   {
      Boolean result = false;

      try
      {
         result = (Boolean) tezosGateway.waitForResult(operationHash, numberOfBlocksToWait);
      } catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to get operation results. See stacktrace for more info.");
      }

      return result;

   }

   public JSONObject callContractEntryPoint(String from, String contract, BigDecimal amount, BigDecimal fee,
                                            String gasLimit, String storageLimit, EncKeys encKeys, String entrypoint,
                                            String[] parameters, Boolean rawParameter, String smartContractType)
   {
      JSONObject result = new JSONObject();

      try
      {
         result = (JSONObject) tezosGateway.callContractEntryPoint(from, contract, amount, fee, gasLimit, storageLimit,
               encKeys, entrypoint, parameters, rawParameter, smartContractType);
      } catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to call to a contract. See stacktrace for more info.");
      }

      return result;

   }

   public ArrayList<Map> getContractStorage(String contractAddress) throws Exception
   {
      ArrayList<Map> items = new ArrayList<Map>();

      items = (ArrayList<Map>) tezosGateway.getContractStorage(contractAddress);

      return items;
   }

   public Boolean waitForAndCheckResult(String operationHash, Integer numberOfBlocksToWait)
   {
      Boolean result = false;

      try
      {
         result = (Boolean) tezosGateway.waitForAndCheckResult(operationHash, numberOfBlocksToWait);
      } catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to check operation results. See stacktrace for more info.");
      }

      return result;

   }

   public JSONObject waitForAndCheckResultByDestinationAddress(String address, Integer numberOfBlocksToWait)
   {
      JSONObject result = null;

      try
      {
         result = tezosGateway.waitForAndCheckResultByDestinationAddress(address, numberOfBlocksToWait);
      } 
      catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to check operation results. See stacktrace for more info.");
      }

      return result;

   }
   
   
   public JSONObject transferImplicit(String contract, String implicitAddress, String managerAddress, BigDecimal amount, EncKeys encKeys)
   {
      JSONObject result = new JSONObject();

      try
      {
         result = (JSONObject) tezosGateway.transferImplicit(contract, implicitAddress, managerAddress, amount, encKeys);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to perform a transfer implicit operation. See stacktrace for more info.");
      }

      return result;

   }

   public JSONObject transferToContract(String contract, String destinationKT, String managerAddress, BigDecimal amount, EncKeys encKeys)
   {
      JSONObject result = new JSONObject();

      try
      {
         result = (JSONObject) tezosGateway.transferToContract(contract, destinationKT, managerAddress, amount, encKeys);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to perform a transfer to contract operation. See stacktrace for more info.");
      }

      return result;

   }

   public JSONObject sendDelegationFromContract(String delegator, String delegate, String managerAddress, EncKeys encKeys) throws Exception
   {
      JSONObject result = new JSONObject();

      try
      {
         result = (JSONObject) tezosGateway.sendDelegationFromContract(delegator, delegate, managerAddress, encKeys);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         throw new java.lang.RuntimeException(
               "An error occured while trying to perform a delegation operation. See stacktrace for more info.");
      }

      return result;

   }
  
   
   
}
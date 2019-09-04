package milfont.com.tezosj.domain;

import org.json.JSONObject;
import java.math.BigDecimal;
import java.util.ArrayList;

import milfont.com.tezosj.data.TezosGateway;
import milfont.com.tezosj.model.BatchTransactionItem;
import milfont.com.tezosj.model.EncKeys;

/**
 * 
 */
public class Rpc {
    private TezosGateway tezosGateway = null;

    public Rpc() {
        this.tezosGateway = new TezosGateway();
    }

    public TezosGateway getTezosGateway() {
		return tezosGateway;
	}

	public String getHead() {
        String response = "";

        try {
            response = (String) tezosGateway.getHead().get("result");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public JSONObject getBalance(String address) {
        JSONObject result = new JSONObject();

        try {
            String response = (String) tezosGateway.getBalance(address).get("result");
            result.put("result", response);
        } catch (Exception e) {
            e.printStackTrace();
            try
            {
                result.put("result", e.toString());
            }
            catch (Exception f)
            {
                f.printStackTrace();
            }
        }

        return result;
    }

    public JSONObject transfer(String from, String to, BigDecimal amount, BigDecimal fee, String gasLimit, String storageLimit, EncKeys encKeys)
    {
        JSONObject result = new JSONObject();

        try
        {
            result = (JSONObject) tezosGateway.sendTransaction(from, to, amount, fee, gasLimit, storageLimit, encKeys);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new java.lang.RuntimeException("An error occured while trying to perform a transfer operation. See stacktrace for more info.");
        }

        return result;

    }

    public JSONObject delegate(String delegateFrom, String delegateTo, BigDecimal fee, String gasLimit, String storageLimit, EncKeys encKeys)
    {
        JSONObject result = new JSONObject();

        try
        {
            result = (JSONObject) tezosGateway.sendDelegationOperation(delegateFrom, delegateTo, fee, gasLimit, storageLimit, encKeys);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new java.lang.RuntimeException("An error occured while trying to do perform a delegation operation. See stacktrace for more info.");
        }

        return result;

    }

    public JSONObject originate(String from, Boolean spendable, Boolean delegatable, BigDecimal fee, String gasLimit, String storageLimit, BigDecimal amount, String code, String storage, EncKeys encKeys)
    {
        JSONObject result = new JSONObject();

        try
        {
            result = (JSONObject) tezosGateway.sendOriginationOperation(from, spendable, delegatable, fee, gasLimit, storageLimit, amount, code, storage, encKeys);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new java.lang.RuntimeException("An error occured while trying to do perform an origination operation. See stacktrace for more info.");
        }

        return result;

    }

    public JSONObject undelegate(String delegateFrom, BigDecimal fee, EncKeys encKeys)
    {
        JSONObject result = new JSONObject();

        try
        {
            result = (JSONObject) tezosGateway.sendUndelegationOperation(delegateFrom, fee, encKeys);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new java.lang.RuntimeException("An error occured while trying to do perform an undelegation operation. See stacktrace for more info.");
        }

        return result;

    }

 
    public JSONObject sendBatchTransactions(ArrayList<BatchTransactionItem> transactions, EncKeys encKeys)
    {
        JSONObject result = new JSONObject();

        try
        {
            result = (JSONObject) tezosGateway.sendBatchTransactions(transactions, encKeys);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new java.lang.RuntimeException("An error occured while trying to send a batch transactions operation. See stacktrace for more info.");
        }

        return result;

    }

    
    public Boolean waitForResult(String operationHash, Integer numberOfBlocksToWait)
    {
        Boolean result = false;

        try
        {
            result = (Boolean) tezosGateway.waitForResult(operationHash, numberOfBlocksToWait);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new java.lang.RuntimeException("An error occured while trying to get operation results. See stacktrace for more info.");
        }

        return result;

    }

    
}
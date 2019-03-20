package milfont.com.tezosj.domain;

import org.json.JSONObject;
import java.math.BigDecimal;

import milfont.com.tezosj.data.TezosGateway;
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
            throw new java.lang.RuntimeException("An error occured while trying to do perform an operation. See stacktrace for more info.");
        }

        return result;

    }
}
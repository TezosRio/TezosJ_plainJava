
package milfont.com.tezosj.data;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import milfont.com.tezosj.model.Transaction;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConseilGateway {
    private String _apiKey;
    private URL _url;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 
     * @param url Conseil server url
     * @param apiKey Conseil API key
     * @param network Tezos network: "mainnet", "alphanet", "zeronet"
     * @throws MalformedURLException
     */
    public ConseilGateway(URL url, String apiKey, String network) throws MalformedURLException {
        _url = new URL(url, String.format("/v2/data/tezos/%s", network));
        _apiKey = apiKey;
    }

    public List<Transaction> getTransactions(String address) throws Exception {
        URL requestURL = new URL(_url, String.format("%s%s", _url.getPath(), "/operations"));

        RequestBody spendQuery = RequestBody.create(JSON, String.format("{\"fields\": [\"timestamp\", \"source\", \"destination\", \"amount\", \"fee\"],\"predicates\": [{\"field\": \"kind\", \"set\": [\"transaction\"], \"operation\": \"eq\"}, {\"field\": \"source\", \"set\": [\"%s\"], \"operation\": \"eq\"}],\"orderBy\": [{\"field\": \"timestamp\", \"direction\": \"desc\"}],\"limit\": 100}", address));
        Request spendRequest = new Request.Builder().url(requestURL).addHeader("Content-Type", "application/json").addHeader("apiKey", _apiKey).post(spendQuery).build();

        RequestBody receiveQuery = RequestBody.create(JSON, String.format("{\"fields\": [\"timestamp\", \"source\", \"destination\", \"amount\", \"fee\"],\"predicates\": [{\"field\": \"kind\", \"set\": [\"transaction\"], \"operation\": \"eq\"}, {\"field\": \"destination\", \"set\": [\"%s\"], \"operation\": \"eq\"}],\"orderBy\": [{\"field\": \"timestamp\", \"direction\": \"desc\"}],\"limit\": 100}", address));
        Request receiveRequest = new Request.Builder().url(requestURL).addHeader("Content-Type", "application/json").addHeader("apiKey", _apiKey).post(receiveQuery).build();

        List<Transaction> results = new ArrayList<Transaction>();
        try {
            Response spendResponse = (new OkHttpClient()).newCall(spendRequest).execute();
            JSONArray s = new JSONArray(spendResponse.body().string());
            for (Object o : s) {
                results.add(new Transaction((JSONObject)o));
            }

            Response receiveResponse = (new OkHttpClient()).newCall(receiveRequest).execute();
            JSONArray r = new JSONArray(receiveResponse.body().string());
            for (Object o : r) {
                results.add(new Transaction((JSONObject)o));
            }
        } catch (IOException e) {
            // TODO: uniform exception processing
        } catch (JSONException e) {
            // TODO: uniform exception processing
        } catch (NumberFormatException e) {
            // TODO: uniform exception processing
        }

        results.sort(Comparator.comparingLong(Transaction::getEpoch).reversed());
        return results;
    }
}
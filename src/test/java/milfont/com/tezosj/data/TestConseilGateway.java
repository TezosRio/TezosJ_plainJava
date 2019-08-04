package milfont.com.tezosj.data;

import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import milfont.com.tezosj.model.Transaction;

import java.net.URL;
import java.util.List;

public class TestConseilGateway {
    public static final String url = "https://conseil-dev.cryptonomic-infra.tech:443";
    public static final String API_KEY = "BUIDLonTezos-029";
    public static final String NETWORK = "alphanet";

    @Test
    public void testGetTransactions() throws Exception {
        ConseilGateway gateway = new ConseilGateway(new URL(url), API_KEY, NETWORK);
        List<Transaction> transactions = gateway.getTransactions("tz1PDwT7dj2xQitXE1nrQXVzRXsnxkWeUUEk");
        assertThat(transactions.size(), greaterThan(0));
    }
}

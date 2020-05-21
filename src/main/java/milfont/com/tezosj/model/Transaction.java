package milfont.com.tezosj.model;

import java.util.Date;

import org.json.JSONObject;

public class Transaction {
    private Date _timestamp;
    private String _from;
    private String _to;
    private long _amount;
    private long _fee;
    private long _blockLevel;

    public Transaction(Date timestamp, String from, String to, long amount, long fee) {
        _timestamp = timestamp;
        _from = from;
        _to = to;
        _amount = amount;
        _fee = fee;
    }

    public Transaction(Date timestamp, String from, String to, long amount, long fee, long blockLevel) {
        this(timestamp, from, to, amount, fee);
        _blockLevel = blockLevel;
    }

    /**
     * Parses a Transaction from a JSON object containing standard Tezos RPC keys.
     * 
     * @param json A JSON object containing "timestamp", "source", "destination", "amount", and "fee" keys
     */
    public Transaction(JSONObject json) {
        this(new Date(((Number)json.get("timestamp")).longValue()), (String)json.get("source"), (String)json.get("destination"), ((Number)json.get("amount")).longValue(), ((Number)json.get("fee")).longValue());
    }

    public Date getTimestamp() {
        return _timestamp;
    }

    public long getEpoch() {
        return _timestamp.getTime();
    }

    public String getFrom() {
        return _from;
    }

    public String getTo() {
        return _to;
    }

    public long getAmount() {
        return _amount;
    }

    public long getFee() {
        return _fee;
    }
}

package milfont.com.tezosj.model;

import org.json.JSONObject;

public class Account {
    private long _blockLevel;
    private String _address;
    private String _manager;
    private String _delegate;
    private long _balance;
    private boolean _delegatable;

    /**
     * 
     * 
     * @param address
     * @param manager
     * @param blockLevel
     * @param balance
     */
    public Account(String address, String manager, String delegate, long blockLevel, long balance, boolean delegatable) {
        this._blockLevel = blockLevel;
        this._address = address;
        this._manager = manager;
        this._delegate = delegate;
        this._balance = balance;
        this._delegatable = delegatable;
    }

    public Account(JSONObject json) {
        this((String)json.get("account_id"), (String)json.get("manager"), (json.get("delegate_value") != JSONObject.NULL ? (String)json.get("delegate_value") : null), ((Number)json.get("block_level")).longValue(), ((Number)json.get("balance")).longValue(), ((Boolean)json.get("delegate_setable")).booleanValue());
    }

    public String getAddress() {
        return this._address;
    }

    public String getManager() {
        return this._manager;
    }

    public String getDelegate() {
        return this._delegate;
    }

    public boolean isDelegatable() {
        return _delegatable;
    }

    public long getBalance() {
        return this._balance;
    }

    public long getLastActiveBlock() {
        return this._blockLevel;
    }
}

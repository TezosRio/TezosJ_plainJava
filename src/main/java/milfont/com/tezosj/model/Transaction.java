package milfont.com.tezosj.model;

public class Transaction
{
    private String timestamp = "";
    private String from = "";
    private String to = "";
    private String amount = "";

    public Transaction(String timestamp, String from, String to, String amount)
    {
        this.timestamp = timestamp;
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(String timestamp)
    {
        this.timestamp = timestamp;
    }

    public String getFrom()
    {
        return from;
    }

    public void setFrom(String from)
    {
        this.from = from;
    }

    public String getTo()
    {
        return to;
    }

    public void setTo(String to)
    {
        this.to = to;
    }

    public String getAmount()
    {
        return amount;
    }

    public void setAmount(String amount)
    {
        this.amount = amount;
    }
}

package milfont.com.tezosj.model;

import java.math.BigDecimal;
import java.util.Date;

import org.json.JSONObject;

public class BatchTransactionItem implements Comparable<BatchTransactionItem>
{
    private String _from;
    private String _to;
    private BigDecimal _amount;
    private BigDecimal _fee;
    private Integer _count;
    private Integer _index;

    public BatchTransactionItem(String from, String to, BigDecimal amount, BigDecimal fee, Integer index)
    {
        _from = from;
        _to = to;
        _amount = amount;
        _fee = fee;
        _count = new Integer("0");
        _index = index; 
    }

	public String getFrom()
	{
		return _from;
	}

	public void setFrom(String _from)
	{
		this._from = _from;
	}

	public String getTo()
	{
		return _to;
	}

	public void setTo(String _to)
	{
		this._to = _to;
	}

	public BigDecimal getAmount()
	{
		return _amount;
	}

	public void setAmount(BigDecimal _amount)
	{
		this._amount = _amount;
	}

	public BigDecimal getFee()
	{
		return _fee;
	}

	public void setFee(BigDecimal _fee)
	{
		this._fee = _fee;
	}

	public Integer getCount()
	{
		return _count;
	}

	public void setCount(Integer _count)
	{
		this._count = _count;
	}

	public Integer getIndex()
	{
		return _index;
	}

	public void setIndex(Integer _index)
	{
		this._index = _index;
	}

	@Override
	public int compareTo(BatchTransactionItem item)
	{
		return this.getFrom().compareTo(item.getFrom()); 		
	}	

}

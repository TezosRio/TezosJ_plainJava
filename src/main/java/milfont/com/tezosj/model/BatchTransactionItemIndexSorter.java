package milfont.com.tezosj.model;

import java.util.Comparator;

public class BatchTransactionItemIndexSorter implements Comparator<BatchTransactionItem>
{

	@Override
	public int compare(BatchTransactionItem item1, BatchTransactionItem item2)
	{
		return item1.getIndex().compareTo(item2.getIndex());
	}

}

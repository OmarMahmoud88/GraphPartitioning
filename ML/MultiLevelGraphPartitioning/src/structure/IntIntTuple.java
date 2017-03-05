package structure;

public class IntIntTuple {
	
	private int val1;
	private int val2;
	public IntIntTuple(int val1, int val2) {
		// TODO Auto-generated constructor stub
		this.val1 = val1;
		this.val2 = val2;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + val1;
		result = prime * result + val2;
		return result;
	}

	@Override
	public boolean equals(Object tuple) {
		if (this == tuple)
			return true;
		if (tuple == null)
			return false;
		if (getClass() != tuple.getClass())
			return false;
		
		if(this.val1 == ((IntIntTuple)tuple).val1 && this.val2 == ((IntIntTuple)tuple).val2)
			return true;
		return false;
	}

	public int first() {
		return this.val1;
	}

	public int second() {
		return this.val2;
	}
}

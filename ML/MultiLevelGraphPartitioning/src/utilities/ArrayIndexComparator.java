package utilities;

import java.util.Comparator;

public class ArrayIndexComparator implements Comparator<Integer>
{
    private final double[] array;

    public ArrayIndexComparator(double[] array)
    {
        this.array = array;
    }

    public Integer[] createIndexArray()
    {
        Integer[] indexes = new Integer[array.length];
        for (int i = 0; i < array.length; i++)
        {
            indexes[i] = i; // Autoboxing
        }
        return indexes;
    }

    @Override
    public int compare(Integer index1, Integer index2)
    {
         // Autounbox from Integer to int to use as array indexes
    	double diff = array[index1]-array[index2];
    	if(diff > 0.0000001){
    		return 1;
    	}
    	else if(diff< -0.0000001){
    		return -1;
    	}
    	
    	return 0;
    }
}
package com.ibm.airlock.engine;

import java.util.Random;

import org.apache.commons.codec.binary.Base64;

// maintain a random set of bits representing a percentage of users that are allowed to see a feature
public class Percentile
{
	public static class PercentileException extends Exception
	{
		private static final long serialVersionUID = 1L;
		PercentileException(String err) {
			super(err);
		}
	}

	static final int maxNum = 100;
	static final int byteSZ = Byte.SIZE;
	@SuppressWarnings("unused")
	static final int maxBytes = (maxNum / byteSZ) + ((maxNum % byteSZ > 0) ? 1 : 0);

	int percentage;
	byte[] array;

	public Percentile(String b64) throws PercentileException
	{
		array = Base64.decodeBase64(b64);
		if (array.length != maxBytes)
			throw new PercentileException("Invalid percentile string " + b64);

		percentage = countOn();
	}
	int countOn()
	{
		int count = 0;
		for (int i = 0; i < maxNum; ++i)
		{
			if (isOn(i))
				++count;
		}
		return count;
	}

	public String toString()
	{
		return Base64.encodeBase64String(array);
	}

	public Percentile(int percentage) throws PercentileException
	{
		if (percentage < 0 || percentage > maxNum)
			throw new PercentileException("Invalid percentage " + percentage);

		this.percentage = percentage;

		array = new byte[maxBytes]; // initialized to 0 by default

		// randomize numbers from 0 to 99 inclusive
	    int[] numbers = new int[maxNum];
	    for (int i = 0; i < maxNum; ++i)
	    {
	    	numbers[i] = i;
	    }
	    shuffle(numbers, numbers.length);

	    // select a subset whose size is the percentage
	    for (int i = 0; i < percentage; ++i)
	    {
	    	setOn(numbers[i]);
	    }
	}

	// create a percentile for a sub-feature based on the percentile of its parent feature
	public Percentile(Percentile parent, int percentOfParent) throws PercentileException
	{
		if (percentOfParent < 0 || percentOfParent > maxNum)
			throw new PercentileException("Invalid percentage " + percentOfParent);

		this.percentage = parent.percentage;
		this.array = copyBits(parent.array);

		int actualPercentage = parent.percentage * percentOfParent / maxNum;
		changePercentage(actualPercentage);
	}
	// given a user number from 0 to 99, see if it appears in the set of accepted numbers
	public boolean isAccepted(int userRandomNumber) throws PercentileException
	{
		// TODO just take (number % 100) - then any user number will do
		if (userRandomNumber < 0 || userRandomNumber >= maxNum)
			throw new PercentileException("Invalid user random number " + userRandomNumber);

		return isOn(userRandomNumber);
	}

	public int getEffectivePercentage()
	{
		return percentage;
	}
	public int[] getContent() // for internal debugging 
	{
		int[] src = new int[maxNum];
		int count = getListing(src, true); // get the numbers that are currently on

		int[] dest = new int[count];
		System.arraycopy( src, 0, dest, 0, count );
		return dest;
	}
	public String printBits()
	{
		int[] src = new int[maxNum];
		int count = getListing(src, true);
		if (count == maxNum)
			return String.format("0-%d", maxNum - 1);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; ++i)
		{
			if (sb.length() > 0)
				sb.append(" ");
			sb.append(src[i]);
		}
		return sb.toString();
	}
	// get a listing of on numbers or off numbers
	int getListing(int[] numbers, boolean getOn)
	{
	    int count = 0;
		for (int i = 0; i < maxNum; ++i)
		{
			if (getOn == isOn(i))
				numbers[count++] = i;
		}
		return count;
	}

	public void changePercentage(int newPercentage) throws PercentileException
	{
		if (newPercentage < 0 || newPercentage > maxNum)
			throw new PercentileException("Invalid percentage " + newPercentage);

	    if (newPercentage == percentage)
	    	return;

		if (newPercentage > percentage)
			increase(newPercentage);
		else
			decrease(newPercentage);

	    percentage = newPercentage;
	}

	// change a percentile for a sub-feature based on its parent feature
	public void changePercentage(Percentile parent, int newPercentOfParent) throws PercentileException
	{
		if (newPercentOfParent < 0 || newPercentOfParent > maxNum)
			throw new PercentileException("Invalid percentage " + newPercentOfParent);
		
		int actualPercentage = parent.percentage * newPercentOfParent / maxNum;
		if (actualPercentage == percentage)
			return;

		if (actualPercentage > percentage)
		{
			// extend the bitmap with random numbers that are available in the parent but not the child
			Percentile temp = new Percentile(0);
			temp.array = availableParentBits(parent.array, this.array);

		    int[] available = new int[maxNum];
			int count = temp.getListing(available, true);

			
		    shuffle(available, count);
		    int addedSize = actualPercentage - percentage;

		    for (int j = 0; j < addedSize; ++j)
		    {
		    	setOn(available[j]);
		    }

		}
		else // remove child bits that have been removed in the parent, then reduce to new size
		{
			Percentile temp = new Percentile(0);
			temp.array = intersectParentBits(parent.array, this.array);
			temp.percentage = temp.countOn();
			temp.decrease(actualPercentage);
			array = temp.array;
		}

		 percentage = actualPercentage;
	}
	void increase(int newPercentage)
	{
		// get the numbers that are currently off
		int[] numbers = new int[maxNum];
		int count = getListing(numbers, false);

		// extend the bitmap with random numbers that are currently off
	    shuffle(numbers, count);
	    int addedSize = newPercentage - percentage;

	    for (int j = 0; j < addedSize; ++j)
	    {
	    	setOn(numbers[j]);
	    }
	}
	void decrease(int newPercentage)
	{
		// get the numbers that are currently on
		int[] numbers = new int[maxNum];
		int count = getListing(numbers, true);

		// reduce the bitmap using random numbers that are currently on
	    shuffle(numbers, count);
	    int reducedSize = percentage - newPercentage;

	    for (int j = 0; j < reducedSize; ++j)
	    {
	    	setOff(numbers[j]);
	    }
	}
	void shuffle(int[] numbers, int size)
	{
	    Random random = new Random();
	    for (int i = size - 1; i > 0; --i)
	    {
	    	int index = random.nextInt(i + 1);
	    	int temp = numbers[index];
	        numbers[index] = numbers[i];
	        numbers[i] = temp;
	    }
	}

	boolean isOn(int i)
	{
		int onBit = 1 << (i % byteSZ);
		return (array[ i / byteSZ ]  & onBit) != 0;
	}
	void setOn(int i)
	{
		int onBit = 1 << (i % byteSZ);
		array[ i / byteSZ ]  |= (byte) onBit ;
	}
	void setOff(int i)
	{
		int onBit = 1 << (i % byteSZ);
		array[ i / byteSZ ]  &= ~((byte)onBit) ;
	}

	byte[] copyBits(byte[] in)
	{
		byte[] out = new byte[maxBytes];
		System.arraycopy(in, 0, out, 0, maxBytes);
		return out;
	}

	// get all bits used by the parent but not by the child
	byte[] availableParentBits(byte[] parent, byte[] child)
	{
		byte[] out = new byte[maxBytes];

		for (int i = 0; i < maxBytes; ++i)
		{
			out[i] = (byte) (parent[i] & ~child[i]);
		}
		return out;
	}

	// get all bits used by both parent and child
	byte[] intersectParentBits(byte[] parent, byte[] child)
	{
		byte[] out = new byte[maxBytes];

		for (int i = 0; i < maxBytes; ++i)
		{
			out[i] = (byte) (parent[i] & child[i]);
		}
		return out;
	}
	
	//-------------------------------------------------------------------
	/*
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("missing input json");
			return;
		}

		try {

			JSONObject features = Utilities.readJson(args[0]);
			try {
				features = features.getJSONObject(Constants.JSON_FIELD_ROOT);
			}
			catch (Exception e)
			{} // input is a feature, not a season

			System.out.println("Feature,Percent,\"Internal Percent\",Items");
			printPercentile(features);
		}
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
	}
	static void printPercentile(JSONObject obj) throws PercentileException, JSONException
	{
		String b64 = obj.optString(Constants.JSON_FEATURE_FIELD_PERCENTAGE_BITMAP);
		if (b64 != null)
		{
			Percentile p = new Percentile(b64);
			String name = obj.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE, "<unknown>") + "." + obj.optString(Constants.JSON_FIELD_NAME, "<unknown>");
			int percent = obj.optInt(Constants.JSON_FEATURE_FIELD_PERCENTAGE, 100);
			int[] items = p.getContent();
			System.out.print("\"" + name + "\"," +  percent + "," + p.getPercentage() + "," );
			for (int item : items)
			{
				System.out.print(item + " ");
			}
			System.out.println();
		}

		JSONArray conf = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
		if (conf != null)
		{
			for (int i = 0; i < conf.length(); ++i)
				printPercentile(conf.getJSONObject(i));
		}

		JSONArray array = obj.optJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
		if (array != null)
		{
			for (int i = 0; i < array.length(); ++i)
				printPercentile(array.getJSONObject(i));
		}
	}*/

}
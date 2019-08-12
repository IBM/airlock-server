package com.ibm.airlock.engine;

// add versions that break compatibility to this list.
// versions not found in the list are mapped to the most appropriate version using the find() method.
// use the .i suffix for quick numerical comparisons.
// Example:
// Version significant_version = Version.find("3.7.81") // returns the highest relevant version
// if ( significant_version.i  <  Version.v2_5.i ) {...} else {...}

public enum Version
{

	v1, v2_1, v2_5, v3_0, v4_0, v4_1, v4_5, v5_0, v5_5;


	public String s;
	public int i;

	Version()
	{
		this.i = this.ordinal();
		String str = this.toString().substring(1); // skip the v
		this.s = str.replace('_', '.'); // change 2_1 to 2.1
	}

	// find the highest version that matches the x.x... string
	public static Version find(String version)
	{
		Version[] all = Version.values();
		for (int i = all.length - 1; i >= 0; --i)
		{
			if (AirlockVersionComparator.compare(version, all[i].s) >= 0)
				return all[i];
		}
		return v1;
	}
}

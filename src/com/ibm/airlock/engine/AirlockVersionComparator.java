package com.ibm.airlock.engine;

/**
 * @author Rachel Levy
 * Compare between two strings represent application version
 */
public class AirlockVersionComparator{

    private static String[] s1Array;
    private static String[] s2Array;
    /**
     * Compare between two strings that are in Airlock version format.
     * @param s1 The first version.
     * @param s2 The second version.
     * @return an integer < 0 if s1 is less than s2, 0 if they are
     *         equal, and > 0 if s1 is greater than s2.
     */

    public static int compare(String s1, String s2) {

        if (s1!= null && s1.equals(s2))
            return 0;

        s1Array = (s1 == null ? new String[0] : s1.split("\\."));
        s2Array = (s2 == null ? new String[0] : s2.split("\\."));

        addPaddingToShorterArray();
        for (int i=0; i<s1Array.length; i++) {
            // try to compare numeric
            try {
                int s1Val = Integer.parseInt(s1Array[i]);
                int s2Val = Integer.parseInt(s2Array[i]);
                if (s1Val > s2Val) {
                    return 1;
                }
                else if (s1Val < s2Val) {
                    return -1;
                }
            }
            // compare Strings
            catch (NumberFormatException e1) {
                String s1Val = s1Array[i];
                String s2Val = s2Array[i];
                if (!s1Val.equals(s2Val)) {
                    return s1Val.compareTo(s2Val);
                }
            }
        }
        return 0;
    }

    public static boolean equals (String s1, String s2){
        return compare(s1,s2) == 0;
    }

    // add 0 for the shorter array
    private static void addPaddingToShorterArray(){
        if (s1Array.length > s2Array.length){
            String[] tmp = new String[s1Array.length];
            System.arraycopy(s2Array,0,tmp,0,s2Array.length);
            for (int i = s2Array.length; i < tmp.length ; i++) {
                tmp[i] = "0";
            }
            s2Array = tmp;
        }
        else if (s2Array.length > s1Array.length){
            String[] tmp = new String[s2Array.length];
            System.arraycopy(s1Array,0,tmp,0,s1Array.length);
            for (int i = s1Array.length; i < tmp.length ; i++) {
                tmp[i] = "0";
            }
            s1Array = tmp;
        }
    }
}


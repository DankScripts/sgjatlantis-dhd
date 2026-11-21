package com.mustangdoc.sgjpatch.compat;
import java.util.*;
/** Lantea's seventh destination symbol is a galaxy code and may repeat a place symbol. */
public final class NativeDhdAddress {
    private NativeDhdAddress() {}
    public static List<Integer> parse(String text,int alphabet) {
        if(text==null || text.isBlank() || text.length()>80 || !text.matches("[0-9,\\s-]+"))throw new IllegalArgumentException("Enter Lantea symbol numbers.");
        List<Integer> values=new ArrayList<>();
        try { for(String value:text.trim().split("[\\s,-]+"))values.add(Integer.parseInt(value)); }
        catch(NumberFormatException ex){throw new IllegalArgumentException("Invalid symbol number.");}
        if(!values.isEmpty()&&values.get(values.size()-1)==0)values.remove(values.size()-1);
        if(values.size()<6 || values.size()>7)throw new IllegalArgumentException("Use six destination symbols, plus a galaxy symbol if needed.");
        Set<Integer> seen=new HashSet<>();
        for(int i=0;i<values.size();i++) {
            int value=values.get(i);
            if(value<=0 || value>=alphabet || i<6&&!seen.add(value))throw new IllegalArgumentException("Invalid or repeated destination symbol.");
        }
        return List.copyOf(values);
    }
}

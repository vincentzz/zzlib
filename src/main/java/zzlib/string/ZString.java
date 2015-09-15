package zzlib.string;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by Vincent on 15/9/15.
 */
public class ZString {

    public static String makeString(Object[] objects, String prefix, String postfix, String connector, int start, int end) {
        int size = objects.length;
        if ( size < start || start > end ) return "";
        if ( size < end || end == 0 ) end = size;

        StringBuilder stringbuilder = new StringBuilder();

        for ( int i = start ; i < end ; i ++) {
            stringbuilder.append(prefix).append(objects[i].toString()).append(postfix).append(connector);
        }

        int currentLength = stringbuilder.length();
        stringbuilder.delete(currentLength-connector.length(), currentLength);

        return stringbuilder.toString();
    }

    public static String makeString(Collection objects, String prefix, String postfix, String connector, int start, int end) {
        return makeString(objects.toArray(), prefix, postfix, connector, start, end);
    }

    public static String makeString(Collection objects, String prefix, String postfix, String connector) {
        return makeString(objects.toArray(), prefix,  postfix,  connector, 0, 0);
    }

    public static String makeString(Collection objects, String connector) {
        return makeString(objects.toArray(), "",  "",  connector, 0, 0);
    }

    public static void main (String[] args) {
        LinkedList<String> sl = new LinkedList<String>();
        sl.add("asdf");
        sl.add("a321f");
        sl.add("a1s321df");
        sl.add("a32112sdf");
        System.out.println(makeString(sl,"\"","\"",",,,",0,4));
    }
}

package zzlib.date;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Vincent on 15/9/16.
 */
public class ZDate {
    public static String format (Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern(format);
        SimpleDateFormat.getInstance();
        return "";
    }
}

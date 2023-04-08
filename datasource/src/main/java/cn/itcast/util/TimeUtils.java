package cn.itcast.util;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * desc:时间工具类
 */
public abstract class TimeUtils {
    public static String format(Long timestamp,String pattern){
        return FastDateFormat.getInstance(pattern).format(timestamp);
    }

    public static void main(String[] args) {
        System.out.println(TimeUtils.format(System.currentTimeMillis(), "yyyy-MM-dd"));
    }
}

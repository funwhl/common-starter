package com.eighteen.common.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by wangwei.
 * Date: 2020/3/25
 * Time: 13:03
 */
public class DateUtils {
    public static void foreachRange(Date start, Date end, Consumer<Date> action) {
        while (start.before(end)) {
            action.accept(start);
            start = new Date(start.getTime()+TimeUnit.MINUTES.toMillis(1));
        }
    }

    public static void main(String[] args) {
        foreachRange(new Date(new Date().getTime() - TimeUnit.DAYS.toMillis(1)), new Date(),
                date -> {
                    System.out.println(date + "-" + new Date(date.getTime() + TimeUnit.MINUTES.toMillis(1)));
                });
    }
}

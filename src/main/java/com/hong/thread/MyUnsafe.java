package com.hong.thread;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class MyUnsafe {

    public static Unsafe getUnsafe() {
        Field field;
        try {
            field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}

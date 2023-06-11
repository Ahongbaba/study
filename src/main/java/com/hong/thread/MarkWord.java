package com.hong.thread;

import lombok.Data;

@Data
public class MarkWord {

    private String lockFlag = "01";

    private ObjectMonitor ptrMonitor = null;
}

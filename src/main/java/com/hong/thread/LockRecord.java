package com.hong.thread;

import lombok.Data;

@Data
public class LockRecord {

    // head
    private MarkWord markWord;

    private MarkWord owner;
}

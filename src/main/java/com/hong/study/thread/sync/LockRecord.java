package com.hong.study.thread.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LockRecord {

    // head
    private MarkWord markWord;

    private MarkWord owner;
}

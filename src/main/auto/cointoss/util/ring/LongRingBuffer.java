/*
 * Copyright (C) 2020 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.util.ring;

import javax.annotation.processing.Generated;


@Generated("SpecializedCodeGenerator")
public class LongRingBuffer {

    /** The fixed buffer size. */
    private final int size;

    /** The actual buffer. */
    private final long[] buffer;

    /** The current index. */
    private int index = -1;

    /**
     * Create new buffer.
     * 
     * @param size A fixed buffer size.
     */
    public LongRingBuffer(int size) {
        this.size = size;
        this.buffer = new long[size];
    }

    /**
     * Add an item at tail.
     * 
     * @param item An item to add.
     * @return Removed item.
     */
    public long add(long item) {
        int nextIndex = (index + 1) % size;
        long prev = buffer[nextIndex];
        buffer[nextIndex] = item;
        index = nextIndex;
        return prev;
    }

    /**
     * Get the latest item.
     * 
     * @return A latest item.
     */
    public long latest() {
        return buffer[index];
    }
}
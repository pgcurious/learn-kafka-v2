package com.kafka.learning.chapter01.model;

/**
 * Offset information for a partition.
 *
 * @param partition     Partition number
 * @param beginOffset   Earliest available offset
 * @param endOffset     Next offset to be written (log end offset)
 * @param messageCount  Number of messages (end - begin)
 */
public record PartitionOffsetInfo(
        int partition,
        long beginOffset,
        long endOffset,
        long messageCount
) {
}

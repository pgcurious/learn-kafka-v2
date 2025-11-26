package com.kafka.learning.chapter01.model;

/**
 * Information about a broker's log directory.
 *
 * @param path           Filesystem path of the log directory
 * @param partitionCount Number of partitions stored in this directory
 * @param totalSize      Total size in bytes of all data in this directory
 * @param error          Error message if there was an issue reading this directory
 */
public record LogDirInfo(
        String path,
        int partitionCount,
        long totalSize,
        String error
) {
    /**
     * Returns the size in a human-readable format.
     */
    public String totalSizeFormatted() {
        if (totalSize < 1024) return totalSize + " B";
        if (totalSize < 1024 * 1024) return String.format("%.2f KB", totalSize / 1024.0);
        if (totalSize < 1024 * 1024 * 1024) return String.format("%.2f MB", totalSize / (1024.0 * 1024));
        return String.format("%.2f GB", totalSize / (1024.0 * 1024 * 1024));
    }
}

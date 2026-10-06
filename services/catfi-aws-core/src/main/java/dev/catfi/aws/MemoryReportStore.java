package dev.catfi.aws;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MemoryReportStore implements ReportStore {
  private final Map<String, ReportDocument> values = new ConcurrentHashMap<>();
  public Optional<ReportDocument> find(String id) { return Optional.ofNullable(values.get(id)); }
  public synchronized boolean compareAndSet(ReportDocument next, long expected) {
    long actual = find(next.propertyId()).map(ReportDocument::version).orElse(0L);
    if (actual != expected) return false;
    values.put(next.propertyId(), next); return true;
  }
  public Page list(String cursor, int limit) {
    if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be 1..100");
    List<ReportDocument> items = values.values().stream().sorted(Comparator.comparing(ReportDocument::propertyId))
        .filter(v -> cursor == null || v.propertyId().compareTo(cursor) > 0).limit(limit + 1L).toList();
    boolean more = items.size() > limit;
    List<ReportDocument> selected = items.subList(0, Math.min(limit, items.size()));
    return new Page(List.copyOf(selected), more ? selected.get(selected.size()-1).propertyId() : null);
  }
}

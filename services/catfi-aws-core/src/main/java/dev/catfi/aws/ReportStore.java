package dev.catfi.aws;

import java.util.List;
import java.util.Optional;

public interface ReportStore {
  Optional<ReportDocument> find(String propertyId);
  boolean compareAndSet(ReportDocument next, long expectedVersion);
  Page list(String cursor, int limit);
  record Page(List<ReportDocument> items, String cursor) {}
}

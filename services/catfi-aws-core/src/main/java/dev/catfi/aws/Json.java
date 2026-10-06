package dev.catfi.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class Json {
  public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
  private Json() {}
  public static String write(Object value) {
    try { return MAPPER.writeValueAsString(value); }
    catch (Exception e) { throw new IllegalArgumentException("Cannot serialize document", e); }
  }
  public static <T> T read(String value, Class<T> type) {
    try { return MAPPER.readValue(value, type); }
    catch (Exception e) { throw new IllegalArgumentException("Invalid document", e); }
  }
}

package com.techconqueror.initializr.core.utility;

import java.util.stream.Stream;

public final class Try<T> {

  private final T value;

  private final Exception exception;

  public interface CheckedSupplier<T> {

    T get() throws Exception;
  }

  public static <T> Try<T> success(T value) {
    return new Try<>(value, null);
  }

  public static <T> Try<T> exception(Exception exception) {
    return new Try<>(null, exception);
  }

  public static <T> Try<T> empty() {
    return new Try<>(null, null);
  }

  public static <T> Try<T> of(CheckedSupplier<T> supplier) {
    try {
      return success(supplier.get());
    } catch (Exception e) {
      return exception(e);
    }
  }

  public Stream<T> valueStream() {
    return Stream.ofNullable(value);
  }

  private Try(T value, Exception exception) {
    this.value = value;
    this.exception = exception;
  }
}

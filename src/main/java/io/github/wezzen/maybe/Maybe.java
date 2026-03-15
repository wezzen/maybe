package io.github.wezzen.maybe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A null-safe container that forces explicit handling of both present and absent cases.
 *
 * <p>Unlike {@link Optional}, {@code Maybe} has no {@code get()} method.
 * The only ways to retrieve a value are:
 * <ul>
 *   <li>{@link #resolve(Function, Supplier)} — handle both cases, returns a value</li>
 *   <li>{@link #consume(Consumer, Runnable)} — handle both cases, void</li>
 *   <li>{@link #orElse(Object)}, {@link #orElseGet(Supplier)}, {@link #orElseThrow(Supplier)}</li>
 * </ul>
 *
 * <h2>Creating a Maybe</h2>
 * <pre>{@code
 * Maybe<String> a = Maybe.of(nullable);      // from nullable value
 * Maybe<String> b = Maybe.require(nonNull);  // guaranteed non-null, throws if null
 * Maybe<String> c = Maybe.empty();           // explicitly absent
 * }</pre>
 *
 * <h2>Forced bifurcation — both cases must be handled</h2>
 * <pre>{@code
 * String greeting = user.resolve(
 *     u  -> "Hello, " + u.name(),
 *     () -> "Hello, stranger"
 * );
 *
 * user.consume(
 *     u  -> emailService.send(u.email()),
 *     () -> log.warn("User not found")
 * );
 * }</pre>
 *
 * <h2>Early return pattern</h2>
 * <pre>{@code
 * Maybe<String> result = someMethod();
 *
 * if (result.isEmpty()) {
 *     logger.log("no value");
 *     return;
 * }
 *
 * String value = result.orElseThrow(); // safe — already checked above
 * computeOnData(value);
 * }</pre>
 *
 * <h2>Stream integration</h2>
 * <pre>{@code
 * // filter nulls from a list
 * List<Integer> ids = raw.stream()
 *     .map(this::parseId)
 *     .collect(Maybe.present());
 *
 * // convert from Optional
 * Maybe<String> maybe = optional.map(Maybe::of).orElse(Maybe.empty());
 * }</pre>
 *
 * @param <T> the type of the contained value
 */
public final class Maybe<T> {

    private static final Maybe<?> EMPTY = new Maybe<>(null);

    private final T value;

    private Maybe(final T value) {
        this.value = value;
    }

    /**
     * Creates a {@code Maybe} from a nullable value.
     * Returns a present {@code Maybe} if value is non-null, empty otherwise.
     */
    public static <T> @NotNull Maybe<T> of(@Nullable final T value) {
        return Objects.nonNull(value) ? new Maybe<>(value) : empty();
    }

    /**
     * Creates a {@code Maybe} from a guaranteed non-null value.
     * Use this when you are certain the value is non-null — throws immediately if not.
     *
     * @throws NullPointerException if value is null — use {@link #of} for nullable values
     */
    public static <T> @NotNull Maybe<T> require(@NotNull final T value) {
        Objects.requireNonNull(value, "Maybe.require() requires a non-null value. " +
                "Use Maybe.of() for nullable values");
        return new Maybe<>(value);
    }

    /**
     * Returns the singleton empty {@code Maybe}.
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull Maybe<T> empty() {
        return (Maybe<T>) EMPTY;
    }

    /**
     * The primary way to extract a value from {@code Maybe}.
     * Forces you to handle both the present and absent cases.
     *
     * <p>There is no way to call this and "forget" the empty branch.
     *
     * <pre>{@code
     * String greeting = user.resolve(
     *     u  -> "Hello, " + u.name(),
     *     () -> "Hello, stranger"
     * );
     * }</pre>
     *
     * @param ifPresent function applied to the value if present
     * @param ifEmpty   supplier called if absent
     * @return result of whichever function was applied
     */
    public <R> @NotNull R resolve(
            @NotNull final Function<? super T, ? extends R> ifPresent,
            @NotNull final Supplier<? extends R> ifEmpty
    ) {
        Objects.requireNonNull(ifPresent, "ifPresent must not be null");
        Objects.requireNonNull(ifEmpty, "ifEmpty must not be null");
        if (isPresent()) {
            return Objects.requireNonNull(ifPresent.apply(this.value),
                    "ifPresent() must return a non-null value");
        }
        return Objects.requireNonNull(ifEmpty.get(), "ifEmpty must return a non-null value");
    }

    /**
     * Void variant of {@link #resolve} for side effects.
     * Forces you to handle both the present and absent cases.
     *
     * <p>Use this when you don't need to return a value.
     * Prefer {@link #resolve} when you need to return a result.
     *
     * <pre>{@code
     * user.consume(
     *     u  -> emailService.send(u.email()),
     *     () -> log.warn("User not found")
     * );
     * }</pre>
     *
     * @param ifPresent consumer called with the value if present
     * @param ifEmpty   runnable called if absent
     */
    public void consume(
            @NotNull final Consumer<? super T> ifPresent,
            @NotNull final Runnable ifEmpty
    ) {
        Objects.requireNonNull(ifPresent, "ifPresent must not be null");
        Objects.requireNonNull(ifEmpty, "ifEmpty must not be null");
        if (isPresent()) {
            ifPresent.accept(value);
        } else {
            ifEmpty.run();
        }
    }

    /**
     * Returns the value if present, otherwise {@code fallback}.
     *
     * <p>Note: {@code fallback} is always evaluated regardless of whether
     * the value is present — use {@link #orElseGet} to avoid unnecessary evaluation.
     */
    public @NotNull T orElse(@NotNull final T fallback) {
        Objects.requireNonNull(fallback, "fallback must not be null");
        return isPresent() ? value : fallback;
    }

    /**
     * Returns the value if present, otherwise the result of {@code fallback}.
     * The fallback is only called if the value is absent.
     */
    public @NotNull T orElseGet(@NotNull final Supplier<? extends T> fallback) {
        Objects.requireNonNull(fallback, "fallback must not be null");
        if (isPresent()) {
            return value;
        }
        final T result = fallback.get();
        return Objects.requireNonNull(result, "fallback supplier must not return null");
    }

    /**
     * Returns the value if present, otherwise throws the exception from {@code exceptionSupplier}.
     *
     * @throws X if the value is absent
     */
    public <X extends Throwable> @NotNull T orElseThrow(
            @NotNull final Supplier<? extends X> exceptionSupplier
    ) throws X {
        Objects.requireNonNull(exceptionSupplier, "exceptionSupplier must not be null");
        if (isPresent()) {
            return value;
        }
        throw exceptionSupplier.get();
    }

    /**
     * Returns {@code true} if a value is present.
     */
    public boolean isPresent() {
        return Objects.nonNull(value);
    }

    /**
     * Returns {@code true} if no value is present.
     */
    public boolean isEmpty() {
        return Objects.isNull(value);
    }

    /**
     * Applies {@code mapper} to the value if present, wrapping the result in a new {@code Maybe}.
     * If the mapper returns {@code null}, the result is an empty {@code Maybe}.
     */
    public <R> @NotNull Maybe<R> map(
            @NotNull final Function<? super T, ? extends @Nullable R> mapper
    ) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return isPresent() ? Maybe.of(mapper.apply(value)) : Maybe.empty();
    }

    /**
     * Applies {@code mapper} to the value if present, returning the resulting {@code Maybe}.
     * The mapper must not return {@code null} — return {@link #empty()} instead.
     */
    public <R> @NotNull Maybe<R> flatMap(
            @NotNull final Function<? super T, @NotNull Maybe<? extends R>> mapper
    ) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        if (isEmpty()) {
            return Maybe.empty();
        }
        @SuppressWarnings("unchecked") final Maybe<R> result = (Maybe<R>) mapper.apply(value);
        return Objects.requireNonNull(
                result,
                "flatMap mapper must not return null"
        );
    }

    /**
     * Returns this {@code Maybe} if the value is present and matches the predicate,
     * otherwise returns an empty {@code Maybe}.
     */
    public @NotNull Maybe<T> filter(@NotNull final Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        if (isPresent()) {
            return predicate.test(value) ? this : Maybe.empty();
        }
        return this;
    }

    /**
     * Returns this {@code Maybe} if present, otherwise the result of {@code fallback}.
     */
    public @NotNull Maybe<T> or(@NotNull final Supplier<@NotNull Maybe<T>> fallback) {
        Objects.requireNonNull(fallback, "fallback must not be null");
        if (isPresent()) {
            return this;
        }
        final Maybe<T> result = fallback.get();
        return Objects.requireNonNull(result, "fallback supplier must not return null");
    }

    /**
     * Executes {@code action} if a value is present. Does nothing if absent.
     * Prefer {@link #consume} when you also need to handle the empty case.
     */
    public void ifPresent(@NotNull final Consumer<? super T> action) {
        Objects.requireNonNull(action, "action must not be null");
        if (isPresent()) {
            action.accept(value);
        }
    }

    /**
     * Converts this {@code Maybe} to an {@link Optional}.
     */
    public @NotNull Optional<T> toOptional() {
        return Optional.ofNullable(value);
    }

    /**
     * Returns a {@link Stream} of the value if present, or an empty stream.
     * Useful for {@code list.stream().flatMap(x -> Maybe.of(x).stream())}.
     */
    public @NotNull Stream<T> stream() {
        return isPresent() ? Stream.of(value) : Stream.empty();
    }

    /**
     * A collector that extracts only present values from a stream of {@code Maybe}.
     *
     * <pre>{@code
     * List<Integer> ids = raw.stream()
     *     .map(this::parseId)
     *     .collect(Maybe.present());
     * }</pre>
     */
    public static <T> Collector<Maybe<T>, ?, List<T>> present() {
        return Collectors.flatMapping(
                Maybe::stream,
                Collectors.toList()
        );
    }

    /**
     * Maps each element of a stream with a Maybe-returning function,
     * returning only the present values.
     *
     * <p>Instead of:
     * <pre>{@code stream.flatMap(x -> Maybe.of(f(x)).stream())}</pre>
     * write:
     * <pre>{@code Maybe.mapPresent(stream, f)}</pre>
     */
    public static <T, R> @NotNull Stream<R> mapPresent(
            @NotNull final Stream<T> stream,
            @NotNull final Function<T, @NotNull Maybe<R>> mapper
    ) {
        Objects.requireNonNull(stream, "stream must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");
        return stream.flatMap(x -> {
            final Maybe<R> result = mapper.apply(x);
            Objects.requireNonNull(result,
                    "mapper must not return null");
            return result.stream();
        });
    }

    /**
     * Filters a stream of Maybe, returning only the present values.
     *
     * <p>Instead of:
     * <pre>{@code stream.flatMap(Maybe::stream)}</pre>
     * write:
     * <pre>{@code Maybe.filterPresent(stream)}</pre>
     */
    public static <T> @NotNull Stream<T> filterPresent(@NotNull final Stream<Maybe<T>> stream) {
        Objects.requireNonNull(stream, "stream must not be null");
        return stream.flatMap(Maybe::stream);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Maybe<?> other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        return isPresent() ? "Maybe[" + value + "]" : "Maybe.empty()";
    }
}

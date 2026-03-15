package io.github.wezzen.maybe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("DataFlowIssue")
final class MaybeTest {

    @Nested
    @DisplayName("Maybe.of()")
    class Of {

        @Test
        void returnsPresent_whenValueNonNull() {
            assertThat(Maybe.of("hello").isPresent()).isTrue();
        }

        @Test
        void returnsEmpty_whenValueNull() {
            assertThat(Maybe.of(null).isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("Maybe.require()")
    class Require {

        @Test
        void returnsPresent_whenValueNonNull() {
            assertThat(Maybe.require(42).isPresent()).isTrue();
        }

        @Test
        void throwsNPE_whenValueNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.require(null))
                    .withMessageContaining("Maybe.require()");
        }
    }

    @Nested
    @DisplayName("Maybe.empty()")
    class Empty {

        @Test
        void isEmpty() {
            assertThat(Maybe.empty().isEmpty()).isTrue();
        }

        @Test
        void isNotPresent() {
            assertThat(Maybe.empty().isPresent()).isFalse();
        }
    }


    @Nested
    @DisplayName("isPresent() and isEmpty() symmetry")
    class Symmetry {

        @Test
        void presentImpliesNotEmpty() {
            final Maybe<String> maybe = Maybe.of("x");
            assertThat(maybe.isPresent()).isEqualTo(!maybe.isEmpty());
        }

        @Test
        void emptyImpliesNotPresent() {
            final Maybe<String> maybe = Maybe.empty();
            assertThat(maybe.isEmpty()).isEqualTo(!maybe.isPresent());
        }
    }


    @Nested
    @DisplayName("resolve()")
    class Resolve {

        @Test
        void callsIfPresent_whenPresent() {
            final String result = Maybe.of("world").resolve(
                    v -> "hello " + v,
                    () -> "nobody"
            );
            assertThat(result).isEqualTo("hello world");
        }

        @Test
        void callsIfEmpty_whenEmpty() {
            final String result = Maybe.<String>empty().resolve(
                    v -> "hello " + v,
                    () -> "nobody"
            );
            assertThat(result).isEqualTo("nobody");
        }

        @Test
        void doesNotCallIfEmpty_whenPresent() {
            final AtomicBoolean emptyCalled = new AtomicBoolean(false);
            Maybe.of("x").resolve(
                    v -> v,
                    () -> {
                        emptyCalled.set(true);
                        return "fallback";
                    }
            );
            assertThat(emptyCalled).isFalse();
        }

        @Test
        void doesNotCallIfPresent_whenEmpty() {
            final AtomicBoolean presentCalled = new AtomicBoolean(false);
            Maybe.<String>empty().resolve(
                    v -> {
                        presentCalled.set(true);
                        return v;
                    },
                    () -> "fallback"
            );
            assertThat(presentCalled).isFalse();
        }

        @Test
        void throwsNPE_whenIfPresentIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.of("x").resolve(null, () -> "fallback"))
                    .withMessageContaining("ifPresent");
        }

        @Test
        void throwsNPE_whenIfEmptyIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.of("x").resolve(v -> v, null))
                    .withMessageContaining("ifEmpty");
        }

        @Test
        void throwsNPE_whenIfPresentReturnsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.of("x").resolve(v -> null, () -> "fallback"))
                    .withMessageContaining("ifPresent");
        }

        @Test
        void throwsNPE_whenIfEmptyReturnsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.<String>empty().resolve(v -> v, () -> null))
                    .withMessageContaining("ifEmpty");
        }
    }

    @Nested
    @DisplayName("consume()")
    class Consume {

        @Test
        void callsIfPresent_whenPresent() {
            final AtomicReference<String> received = new AtomicReference<>();
            Maybe.of("hello").consume(
                    received::set,
                    () -> fail("should not call ifEmpty")
            );
            assertThat(received.get()).isEqualTo("hello");
        }

        @Test
        void callsIfEmpty_whenEmpty() {
            final AtomicBoolean emptyCalled = new AtomicBoolean(false);
            Maybe.<String>empty().consume(
                    v -> fail("should not call ifPresent"),
                    () -> emptyCalled.set(true)
            );
            assertThat(emptyCalled).isTrue();
        }

        @Test
        void doesNotCallIfEmpty_whenPresent() {
            final AtomicBoolean emptyCalled = new AtomicBoolean(false);
            Maybe.of("x").consume(
                    v -> {
                    },
                    () -> emptyCalled.set(true)
            );
            assertThat(emptyCalled).isFalse();
        }

        @Test
        void doesNotCallIfPresent_whenEmpty() {
            final AtomicBoolean presentCalled = new AtomicBoolean(false);
            Maybe.<String>empty().consume(
                    v -> presentCalled.set(true),
                    () -> {
                    }
            );
            assertThat(presentCalled).isFalse();
        }

        @Test
        void throwsNPE_whenIfPresentIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.of("x").consume(null, () -> {
                    }))
                    .withMessageContaining("ifPresent");
        }

        @Test
        void throwsNPE_whenIfEmptyIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.of("x").consume(v -> {
                    }, null))
                    .withMessageContaining("ifEmpty");
        }
    }

    @Nested
    @DisplayName("orElse()")
    class OrElse {

        @Test
        void returnsValue_whenPresent() {
            assertThat(Maybe.of("a").orElse("b")).isEqualTo("a");
        }

        @Test
        void returnsFallback_whenEmpty() {
            assertThat(Maybe.<String>empty().orElse("b")).isEqualTo("b");
        }

        @Test
        void throwsNPE_whenFallbackIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.empty().orElse(null))
                    .withMessageContaining("fallback");
        }
    }

    @Nested
    @DisplayName("orElseGet()")
    class OrElseGet {

        @Test
        void returnsValue_whenPresent() {
            assertThat(Maybe.of("a").orElseGet(() -> "b")).isEqualTo("a");
        }

        @Test
        void doesNotCallSupplier_whenPresent() {
            final AtomicBoolean called = new AtomicBoolean(false);
            Maybe.of("x").orElseGet(() -> {
                called.set(true);
                return "fallback";
            });
            assertThat(called).isFalse();
        }

        @Test
        void callsSupplier_whenEmpty() {
            assertThat(Maybe.<String>empty().orElseGet(() -> "computed")).isEqualTo("computed");
        }

        @Test
        void throwsNPE_whenFallbackIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.of("x").orElseGet(null))
                    .withMessageContaining("fallback");
        }

        @Test
        void throwsNPE_whenSupplierReturnsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.empty().orElseGet(() -> null))
                    .withMessageContaining("fallback supplier");
        }
    }

    @Nested
    @DisplayName("orElseThrow()")
    class OrElseThrow {

        @Test
        void returnsValue_whenPresent() {
            assertThat(Maybe.of(1).orElseThrow(RuntimeException::new)).isEqualTo(1);
        }

        @Test
        void throwsGivenException_whenEmpty() {
            assertThatThrownBy(() ->
                    Maybe.empty().orElseThrow(() -> new IllegalStateException("gone"))
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("gone");
        }

        @Test
        void throwsNPE_whenExceptionSupplierIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.empty().orElseThrow(null))
                    .withMessageContaining("exceptionSupplier");
        }
    }

    @Nested
    @DisplayName("map()")
    class MapTest {

        @Test
        void transformsValue_whenPresent() {
            assertThat(Maybe.of("hello").map(String::toUpperCase).orElse("")).isEqualTo("HELLO");
        }

        @Test
        void returnsEmpty_whenEmpty() {
            assertThat(Maybe.<String>empty().map(String::toUpperCase).isEmpty()).isTrue();
        }

        @Test
        void returnsEmpty_whenMapperReturnsNull() {
            assertThat(Maybe.of("x").map(v -> null).isEmpty()).isTrue();
        }

        @Test
        void canChain_multipleMaps() {
            final int result = Maybe.of("  hello  ")
                    .map(String::trim)
                    .map(String::length)
                    .orElse(0);
            assertThat(result).isEqualTo(5);
        }

        @Test
        void throwsNPE_whenMapperIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.of("x").map(null))
                    .withMessageContaining("mapper");
        }
    }

    @Nested
    @DisplayName("flatMap()")
    class FlatMapTest {

        @Test
        void chainsPresent() {
            final Maybe<Integer> result = Maybe.of("42")
                    .flatMap(s -> Maybe.of(Integer.parseInt(s)));
            assertThat(result.orElse(0)).isEqualTo(42);
        }

        @Test
        void returnsEmpty_whenSourceIsEmpty() {
            assertThat(Maybe.<String>empty()
                    .flatMap(s -> Maybe.of(s.length()))
                    .isEmpty()
            ).isTrue();
        }

        @Test
        void returnsEmpty_whenMapperReturnsEmpty() {
            assertThat(Maybe.of("x")
                    .flatMap(s -> Maybe.empty())
                    .isEmpty()
            ).isTrue();
        }

        @Test
        void throwsNPE_whenMapperReturnsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.of("x").flatMap(s -> null))
                    .withMessageContaining("must not return null");
        }

        @Test
        void throwsNPE_whenMapperIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.of("x").flatMap(null))
                    .withMessageContaining("mapper");
        }
    }

    @Nested
    @DisplayName("filter()")
    class FilterTest {

        @Test
        void keepsValue_whenPredicateMatches() {
            assertThat(Maybe.of(5).filter(n -> n > 3).isPresent()).isTrue();
        }

        @Test
        void returnsEmpty_whenPredicateDoesNotMatch() {
            assertThat(Maybe.of(5).filter(n -> n > 10).isEmpty()).isTrue();
        }

        @Test
        void returnsEmpty_whenAlreadyEmpty() {
            assertThat(Maybe.<Integer>empty().filter(n -> true).isEmpty()).isTrue();
        }

        @Test
        void throwsNPE_whenPredicateIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.of("x").filter(null))
                    .withMessageContaining("predicate");
        }
    }

    @Nested
    @DisplayName("or()")
    class OrTest {

        @Test
        void returnsOriginal_whenPresent() {
            assertThat(Maybe.of("a").or(() -> Maybe.of("b")).orElse("")).isEqualTo("a");
        }

        @Test
        void returnsFallback_whenEmpty() {
            assertThat(Maybe.<String>empty().or(() -> Maybe.of("b")).orElse("")).isEqualTo("b");
        }

        @Test
        void doesNotCallFallback_whenPresent() {
            final AtomicBoolean called = new AtomicBoolean(false);
            Maybe.of("a").or(() -> {
                called.set(true);
                return Maybe.of("b");
            });
            assertThat(called).isFalse();
        }

        @Test
        void returnsEmpty_whenFallbackReturnsEmpty() {
            assertThat(Maybe.<String>empty().or(Maybe::empty).isEmpty()).isTrue();
        }

        @Test
        void throwsNPE_whenFallbackIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.<String>empty().or(null))
                    .withMessageContaining("fallback");
        }

        @Test
        void throwsNPE_whenFallbackReturnsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.<String>empty().or(() -> null))
                    .withMessageContaining("fallback supplier");
        }
    }

    @Nested
    @DisplayName("ifPresent()")
    class IfPresentTest {

        @Test
        void executesAction_whenPresent() {
            final AtomicReference<String> received = new AtomicReference<>();
            Maybe.of("hello").ifPresent(received::set);
            assertThat(received.get()).isEqualTo("hello");
        }

        @Test
        void doesNotExecuteAction_whenEmpty() {
            final AtomicBoolean called = new AtomicBoolean(false);
            Maybe.<String>empty().ifPresent(v -> called.set(true));
            assertThat(called).isFalse();
        }

        @Test
        void throwsNPE_whenActionIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.of("x").ifPresent(null))
                    .withMessageContaining("action");
        }
    }

    @Nested
    @DisplayName("toOptional()")
    class ToOptional {

        @Test
        void convertsPresent() {
            assertThat(Maybe.of("x").toOptional()).contains("x");
        }

        @Test
        void convertsEmpty() {
            assertThat(Maybe.<String>empty().toOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("stream()")
    class StreamTest {

        @Test
        void returnsOneElement_whenPresent() {
            assertThat(Maybe.of("x").stream()).containsExactly("x");
        }

        @Test
        void returnsEmptyStream_whenEmpty() {
            assertThat(Maybe.empty().stream()).isEmpty();
        }

        @Test
        void worksWithFlatMap_inStreamPipeline() {
            List<String> result = Stream.of("a", null, "b", null, "c")
                    .flatMap(v -> Maybe.of(v).stream())
                    .toList();
            assertThat(result).containsExactly("a", "b", "c");
        }
    }

    @Nested
    @DisplayName("present() collector")
    class PresentCollector {

        @Test
        void returnsOnlyPresentValues_fromMixedStream() {
            final List<String> result = Stream.<Maybe<String>>of(
                    Maybe.of("a"),
                    Maybe.empty(),
                    Maybe.of("b"),
                    Maybe.empty(),
                    Maybe.of("c")
            ).collect(Maybe.present());
            assertThat(result).containsExactly("a", "b", "c");
        }

        @Test
        void returnsEmptyList_whenAllEmpty() {
            final List<String> result = Stream.of(
                    Maybe.<String>empty(),
                    Maybe.<String>empty()
            ).collect(Maybe.present());
            assertThat(result).isEmpty();
        }

        @Test
        void returnsAll_whenNoneEmpty() {
            final List<String> result = Stream.of(
                    Maybe.of("a"),
                    Maybe.of("b")
            ).collect(Maybe.present());
            assertThat(result).containsExactly("a", "b");
        }
    }

    @Nested
    @DisplayName("mapPresent()")
    class MapPresent {

        @Test
        void returnsOnlyPresentValues() {
            final List<String> raw = Arrays.asList("1", "abc", "2", "???", "3");
            final List<Integer> result = Maybe.mapPresent(raw.stream(), this::parseId).toList();
            assertThat(result).containsExactly(1, 2, 3);
        }

        @Test
        void returnsEmpty_whenAllMapToEmpty() {
            final List<String> raw = List.of("abc", "???");
            final List<Integer> result = Maybe.mapPresent(raw.stream(), this::parseId).toList();
            assertThat(result).isEmpty();
        }

        @Test
        void returnsAll_whenNoneMapToEmpty() {
            final List<String> raw = List.of("1", "2", "3");
            final List<Integer> result = Maybe.mapPresent(raw.stream(), this::parseId).toList();
            assertThat(result).containsExactly(1, 2, 3);
        }

        private Maybe<Integer> parseId(String s) {
            try {
                return Maybe.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Maybe.empty();
            }
        }

        @Test
        void throwsNPE_whenStreamIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.mapPresent(null, Maybe::of))
                    .withMessageContaining("stream");
        }

        @Test
        void throwsNPE_whenMapperIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.mapPresent(Stream.of("x"), null))
                    .withMessageContaining("mapper");
        }

        @Test
        void throwsNPE_whenMapperReturnsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> {
                                final var ignored = Maybe.mapPresent(Stream.of("x"), v -> null).toList();
                            }
                    )
                    .withMessageContaining("mapper");
        }
    }

    @Nested
    @DisplayName("filterPresent()")
    class FilterPresent {

        @Test
        void returnsOnlyPresentValues() {
            final Stream<Maybe<String>> stream = Stream.of(
                    Maybe.of("a"),
                    Maybe.empty(),
                    Maybe.of("b"),
                    Maybe.empty(),
                    Maybe.of("c")
            );
            assertThat(Maybe.filterPresent(stream).toList())
                    .containsExactly("a", "b", "c");
        }

        @Test
        void returnsEmpty_whenAllEmpty() {
            final Stream<Maybe<String>> stream = Stream.of(
                    Maybe.empty(),
                    Maybe.empty()
            );
            assertThat(Maybe.filterPresent(stream).toList()).isEmpty();
        }

        @Test
        void returnsAll_whenNoneEmpty() {
            final Stream<Maybe<String>> stream = Stream.of(
                    Maybe.of("a"),
                    Maybe.of("b")
            );
            assertThat(Maybe.filterPresent(stream).toList())
                    .containsExactly("a", "b");
        }

        @Test
        void throwsNPE_whenStreamIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Maybe.filterPresent(null))
                    .withMessageContaining("stream");
        }
    }


    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsHashCode {

        @Test
        void equalMaybes_withSameValue() {
            final Maybe<String> a = Maybe.of("x");
            final Maybe<String> b = Maybe.of("x");
            assertThat(a).isEqualTo(b);
        }

        @Test
        void notEqual_withDifferentValues() {
            assertThat(Maybe.of("x")).isNotEqualTo(Maybe.of("y"));
        }

        @Test
        void emptyMaybes_areEqual() {
            final Maybe<String> a = Maybe.empty();
            final Maybe<String> b = Maybe.empty();
            assertThat(a).isEqualTo(b);
        }

        @Test
        void presentAndEmpty_areNotEqual() {
            assertThat(Maybe.of("x")).isNotEqualTo(Maybe.empty());
        }

        @Test
        void equalMaybes_haveSameHashCode() {
            final Maybe<String> a = Maybe.of("x");
            final Maybe<String> b = Maybe.of("x");
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTest {

        @Test
        void showsValue_whenPresent() {
            assertThat(Maybe.of("hello").toString()).isEqualTo("Maybe[hello]");
        }

        @Test
        void showsEmpty_whenEmpty() {
            assertThat(Maybe.empty().toString()).isEqualTo("Maybe.empty()");
        }
    }
}
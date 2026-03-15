# Maybe

[![CI](https://github.com/wezzen/maybe/actions/workflows/ci.yml/badge.svg)](https://github.com/wezzen/maybe/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.wezzen/maybe)](https://central.sonatype.com/artifact/io.github.wezzen/maybe)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)

A null-safe `Maybe<T>` container for Java that forces explicit handling of both present and absent cases — with no `get()` method.

## The Problem

Java's `Optional<T>` allows calling `.get()` without checking `.isPresent()` first, which leads to runtime exceptions:

```java
Optional<String> value = findUser();
value.get(); // compiles fine, but throws NoSuchElementException if empty
```

`Maybe<T>` solves this by simply not having a `get()` method. The only ways to extract a value are safe by design.

## Installation

**Gradle (Kotlin DSL):**

```kotlin
implementation("io.github.wezzen:maybe:0.1.0")
```

**Gradle (Groovy):**

```groovy
implementation 'io.github.wezzen:maybe:0.1.0'
```

**Maven:**

```xml
<dependency>
    <groupId>io.github.wezzen</groupId>
    <artifactId>maybe</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick Start

```java
// Create
Maybe<String> present = Maybe.of("hello");       // from nullable value
Maybe<String> certain = Maybe.require("hello");  // throws if null
Maybe<String> absent  = Maybe.empty();           // explicitly absent

// Extract — forced to handle both cases
String greeting = user.resolve(
    u  -> "Hello, " + u.name(),   // present branch
    () -> "Hello, stranger"        // absent branch — cannot be forgotten
);

// Side effects — forced to handle both cases
user.consume(
    u  -> emailService.send(u.email()),
    () -> log.warn("User not found")
);
```

## API Reference

### Creating a Maybe

| Method | Description |
|--------|-------------|
| `Maybe.of(T value)` | Creates a `Maybe` from a nullable value |
| `Maybe.require(T value)` | Creates a `Maybe` from a non-null value, throws `NullPointerException` if null |
| `Maybe.empty()` | Returns the singleton empty `Maybe` |

### Extracting a Value

| Method | Description |
|--------|-------------|
| `resolve(ifPresent, ifEmpty)` | Returns a value — forces handling of both cases |
| `consume(ifPresent, ifEmpty)` | Void — forces handling of both cases |
| `orElse(fallback)` | Returns value if present, otherwise `fallback` |
| `orElseGet(supplier)` | Returns value if present, otherwise calls `supplier` |
| `orElseThrow(supplier)` | Returns value if present, otherwise throws |
| `orElseThrow()` | Returns value if present, otherwise throws `NoSuchElementException` |

### Transformations

| Method | Description |
|--------|-------------|
| `map(mapper)` | Applies mapper to the value if present |
| `flatMap(mapper)` | Applies mapper returning `Maybe` if present |
| `filter(predicate)` | Returns empty if value does not match predicate |
| `or(supplier)` | Returns this if present, otherwise result of supplier |
| `ifPresent(action)` | Executes action if present |

### Stream Integration

| Method | Description |
|--------|-------------|
| `stream()` | Returns a `Stream` of one element or empty |
| `Maybe.present()` | Collector that keeps only present values |
| `Maybe.mapPresent(stream, mapper)` | Maps and filters in one step |
| `Maybe.filterPresent(stream)` | Filters only present values |

### Interop

| Method | Description |
|--------|-------------|
| `toOptional()` | Converts to `Optional<T>` |
| `isPresent()` | Returns `true` if value is present |
| `isEmpty()` | Returns `true` if value is absent |

## Examples

### Early Return Pattern

```java
Maybe<String> result = fetchFromDatabase(id);

if (result.isEmpty()) {
    logger.warn("Value not found for id: {}", id);
    return;
}

String value = result.orElseThrow(); // safe — already checked above
process(value);
doMoreStuff(value);
```

### Stream Pipeline

```java
// Filter nulls from a list
List<Integer> ids = rawValues.stream()
    .map(this::parseId)
    .collect(Maybe.present());

// Parse only valid values
Maybe<Integer> parseId(String s) {
    try {
        return Maybe.of(Integer.parseInt(s));
    } catch (NumberFormatException e) {
        return Maybe.empty();
    }
}
```

### Chaining

```java
String city = Maybe.of(getOrder())
    .map(Order::customer)
    .map(Customer::address)
    .map(Address::city)
    .orElse("Unknown city");
```

### Converting from Optional

```java
Optional<String> optional = findValue();
Maybe<String> maybe = optional.map(Maybe::of).orElse(Maybe.empty());
```

## Requirements

- Java 17+

## License

This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
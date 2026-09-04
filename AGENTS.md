# CastielLib — Agent Instructions

> Project-specific rules layered on the global agent framework.

## Stack Context

- Language: Java 8 bytecode, built with a JDK 25 toolchain
- Framework: Paper API 26.x compile target with compatibility abstractions
- Build: Gradle single-module Java library
- Key dependencies: Paper API, HikariCP, XSeries

## Idiosyncrasies

- CastielLib is shaded into consuming plugins; it is not installed as a server plugin.
- Existing consumers may rely on Java 8 bytecode and current public APIs.
- Modern additions may be versioned alongside compatibility surfaces when required.

## Backlog

<!-- Add project-specific work here when requested. -->

## Project-Specific Rules

- Keep only genuinely reusable infrastructure here; product/game domain belongs in consumers.
- Preserve existing public APIs unless a versioned replacement and compatibility tests are supplied.
- New public behavior requires focused tests and concise Javadoc.

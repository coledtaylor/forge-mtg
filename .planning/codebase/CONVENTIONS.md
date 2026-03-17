# Coding Conventions

**Analysis Date:** 2026-03-16

## Naming Patterns

**Files:**
- PascalCase for all class files: `AbilityKey.java`, `SpellAbilityEffect.java`, `DamageDealAiTest.java`
- Effect classes use suffix pattern: `*Effect.java` (e.g., `AbandonEffect.java`, `AlterAttributeEffect.java`)
- Test classes use suffix pattern: `*Test.java` or `*Tests.java`
- Package names follow Java convention in lowercase with dots: `forge.game.ability`, `forge.ai.ability`

**Functions/Methods:**
- camelCase for all method names: `getDefinedCards()`, `calculateAmount()`, `payManaViaConvoke()`
- Prefix patterns observed:
  - `get*()` for accessors
  - `calculate*()` for computation methods
  - `add*()` for collection operations
  - `is*()` or `has*()` for boolean checks
  - `find*()` for search operations

**Variables:**
- camelCase for local variables and parameters: `hostCard`, `colorsToPay`, `expectedRemainder`
- `final` keyword used frequently for immutability: `final Card hostCard`, `final String def`
- Constants in UPPER_SNAKE_CASE with `static final`: `cmpList` (private static final)

**Types:**
- PascalCase for classes and enums: `AbilityKey`, `ApiType`, `GameEntity`
- Enum values in UPPER_SNAKE_CASE: values like `AbilityMana`, `Activator`, `Affected` (defined as enum constants)
- Generic types used commonly: `FCollection<GameObject>`, `PlayerCollection`, `CardCollectionView`

## Code Style

**Formatting:**
- Maven Checkstyle enforced via `pom.xml` with configuration in `checkstyle.xml`
- Minimal checkstyle rules: RedundantImport and UnusedImports checked
- UTF-8 encoding enforced: `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`
- Java 17 target: Maven compiler plugin configured for release 17

**Linting:**
- Maven Checkstyle Plugin v3.1.2 with Checkstyle 10.18.2
- Configuration file: `/checkstyle.xml`
- Runs in validate phase: `<phase>validate</phase>`
- Fails on error: `<failsOnError>true</failsOnError>`
- Includes test source directories: `<includeTestSourceDirectory>true</includeTestSourceDirectory>`

## Import Organization

**Order:**
1. Standard Java imports: `java.util.*`, `java.util.Map.Entry`
2. Third-party library imports: `com.google.common.collect.*`, `org.apache.commons.lang3.*`
3. Forge internal imports: `forge.game.ability.*`, `forge.card.*`
4. Static imports: `static forge.card.MagicColor.WHITE`

**Path Aliases:**
- No explicit path aliases observed; imports use full package paths
- Domain model packages deeply nested: `forge.game.ability`, `forge.game.card`, `forge.game.player`

## Error Handling

**Patterns:**
- Try-catch blocks used for exception handling: `try { ... } catch (final Exception e) { ... }`
- Final keyword on caught exceptions: `catch (final Exception e)`
- Custom exceptions: `IllegalAbilityException` exists as domain-specific exception
- Exceptions logged or handled contextually, not globally swallowed
- Sentry integration for error tracking: `import io.sentry.Sentry` and breadcrumb logging observed

## Logging

**Framework:** SLF4J API (configured in dependencyManagement as v2.0.16)

**Patterns:**
- No explicit logging seen in sampled code; framework setup suggests structured logging
- Sentry breadcrumb pattern: `Breadcrumb` and `Sentry` imported in utility classes
- Contextual breadcrumbs for tracking game state changes and ability resolution

## Comments

**When to Comment:**
- Used for non-obvious logic: "// default to Self" in AbilityUtils
- Marks special cases: "// An actual copy should be made." in test setup
- Explains complex conditional logic: comments in DamageDealAiTest for test scenarios
- Not verbose for obvious code

**JavaDoc/JSDoc:**
- JavaDoc present on public methods and classes
- Format: `/** ... */` blocks with `@param` and `@return` tags
- Example from SpellAbilityEffect: `/** Returns this effect description with needed prelude and epilogue. @param params @param sa @return */`
- Class-level JavaDoc includes author attribution: `@author Forge`
- Not enforced on private methods

## Function Design

**Size:**
- Utility methods vary widely; AbilityUtils has large methods (3887 lines total) with internal helper methods
- Test methods focused and concise: 10-50 lines typical
- Helper methods extracted: `runConvokeTest()`, `createManaCostBeingPaid()` in test classes

**Parameters:**
- Heavy use of `final` keyword for parameter immutability
- Card, SpellAbility, and Player are frequent parameter types (domain model objects)
- Generic collections used: `CardCollectionView`, `FCollection<T>`, `List<T>`
- Method overloading for convenience: `countCardsWithName(game, name)` vs `countCardsWithName(game, name, type)`

**Return Values:**
- Specific collection types returned: `CardCollection`, `PlayerCollection`, `FCollection<T>`
- Null return possible but not explicit null-safe handling observed
- void return for command/action methods

## Module Design

**Exports:**
- Public methods explicitly exported from utility classes
- Public static methods common for factory/utility patterns: `AbilityKey.fromString()`, `AbilityKey.newMap()`
- Package-private or protected for test base classes: `AITest` is public with protected helper methods

**Barrel Files:**
- No explicit barrel files/index.ts pattern observed
- Package organization serves as module definition: abilities grouped under `forge.game.ability.*`

---

*Convention analysis: 2026-03-16*

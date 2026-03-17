# Testing Patterns

**Analysis Date:** 2026-03-16

## Test Framework

**Runner:**
- TestNG v6+ (based on import pattern and Maven Surefire configuration)
- Maven Surefire Plugin v3.1.2 for test execution
- Config: `pom.xml` with Surefire configuration (JVM module opening for reflection access)

**Assertion Library:**
- `org.testng.AssertJUnit` - TestNG's JUnit-style assertions

**Run Commands:**
```bash
mvn test                  # Run all tests
mvn test -Dtest=TestName # Run specific test
mvn verify                # Run tests as part of verify phase
mvn clean test            # Clean and run tests
```

## Test File Organization

**Location:**
- Co-located with source: `src/test/java/` mirrors `src/main/java/` structure
- Test files in same package as source: `forge.game.ability.AbilityKeyTest` tests `forge.game.ability.AbilityKey`
- Module structure: `forge-game/src/test/java/`, `forge-gui-desktop/src/test/java/`

**Naming:**
- Suffix pattern: `*Test.java` or `*Tests.java`
- Examples: `AbilityKeyTest.java`, `DamageDealAiTest.java`, `AIIntegrationTests.java`

**Structure:**
```
forge-game/src/test/java/forge/game/ability/AbilityKeyTest.java
forge-gui-desktop/src/test/java/forge/ai/ability/DamageDealAiTest.java
forge-gui-desktop/src/test/java/forge/ai/AIIntegrationTests.java
```

## Test Structure

**Suite Organization:**
```java
public class AbilityKeyTest {
    @Test
    public void testFromStringWorksForAllKeys() {
        for (AbilityKey key : AbilityKey.values()) {
            AssertJUnit.assertEquals(key, AbilityKey.fromString(key.toString()));
        }
    }

    @Test
    public void testCopyingEmptyMapWorks() {
        Map<AbilityKey, Object> map = Maps.newHashMap();
        Map<AbilityKey, Object> newMap = AbilityKey.newMap(map);
        AssertJUnit.assertNotSame(map, newMap);
    }
}
```

**Patterns:**
- Setup inline within test methods (no @Before/@BeforeClass observed in sampled tests)
- Each `@Test` method is independent and self-contained
- Direct assertions with `AssertJUnit.assertEquals()`, `AssertJUnit.assertTrue()`, `AssertJUnit.assertNotSame()`
- No teardown observed (TestNG cleanup handled automatically)
- Inheritance for shared test utilities: `DamageDealAiTest extends AITest`

## Mocking

**Framework:** None explicitly configured (Google Guava used for test data: `Maps.newHashMap()`, `Lists.newArrayList()`)

**Patterns:**
```java
// Creating mock/stub objects for testing:
List<RegisteredPlayer> players = Lists.newArrayList();
Deck d1 = new Deck();
players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("opponent", null)));
GameRules rules = new GameRules(GameType.Constructed);
Match match = new Match(rules, players, "Test");
Game game = new Game(players, rules, match);
```

```java
// Direct object construction in tests:
Card dummySource = addCard("Mountain", ai);
SpellAbility damageSa = new SpellAbility.EmptySa(ApiType.DealDamage, dummySource, ai);
damageSa.putParam("NumDmg", "5");
```

**What to Mock:**
- Game state objects created directly, not mocked
- Cards created via helper: `addCard("Card Name", player)`
- Players created as `LobbyPlayerAi` for AI testing
- No true mocking library (Mockito) observed; manual test doubles used

**What NOT to Mock:**
- Domain model objects (Card, SpellAbility, Player) - create real instances
- Game state (Game, Match) - full game state created for integration testing
- Card effects and abilities - tested with actual game engine

## Fixtures and Factories

**Test Data:**
```java
// Helper methods in base class AITest:
protected Card addCard(String name, Player player) {
    // Creates and adds card to player's battlefield
}

protected void addCardToZone(String name, Player player, ZoneType zone) {
    // Creates card in specific zone
}

protected void playUntilPhase(Game game, PhaseType phase) {
    // Game loop automation
}
```

**Location:**
- Base class `AITest` in `/forge-gui-desktop/src/test/java/forge/ai/AITest.java`
- Shared by all AI-related tests: `DamageDealAiTest extends AITest`
- Game initialization and creation in base class: `initAndCreateGame()`, `resetGame()`

## Coverage

**Requirements:** No coverage target enforced (no JaCoCo or coverage plugin configured)

**View Coverage:**
```bash
# Coverage would require adding plugin to pom.xml
# Currently not configured in build
```

## Test Types

**Unit Tests:**
- Scope: Individual components in isolation
- Approach: `AbilityKeyTest` tests enum conversion, `ManaCostBeingPaidTest` tests mana payment logic
- Setup: Direct object creation, no complex fixture setup
- Example: `testFromStringWorksForAllKeys()` - tests single enum conversion

**Integration Tests:**
- Scope: Game state with multiple components
- Approach: Full Game object with multiple players and board state
- Files: `AIIntegrationTests.java`, `GameSimulationTest.java`, `BasicAttackTests.java`
- Example: `testAttackTriggers()` creates game, adds creatures, verifies combat mechanics work
- Setup: `initAndCreateGame()` initializes card database, creates two-player game
- Heavy use of game loops: `playUntilNextTurn()`, `gameLoopUntilNextPhase()`

**E2E Tests:**
- Framework: Not explicitly configured
- Approach: Integration tests serve as end-to-end validation
- Coverage: AI decision-making tested against actual game state

## Common Patterns

**Async Testing:**
```java
// Game loop execution in tests (not traditional async, but step-based):
protected void gameLoopUntilNextPhase(Game game) {
    int maxIterations = 100;
    int iterations = 0;
    PhaseType currentPhase = game.getPhaseHandler().getPhase();
    while (!game.isGameOver() && iterations < maxIterations) {
        game.getPhaseHandler().mainLoopStep();
        iterations++;
        if (!game.getPhaseHandler().is(currentPhase)) {
            break;
        }
    }
}
```

**Error Testing:**
```java
// Testing with expected outcomes:
@Test
public void testPayManaViaConvoke() {
    runConvokeTest("1 W W", new byte[] { WHITE, COLORLESS, WHITE },
                   new String[] { "{1}{W}{W}", "{1}{W}", "{W}" });
    // Verifies state progression through multiple assertions
}

private void runConvokeTest(String initialCost, byte[] colorsToPay, String[] expectedRemainder) {
    ManaCostBeingPaid costBeingPaid = createManaCostBeingPaid(initialCost);
    for (int i = 0; i < colorsToPay.length; i++) {
        AssertJUnit.assertEquals(expectedRemainder[i], costBeingPaid.toString());
        costBeingPaid.payManaViaConvoke(colorsToPay[i]);
    }
    AssertJUnit.assertEquals("0", costBeingPaid.toString());
}
```

**Test Helper Pattern:**
```java
// Private helper methods for test reusability:
private ManaCostBeingPaid createManaCostBeingPaid(String costString) {
    ManaCostParser parsedCostString = new ManaCostParser(costString);
    ManaCost manaCost = new ManaCost(parsedCostString);
    return new ManaCostBeingPaid(manaCost);
}

// Called from multiple test methods
```

## Test Dependencies

**Critical:**
- Forge game engine (`forge-game` module) - required for integration tests
- Card database - initialized in `AITest.initAndCreateGame()` via `FModel.initialize()`
- Google Guava - used for collection initialization in tests

**Maven Configuration:**
- Surefire JVM args open Java modules for reflection access:
  ```
  --add-opens java.base/java.lang=ALL-UNNAMED
  --add-opens java.base/java.util=ALL-UNNAMED
  --add-opens java.desktop/javax.imageio.spi=ALL-UNNAMED
  ```

---

*Testing analysis: 2026-03-16*

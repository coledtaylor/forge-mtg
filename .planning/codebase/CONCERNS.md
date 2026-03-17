# Codebase Concerns

**Analysis Date:** 2026-03-16

## Tech Debt

### AI Evaluation Logic Gaps

**Issue:** AI decision-making contains numerous incomplete or simplified evaluations that limit strategy depth.

**Files:**
- `forge-ai/src/main/java/forge/ai/SpecialCardAi.java` (70+ TODOs)
- `forge-ai/src/main/java/forge/ai/ComputerUtilCard.java` (45+ TODOs)
- `forge-ai/src/main/java/forge/ai/ComputerUtilAbility.java`
- `forge-ai/src/main/java/forge/ai/ComputerUtilCombat.java`

**Examples:**
- Card evaluation defaults to CMC comparisons (line 1839 in SpecialCardAi.java) instead of actual strategic value
- Non-creature permanents have "difficulty evaluating" (line 211) leading to suboptimal plays
- Sacrifice logic doesn't check if the sacrifice could kill opponent with remaining board (line 485)
- Creature damage prediction uses simplified "2*lands" formula instead of actual calculation (line 1501)
- Trample damage calculation incomplete, marked as "trickier" to implement (line 165 in SpecialAiLogic.java)

**Impact:** AI opponents play predictably and suboptimally, reducing challenge and entertainment value. Players can exploit these gaps by playing cards that the AI undervalues.

**Fix approach:**
1. Create unified card valuation system that considers permanence, strategic role, synergies
2. Build comprehensive combat damage predictor accounting for all mechanics
3. Systematize special case handling through configurable AI profiles instead of hardcoded conditionals

---

### Hardcoded Magic Numbers and Configuration

**Issue:** AI logic contains hardcoded values that should be configurable per AI profile.

**Files:**
- `forge-ai/src/main/java/forge/ai/ability/ConniveAi.java` (line 1241): Library margin hardcoded to 5
- `forge-ai/src/main/java/forge/ai/ComputerUtilMana.java` (line 414): Damping Sphere handling incomplete
- `forge-ai/src/main/java/forge/ai/AiAttackController.java`: "bad magic numbers" acknowledged in comments
- `forge-ai/src/main/java/forge/ai/ability/BalanceAi.java`: Counting lands/creatures instead of evaluating actual value
- `forge-ai/src/main/java/forge/ai/ability/PermanentAi.java`: Cost reduction spells hardcoded for specific Illusions

**Impact:** AI behavior cannot be fine-tuned without code changes. Identical profiles across difficulty levels.

**Fix approach:**
1. Extract hardcoded values to AiProfile configuration
2. Create parameter validation framework to ensure safe ranges
3. Allow dynamic difficulty adjustment without recompilation

---

### Map.get() Without Null Checks

**Issue:** Extensive use of `Map.get()` without null checks creates potential NullPointerException risks.

**Files:**
- `forge-game/src/main/java/forge/game/card/Card.java` (26 instanceof checks, multiple unsafe get() calls):
  - Line: `states.get(getAlternateStateName())` - no null check
  - Line: `clStates.get(state)` - no null check
  - Line: `mergedCards.get(0)` - potential IndexOutOfBoundsException
  - Line: `replaceMap.get(oldValue)` - no null check
  - Line: `chosenColors.get(0)` - potential IndexOutOfBoundsException
  - Line: `mayPlay.get(sta)` - no null check

**Impact:** Runtime crashes in edge case scenarios where expected map entries don't exist. Particularly risky in game state transitions and card interactions.

**Fix approach:**
1. Replace `map.get()` with `map.getOrDefault()` where default behavior is known
2. Use Optional<> wrapping for uncertain lookups
3. Add defensive null checks with descriptive error messages pointing to root cause
4. Add unit tests for missing state scenarios

---

## Known Bugs

### iOS Implementation Incomplete

**Issue:** iOS implementation contains unimplemented placeholders that would cause runtime failures.

**Files:** `forge-gui-ios/src/forge/ios/Main.java`

**Symptoms:** Lines 100 and 105 contain `// TODO implement this` comments in critical game logic.

**Trigger:** Running game on iOS platform would hit unimplemented code paths.

**Workaround:** Avoid iOS deployment until implementation completed.

---

### Stale Game State on Stack

**Issue:** Cards can become stuck on the stack and invalidated with no recovery mechanism.

**Files:** `forge-ai/src/main/java/forge/ai/ComputerUtil.java` (line 132)

**Symptoms:** FIXME comment indicates "Card seems to be stuck on stack zone and invalidated and nowhere to be found"

**Trigger:** Complex interaction sequences with zone changes during spell resolution

**Impact:** Game hangs or crashes when attempting to resolve stuck spells. Players cannot continue game.

**Workaround:** None - requires restart and avoiding specific card interactions.

---

### AI Spectacle Cost Identification

**Issue:** Abilities with Spectacle have duplicate representation making them hard to identify as same ability.

**Files:** `forge-ai/src/main/java/forge/ai/ComputerUtilAbility.java` (line 323)

**Symptoms:** FIXME comment: "Any better way to identify that these are the same ability, one with Spectacle and one not?"

**Impact:** AI may evaluate the same ability twice with different cost assumptions, leading to incorrect play decisions.

---

### LDA Model Generation Unknown Failure

**Issue:** LDA model generation has unexplained failure point with no documented cause.

**Files:** `forge-lda/src/forge/lda/LDAModelGenerator.java` (line 358)

**Symptoms:** Comment states "Not sure what was failing here" - code continues without recovery.

**Trigger:** During statistical model building for card analysis

**Impact:** AI deck statistics may be incomplete or inaccurate, affecting matchmaking quality.

---

## Security Considerations

### System.out/err Logging in AI Code

**Issue:** Direct console output instead of proper logging framework leaks internal state and debug information.

**Files:** Multiple files in `forge-ai/src/main/java/forge/ai/`:
- `ChooseCardAi.java`: System.err
- `ChoosePlayerAi.java`: System.out (multiple)
- `ChooseSourceAi.java`: System.err
- `CountersMultiplyAi.java`: System.out
- `DamageDealAi.java`: System.out (mana cost warnings)
- `FightAi.java`: System.out (warning messages)
- `PumpAi.java`: System.err
- `SetStateAi.java`: System.err (2 locations)
- `AiAttackController.java`: System.out (13 locations) - detailed strategy information
- `AiController.java`: System.err (2 locations)
- `ComputerUtilMana.java`: DEBUG_MANA_PAYMENT flag with extensive System.out (lines 915-1610)

**Risk:**
- Debug output visible to players reveals AI decision logic
- Performance degradation from unbuffered console I/O during AI thinking
- Difficulty controlling log verbosity in production

**Current mitigation:** DEBUG_MANA_PAYMENT static flag provides some control, but other System.out calls are unconditional.

**Recommendations:**
1. Replace all System.out/err with Logger.info/error calls
2. Use consistent log levels (DEBUG for detailed AI thinking, INFO for warnings)
3. Implement log level configuration per AI module
4. Remove DEBUG_MANA_PAYMENT flag and use proper logging levels

---

### Sentry Error Reporting Configuration

**Issue:** Sentry breadcrumb collection in Card.java (`forge-game/src/main/java/forge/game/card/Card.java` lines 2779, 3307) adds game state details to crash reports without clear data handling policy.

**Risk:** Player game state, card combinations, and decision history could be sent to external service.

**Current mitigation:** Sentry integration present but configuration not visible in Card.java

**Recommendations:**
1. Document what data gets sent to Sentry
2. Add opt-out mechanism for players concerned about privacy
3. Sanitize card/player identifiers before sending
4. Review breadcrumb payloads for sensitive information

---

## Performance Bottlenecks

### Large Monolithic Classes

**Issue:** Multiple core classes exceed 3000+ lines, creating cognitive load and compilation overhead.

**Files:**
- `forge-game/src/main/java/forge/game/card/Card.java` (8,198 lines) - Card representation and all operations
- `forge-game/src/main/java/forge/game/card/CardFactoryUtil.java` (4,178 lines) - Card construction logic
- `forge-game/src/main/java/forge/game/player/Player.java` (4,073 lines) - Player state and mechanics
- `forge-game/src/main/java/forge/game/ability/AbilityUtils.java` (3,887 lines) - Ability evaluation helpers
- `forge-gui/src/main/java/forge/player/PlayerControllerHuman.java` (3,487 lines) - Human player input handling
- `forge-ai/src/main/java/forge/ai/ComputerUtil.java` (3,202 lines) - AI decision utilities
- `forge-gui-desktop/src/main/java/forge/toolbox/FSkin.java` (3,120 lines) - Theming/skinning system
- `forge-game/src/main/java/forge/game/GameAction.java` (2,904 lines) - Game action processing

**Impact:**
- Difficult to navigate and modify
- High memory usage during compilation
- Testing requires mocking many responsibilities
- Risk of unintended side effects when modifying
- Slower startup times

**Improvement path:**
1. Extract Card to separate concerns: CardProperty (2,118 lines), CardState, CardZone
2. Split PlayerController into InputHandler, DecisionMaker, ActionExecutor
3. Break ComputerUtil into ComputerUtilCombat (2,609 lines), ComputerUtilCard, ComputerUtilAbility
4. Consider spitting out CardFactoryUtil into CardParser + CardBuilder
5. Use composition over god objects for FSkin

---

### Mana Payment Resolution Complexity

**Issue:** ComputerUtilMana contains DEBUG_MANA_PAYMENT flag with extensive debug output indicating complex reasoning that's difficult to trace.

**Files:** `forge-ai/src/main/java/forge/ai/ComputerUtilMana.java` (1,624 lines total)

**Cause:** Mana source ranking, color shard matching, and reflection checking are intertwined with debug output.

**Improvement path:**
1. Extract mana ranking logic to separate ManaSourceRanker class
2. Create ManaShardMatcher for color combination logic
3. Add structured logging/tracing for payment decisions
4. Profile actual mana payment time with realistic deck sizes

---

## Fragile Areas

### Ability Duplicate Detection

**Issue:** No robust mechanism to identify logically equivalent abilities with different configurations.

**Files:**
- `forge-ai/src/main/java/forge/ai/ComputerUtilAbility.java` (line 452): "doesn't account for nearly identical creatures where one is a newer but more cost efficient variant"

**Why fragile:** Hardcoded equality checks based on hostCard and ApiType (line 1) miss variants like:
- Same ability with different cost modifiers (Spectacle, Kicker, Surge)
- Functionally identical abilities from different cards
- Modal abilities in different configurations

**Safe modification:**
1. Create AbilityVariant wrapper tracking cost modifiers and conditions
2. Implement semantic equality checking for ability types
3. Add unit tests comparing variant forms of same ability

**Test coverage:** No visible unit tests for ability deduplication logic

---

### Card State Transitions

**Issue:** Card state management uses Map-based lookup without validation that state exists.

**Files:** `forge-game/src/main/java/forge/game/card/Card.java`

**Why fragile:** Multiple state lookups without defensive checks:
- `states.get(getAlternateStateName())` - returns null if alternate form doesn't exist
- `states.get(CardStateName.FaceDown)` - assumes face-down state initialized
- `clStates.get(state)` - no check before accessing

**Safe modification:**
1. Use Optional<CardState> for all state lookups
2. Validate state existence during card initialization
3. Document state transition rules
4. Add unit tests for all state combinations (Original, Transformed, FaceDown, Melded)

**Test coverage:** Gaps in state transition testing visible from unsafe get() calls

---

### Static Ability Initialization

**Issue:** 520 files use static initialization blocks - potential for ordering issues and memory leaks.

**Files:** Large number across all modules, particularly in:
- `forge-core/` - Card database initialization
- `forge-game/` - Game rules and ability registry
- `forge-ai/` - AI profile and decision tree loading

**Why fragile:**
- Class loading order not guaranteed to be deterministic
- Circular dependencies can cause incomplete initialization
- Static state persists across game instances
- Difficult to test in isolation

**Safe modification:**
1. Audit all static initialization blocks for circular dependencies
2. Replace eager static initialization with lazy loading where possible
3. Use dependency injection for rule/ability registries
4. Document initialization order requirements

**Test coverage:** Limited - static initialization difficult to unit test

---

### Trigger and Replacement Effect Handling

**Issue:** Replacement effects and triggers interact in ways that are fragile to new card mechanics.

**Files:**
- `forge-game/src/main/java/forge/game/trigger/TriggerHandler.java`
- `forge-game/src/main/java/forge/game/replacement/ReplacementEffect.java`
- References in `ComputerUtilCard.java` lines 1119-1214 with multiple TODOs

**Symptoms:**
- Line 1119: "boat-load of when blah dies triggers" not accounted for
- Line 1147: "add threat from triggers and other abilities" incomplete
- Line 1214: "add threat from triggers" for Bident of Thassa incomplete

**Why fragile:**
- Trigger evaluation doesn't account for all trigger types
- Threat assessment incomplete for trigger-based abilities
- New trigger keywords may not integrate properly with AI decision making

**Safe modification:**
1. Create TriggerEvaluator with pluggable trigger handlers
2. Document all supported trigger types and edge cases
3. Add tests for new trigger keywords before adding to game
4. Create threat assessment framework for trigger-based abilities

---

## Scaling Limits

### Card Database Performance

**Issue:** CardDb integration with large card database (2000+ cards analyzed) and CardArtPreference system could impact startup and search performance.

**Current capacity:** Handles modern MTG sets (~1000+ unique cards) but unclear scaling with future expansion sets.

**Limit:** Database load time not profiled; art preference lookup cost not measured.

**Scaling path:**
1. Profile CardDb initialization time with full set of cards
2. Implement card lazy-loading instead of full load at startup
3. Cache art preferences with LRU eviction
4. Consider indexed card search for filter operations

---

### Game Simulation Tree Complexity

**Issue:** AI simulation in GameSimulationTest.java (2,717 lines) explores game trees for decision making. Branching factor increases exponentially with game complexity.

**Current capacity:** Not documented; simulations work for standard games but may timeout with complex board states.

**Limit:** Exponential growth with number of possible actions (spell choices, block combinations, targeting options).

**Scaling path:**
1. Implement branch pruning for low-value game states
2. Add simulation time budget and cutoff mechanism
3. Profile simulation depth requirements for different game states
4. Consider Monte Carlo sampling instead of exhaustive search for complex states

---

### Concurrent Input Handling

**Issue:** InputQueue and InputProxy use concurrent data structures but thread safety model not fully documented.

**Files:**
- `forge-gui/src/main/java/forge/gamemodes/match/input/InputQueue.java` - BlockingDeque
- `forge-gui/src/main/java/forge/gamemodes/match/input/InputProxy.java` - AtomicReference
- `forge-gui/src/main/java/forge/gamemodes/match/input/InputLockUI.java` - AtomicInteger
- `forge-gui/src/main/java/forge/gamemodes/match/input/InputSyncronizedBase.java` - CountDownLatch

**Current capacity:** Handles local multiplayer and AI opponent OK, but unclear under high-frequency online play.

**Limit:** Blocking queue depth, atomic reference contention under high event rate.

**Scaling path:**
1. Profile input handling latency under load
2. Implement input batching for high-frequency events
3. Consider event sourcing for deterministic replay
4. Document thread safety invariants for each input handler

---

## Dependencies at Risk

### Sentry Integration (io.sentry 8.21.1)

**Risk:**
- Version 8.21.1 relatively recent; should verify security advisories
- External dependency for crash reporting adds third-party risk
- Configuration not visible; unclear if data handling meets user expectations

**Impact:** If Sentry service goes down, breadcrumb creation could fail silently or loudly.

**Migration plan:**
- Alternative: SLF4J with local log aggregation
- Alternative: Open Telemetry for portable observability
- Gradual deprecation: Make Sentry optional, add fallback logging

---

### Deprecated CardType Method

**Issue:** CardType has deprecated method without clear migration path.

**Files:** `forge-core/src/main/java/forge/card/CardType.java`

**Symptoms:** `@deprecated` annotation present but no comment indicating replacement method.

**Impact:** Code calling deprecated method generates warnings and may break in future update.

**Migration plan:**
1. Document which method replaces deprecated one
2. Create automated refactoring script if multiple call sites
3. Set deadline for removal (e.g., next major version)

---

### Deprecated CardEdition Method

**Issue:** CardEdition has deprecated method with inline comment.

**Files:** `forge-core/src/main/java/forge/card/CardEdition.java`

**Symptoms:** `@Deprecated //Use CardEdition::hasBasicLands and a nonnull test.` - guidance provided but may not be clear to all callers.

**Impact:** Multiple callers may not migrate to new method.

**Migration plan:**
1. Grep for all references to deprecated method
2. Create refactoring guide with examples
3. Update deprecation to point to javadoc with migration examples

---

## Missing Critical Features

### MDFC (Modal Double-Faced Card) Support

**Issue:** Modal Double-Faced Cards only partially supported in AI evaluation.

**Files:** `forge-ai/src/main/java/forge/ai/ComputerUtilCard.java` (line 735)

**Problem:** Comment states "when PlayAi can consider MDFC this should also look at the back face (if not on stack or battlefield)"

**Blocks:**
- Proper valuation of cards like "Charming Prince" that provide different value on two faces
- Strategic choice of which face to use based on game situation
- Deck building recommendations for MDFC cards

---

### Incomplete Threat Assessment

**Issue:** AI threat evaluation missing key ability types that generate threats.

**Files:** `forge-ai/src/main/java/forge/ai/ComputerUtilCard.java` lines 1119-1214

**Missing from threat assessment:**
- Trigger-based damage sources (line 1147)
- Death trigger effects (line 1119)
- On-attack effects like Bident of Thassa (line 1214)
- When-spell-cast effects
- At-beginning-of-combat effects

**Impact:** AI undervalues permanents with trigger abilities, leading to poor blocking and attack decisions.

---

### Limited Mana Reflection Checking

**Issue:** Replacement effect checking for mana doesn't handle reflected colors.

**Files:** `forge-ai/src/main/java/forge/ai/ComputerUtilMana.java` (line 1636)

**Problem:** Comment "TODO: Replacement Check currently doesn't work for reflected colors" indicates incomplete mana color evaluation.

**Impact:** Mana sources that adjust color requirements aren't properly accounted for, causing payment failure or suboptimal choices.

---

## Test Coverage Gaps

### AI Decision Making

**Untested area:** SpecialCardAi logic for specific card abilities - 70+ TODO comments indicate incomplete implementations.

**Files:** `forge-ai/src/main/java/forge/ai/SpecialCardAi.java`

**What's not tested:**
- Casualty cost evaluation logic
- Non-creature permanent threat assessment
- Draw effect risk analysis for opponent effects
- Card selection for tutor effects based on board state
- Sacrifice-based ability evaluation

**Risk:** New cards with these mechanics won't have proper AI support; bugs in rarely-tested code paths discovered in production.

**Priority:** High - SpecialCardAi is core decision logic

---

### Card State Transitions

**Untested area:** Transform, Meld, Face Down state transitions and interaction with card abilities.

**Files:** `forge-game/src/main/java/forge/game/card/Card.java` - no visible state transition tests

**What's not tested:**
- Transforming card that has counters loses/keeps them correctly
- Melded cards separate and regain original state
- Face-down cards can be turned face-up with correct state
- Spell abilities available on each state
- Combat properties change correctly

**Risk:** State transition bugs lead to permanent loss or corruption of game state.

**Priority:** High - Affects core game integrity

---

### Thread Safety Under Load

**Untested area:** Concurrent input handling with high-frequency events and multiple players.

**Files:** InputQueue, InputProxy, InputLockUI concurrent implementation

**What's not tested:**
- Blocking queue behavior under saturation
- Atomic reference contention under rapid updates
- CountDownLatch correctness in long-running games
- Message ordering guarantees
- Memory leaks from accumulated input objects

**Risk:** Deadlocks, dropped inputs, or memory exhaustion under stressful conditions.

**Priority:** Medium - Affects multiplayer stability

---

### Mana Payment Correctness

**Untested area:** Complex mana payment scenarios with multiple source types and color restrictions.

**Files:** `forge-ai/src/main/java/forge/ai/ComputerUtilMana.java` (1,624 lines, extensive DEBUG_MANA_PAYMENT output indicates manual testing)

**What's not tested:**
- Mana source depletion and ordering correctness
- Color requirement satisfaction with complex restrictions
- Replacement effects on mana payment
- Hybrid and phyrexian mana cost combinations
- Mana source ability triggers during payment

**Risk:** Payment failures or silent incorrect payment selection.

**Priority:** High - Affects core gameplay correctness

---

## Deprecation Status

### InputPayMana

**Issue:** Class marked @Deprecated without clear migration path.

**Files:** `forge-gui/src/main/java/forge/player/input/InputPayMana.java`

**Status:** Deprecated but unclear if:
- Replacement exists
- Callers have migrated
- Should be removed or refactored

---

### FView Progress Bar

**Issue:** Comment indicates "Does not use progress bar, due to be deprecated with battlefield refactoring"

**Files:** `forge-gui-desktop/src/main/java/forge/view/FView.java`

**Status:** Battlefield refactoring mentioned but no clear scope or timeline.

---

*Concerns audit: 2026-03-16*

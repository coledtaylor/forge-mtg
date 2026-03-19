package forge.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.web.dto.CardDto;
import forge.web.dto.CombatDto;
import forge.web.dto.GameStateDto;
import forge.web.dto.PlayerDto;
import forge.web.dto.SpellAbilityDto;

public class DtoSerializationTest {

    private ObjectMapper mapper;

    @BeforeMethod
    public void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    public void testCardDtoRoundTrip() throws Exception {
        final CardDto original = new CardDto();
        original.id = 42;
        original.name = "Lightning Bolt";
        original.manaCost = "{R}";
        original.power = 0;
        original.toughness = 0;
        original.colors = Arrays.asList("Red");
        original.ownerId = 1;
        original.controllerId = 1;
        original.zoneType = "Hand";
        original.tapped = false;
        original.counters = new LinkedHashMap<>();
        original.counters.put("P1P1", 2);
        original.attachmentIds = Arrays.asList(10, 11);
        original.type = "Instant";
        original.oracleText = "Lightning Bolt deals 3 damage to any target.";

        final String json = mapper.writeValueAsString(original);
        final CardDto deserialized = mapper.readValue(json, CardDto.class);

        Assert.assertEquals(deserialized.id, original.id);
        Assert.assertEquals(deserialized.name, original.name);
        Assert.assertEquals(deserialized.manaCost, original.manaCost);
        Assert.assertEquals(deserialized.power, original.power);
        Assert.assertEquals(deserialized.toughness, original.toughness);
        Assert.assertEquals(deserialized.colors, original.colors);
        Assert.assertEquals(deserialized.ownerId, original.ownerId);
        Assert.assertEquals(deserialized.controllerId, original.controllerId);
        Assert.assertEquals(deserialized.zoneType, original.zoneType);
        Assert.assertEquals(deserialized.tapped, original.tapped);
        Assert.assertEquals(deserialized.counters, original.counters);
        Assert.assertEquals(deserialized.attachmentIds, original.attachmentIds);
        Assert.assertEquals(deserialized.type, original.type);
        Assert.assertEquals(deserialized.oracleText, original.oracleText);
    }

    @Test
    public void testCardDtoHasNoNestedOwnerObject() throws Exception {
        final CardDto dto = new CardDto();
        dto.id = 1;
        dto.name = "Test Card";
        dto.ownerId = 5;
        dto.controllerId = 5;
        dto.colors = new ArrayList<>();

        final String json = mapper.writeValueAsString(dto);

        // Should NOT contain nested "owner":{ object -- only ownerId as int
        Assert.assertFalse(json.contains("\"owner\":{"), "JSON should not contain nested owner object");
        Assert.assertFalse(json.contains("\"controller\":{"), "JSON should not contain nested controller object");
        Assert.assertTrue(json.contains("\"ownerId\""), "JSON should contain ownerId field");
        Assert.assertTrue(json.contains("\"controllerId\""), "JSON should contain controllerId field");
    }

    @Test
    public void testGameStateDtoNoStackOverflow() throws Exception {
        final GameStateDto gameState = new GameStateDto();
        gameState.players = new ArrayList<>();
        gameState.cards = new ArrayList<>();
        gameState.stack = new ArrayList<>();
        gameState.phase = "Main1";
        gameState.turn = 5;
        gameState.activePlayerId = 1;

        // Create 2 players
        for (int p = 0; p < 2; p++) {
            final PlayerDto player = new PlayerDto();
            player.id = p + 1;
            player.name = "Player " + (p + 1);
            player.life = 20;
            player.poisonCounters = 0;
            player.mana = new LinkedHashMap<>();
            player.mana.put("White", 0);
            player.mana.put("Blue", 0);
            player.mana.put("Black", 0);
            player.mana.put("Red", 3);
            player.mana.put("Green", 0);
            player.mana.put("Colorless", 0);
            player.zones = new LinkedHashMap<>();
            final List<Integer> handIds = new ArrayList<>();
            final List<Integer> battlefieldIds = new ArrayList<>();
            for (int c = 0; c < 7; c++) {
                handIds.add(p * 20 + c);
            }
            for (int c = 7; c < 13; c++) {
                battlefieldIds.add(p * 20 + c);
            }
            player.zones.put("Hand", handIds);
            player.zones.put("Battlefield", battlefieldIds);
            gameState.players.add(player);
        }

        // Create 20 cards (10 per player)
        for (int c = 0; c < 20; c++) {
            final CardDto card = new CardDto();
            card.id = c;
            card.name = "Card " + c;
            card.manaCost = "{2}{R}";
            card.power = 3;
            card.toughness = 3;
            card.colors = Arrays.asList("Red");
            card.ownerId = c < 10 ? 1 : 2;
            card.controllerId = c < 10 ? 1 : 2;
            card.zoneType = c % 20 < 7 ? "Hand" : "Battlefield";
            card.tapped = false;
            card.type = "Creature - Goblin";
            card.oracleText = "Haste";
            gameState.cards.add(card);
        }

        // Add a stack item
        final SpellAbilityDto spell = new SpellAbilityDto();
        spell.id = 100;
        spell.name = "Lightning Bolt";
        spell.description = "Lightning Bolt deals 3 damage to any target.";
        spell.sourceCardId = 0;
        spell.activatingPlayerId = 1;
        gameState.stack.add(spell);

        // Should not throw StackOverflowError
        final String json = mapper.writeValueAsString(gameState);
        Assert.assertNotNull(json);

        // JSON size should be under 100KB for a reasonable game state
        Assert.assertTrue(json.length() < 100 * 1024,
                "JSON size should be under 100KB, was " + json.length() + " bytes");

        // Verify round-trip
        final GameStateDto deserialized = mapper.readValue(json, GameStateDto.class);
        Assert.assertEquals(deserialized.players.size(), 2);
        Assert.assertEquals(deserialized.cards.size(), 20);
        Assert.assertEquals(deserialized.stack.size(), 1);
        Assert.assertEquals(deserialized.phase, "Main1");
        Assert.assertEquals(deserialized.turn, 5);
    }

    @Test
    public void testCombatDtoSerialization() throws Exception {
        final CombatDto combat = new CombatDto();
        combat.attackers = new ArrayList<>();

        final CombatDto.AttackerInfo attacker = new CombatDto.AttackerInfo();
        attacker.cardId = 5;
        attacker.defendingPlayerId = 2;
        attacker.blockerCardIds = Arrays.asList(10, 11);
        combat.attackers.add(attacker);

        final String json = mapper.writeValueAsString(combat);
        final CombatDto deserialized = mapper.readValue(json, CombatDto.class);

        Assert.assertEquals(deserialized.attackers.size(), 1);
        Assert.assertEquals(deserialized.attackers.get(0).cardId, 5);
        Assert.assertEquals(deserialized.attackers.get(0).defendingPlayerId, 2);
        Assert.assertEquals(deserialized.attackers.get(0).blockerCardIds, Arrays.asList(10, 11));
    }
}

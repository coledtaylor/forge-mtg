package forge.web.simulation;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class DeckArchetypeClassifierTest {

    @Test
    public void burnProfileClassifiesAsBurn() {
        final DeckArchetype result = DeckArchetypeClassifier.classifyFromFeatures(
                1.8,   // avgCmc
                0.25,  // creaturePct
                0.60,  // directDamagePct
                0.0,   // counterspellPct
                0.05   // cardDrawPct
        );
        assertEquals(DeckArchetype.BURN, result);
    }

    @Test
    public void aggroProfileClassifiesAsAggro() {
        final DeckArchetype result = DeckArchetypeClassifier.classifyFromFeatures(
                1.7,   // avgCmc
                0.65,  // creaturePct
                0.05,  // directDamagePct
                0.0,   // counterspellPct
                0.0    // cardDrawPct
        );
        assertEquals(DeckArchetype.AGGRO, result);
    }

    @Test
    public void controlProfileClassifiesAsControl() {
        final DeckArchetype result = DeckArchetypeClassifier.classifyFromFeatures(
                3.2,   // avgCmc
                0.20,  // creaturePct
                0.0,   // directDamagePct
                0.20,  // counterspellPct
                0.20   // cardDrawPct
        );
        assertEquals(DeckArchetype.CONTROL, result);
    }

    @Test
    public void midrangeProfileClassifiesAsMidrange() {
        final DeckArchetype result = DeckArchetypeClassifier.classifyFromFeatures(
                3.0,   // avgCmc
                0.45,  // creaturePct
                0.05,  // directDamagePct
                0.05,  // counterspellPct
                0.10   // cardDrawPct
        );
        assertEquals(DeckArchetype.MIDRANGE, result);
    }
}

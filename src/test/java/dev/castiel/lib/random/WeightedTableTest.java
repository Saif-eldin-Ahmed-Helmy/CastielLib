package dev.castiel.lib.random;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WeightedTableTest {
    @Test
    void rejectsNonFiniteWeights() {
        WeightedTable<String> table = new WeightedTable<String>(new Random(1));
        table.add("infinite", Double.POSITIVE_INFINITY)
                .add("nan", Double.NaN)
                .add("valid", 2);

        assertEquals(2, table.totalWeight());
        assertEquals("valid", table.roll());
    }

    @Test
    void nullWeightedEntriesAreIgnored() {
        WeightedTable<Weighted> table = WeightedTable.fromWeighted(Arrays.asList(null, () -> 1));
        assertEquals(1, table.totalWeight());
        assertEquals(1, table.entries().size());
    }

    @Test
    void emptyTableReturnsNull() {
        assertNull(new WeightedTable<String>().roll());
    }
}

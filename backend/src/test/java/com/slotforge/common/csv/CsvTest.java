package com.slotforge.common.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.slotforge.common.exception.ApiException;
import org.junit.jupiter.api.Test;

class CsvTest {

    private static final List<String> COLUMNS = List.of("roll_no", "full_name");

    @Test
    void readsQuotedValuesWithCommasQuotesAndLineBreaks() {
        List<Csv.Row> rows = Csv.parse("roll_no,full_name\r\n25MCA001,\"Rao, Asha \"\"AJ\"\"\"\r\n25MCA002,\"Two\nlines\"\r\n", COLUMNS);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("full_name")).isEqualTo("Rao, Asha \"AJ\"");
        assertThat(rows.get(1).get("full_name")).isEqualTo("Two\nlines");
    }

    @Test
    void forgivesTheMarkExcelAddsHeaderSpellingAndBlankLines() {
        List<Csv.Row> rows = Csv.parse("\uFEFFRoll No , Full-Name\n\n 25MCA001 , Asha \n,\n", COLUMNS);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("roll_no")).isEqualTo("25MCA001");
            assertThat(row.get("full_name")).isEqualTo("Asha");
            assertThat(row.line()).as("the line a person would look for in the file").isEqualTo(3);
        });
    }

    @Test
    void aShortRowReadsAsEmptyValuesNotAnError() {
        assertThat(Csv.parse("roll_no,full_name\n25MCA001\n", COLUMNS).get(0).get("full_name")).isEmpty();
    }

    @Test
    void saysWhatIsWrongWithAFileItCannotUse() {
        assertThatThrownBy(() -> Csv.parse("", COLUMNS)).isInstanceOf(ApiException.class).hasMessageContaining("empty");
        assertThatThrownBy(() -> Csv.parse("roll_no,name\nA,B\n", COLUMNS)).hasMessageContaining("Missing: full_name");
        assertThatThrownBy(() -> Csv.parse("roll_no,full_name\n", COLUMNS)).hasMessageContaining("no data rows");
        assertThatThrownBy(() -> Csv.parse("roll_no,full_name\nA,\"never closed\n", COLUMNS)).hasMessageContaining("line 2");
    }
}

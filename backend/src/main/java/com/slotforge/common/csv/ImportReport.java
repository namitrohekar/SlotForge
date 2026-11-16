package com.slotforge.common.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of an import. An import is all or nothing: when errors is not empty, nothing was saved and imported is 0.
 * Only the first few errors are listed, and errorCount says how many there were.
 */
public record ImportReport(int rows, int imported, int errorCount, List<RowError> errors) {

    public record RowError(int line, String message) {
    }

    private static final int ERRORS_SHOWN = 50;

    public static ImportReport rejected(int rows, List<RowError> errors) {
        return new ImportReport(rows, 0, errors.size(), new ArrayList<>(errors.subList(0, Math.min(ERRORS_SHOWN, errors.size()))));
    }

    public static ImportReport accepted(int rows, int imported) {
        return new ImportReport(rows, imported, 0, List.of());
    }
}

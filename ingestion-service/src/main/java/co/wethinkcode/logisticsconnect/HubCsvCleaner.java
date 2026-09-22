package co.wethinkcode.logisticsconnect;

import java.util.Set;

public class HubCsvCleaner {

    // Values that mean "we don't actually have this piece of data", however the
    // export happened to spell that. Compared in lower case after trimming.
    private static final Set<String> PLACEHOLDER_VALUES = Set.of(
            "", "n/a", "na", "tbd", "unknown", "-", "nan"
    );
}

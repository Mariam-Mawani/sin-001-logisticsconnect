package co.wethinkcode.logisticsconnect;

/**
 * A single cleaned hub record - one row of hubs-global.csv after we've fixed up
 * casing, padding, province spelling, and the active flag.
 */
public class Hub {

    public String hubId;
    public String province;
    public String sortingCenter;
    // Boolean (capital B), not boolean, so it can be null. A null here means
    // "we genuinely don't know" (e.g. every duplicate row for this hub had a
    // placeholder value like N/A) rather than silently guessing false.
    public Boolean active;
}


package co.wethinkcode.logisticsconnect;

/**
 * A single cleaned hub record - one row of hubs-global.csv after we've fixed up
 * casing, padding, province spelling, and the active flag.
 */
public class Hub {

    public String hubId;
    public String province;
    public String sortingCenter;

}


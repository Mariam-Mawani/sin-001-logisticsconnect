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

    // Jackson needs a no-argument constructor to build objects when reading JSON
    // back in (e.g. inside hub-service, which receives Hub objects over REST).
    public Hub() {
    }

    public Hub(String hubId, String province, String sortingCenter, Boolean active) {
        this.hubId = hubId;
        this.province = province;
        this.sortingCenter = sortingCenter;
        this.active = active;
    }
}


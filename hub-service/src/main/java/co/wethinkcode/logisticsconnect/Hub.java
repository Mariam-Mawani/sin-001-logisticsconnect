package co.wethinkcode.logisticsconnect;

/**
 * Same shape as ingestion-service's Hub class. It's duplicated rather than
 * shared because every service in this repo is an independent Maven project
 * with no parent pom - see MqConfig for the same pattern with the MQ services.
 */
public class Hub {

    public String hubId;
    public String province;
    public String sortingCenter;
    public Boolean active;


    // Needed so Jackson can build one of these while reading the JSON array
    // that comes back from ingestion-service's GET /hubs.
    public Hub() {
    }

    public Hub(String hubId, String province, String sortingCenter, Boolean active) {
        this.hubId = hubId;
        this.province = province;
        this.sortingCenter = sortingCenter;
        this.active = active;
    }
}

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

}

package model;

import java.util.Date;

/**
 * Created by Shunjie Ding on 31/12/2017.
 */
public final class PostRoute {
    public enum RouteType {
        // FIXME There must be more types
        ACCEPT,
        TRANSPORT,
        ARRIVAL,
        DELIVERY,
        RECEIPT,
        UNRECOGNIZED
    }

    private RouteType type;

    private Date time;

    private String status;

    protected PostRoute() {}

    public RouteType getType() {
        return type;
    }

    public Date getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }
}

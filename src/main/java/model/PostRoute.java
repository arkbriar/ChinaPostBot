package model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 * Created by Shunjie Ding on 31/12/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PostRoute {
    public enum RouteType {
        // FIXME There must be more types
        @JsonProperty("SJ") ACCEPT,
        @JsonProperty("FY") TRANSPORT,
        @JsonProperty("DD") ARRIVAL,
        @JsonProperty("APTD") DELIVERY,
        @JsonProperty("TT") RECEIPT,
        @JsonEnumDefaultValue UNRECOGNIZED
    }

    @JsonProperty("V_HJDM")
    private RouteType type;

    @JsonProperty("D_SJSJ")
    private Date time;

    @JsonProperty("V_ZT")
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

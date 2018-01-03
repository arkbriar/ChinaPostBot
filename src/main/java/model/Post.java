package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Shunjie Ding on 31/12/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Post {
    @JsonProperty("V_YJHM")
    private String id;

    @JsonProperty("D_SJSJ")
    private Date receiveTime;

    @JsonProperty("V_JDJMC")
    private String nowLocation;

    @JsonProperty("V_ZT")
    private String status;

    @JsonProperty("GJ_FLAG")
    private String paramGJBZ;

    @JsonProperty("N_TDBZ")
    private String paramTDBZ;

    protected Post() {}

    public String getId() {
        return id;
    }

    public Date getReceiveTime() {
        return receiveTime;
    }

    public String getNowLocation() {
        return nowLocation;
    }

    public String getStatus() {
        return status;
    }

    public List<NameValuePair> paramsForQuery() {
        List<NameValuePair> params = new ArrayList<>(4);
        params.add(new BasicNameValuePair("vYjhm", id));
        params.add(new BasicNameValuePair("vTdbz", paramTDBZ));
        params.add(new BasicNameValuePair("vGjbz", paramGJBZ));
        params.add(new BasicNameValuePair("cgsyj", "null"));
        return params;
    }
}

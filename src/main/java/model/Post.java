package model;

import java.util.Date;

/**
 * Created by Shunjie Ding on 31/12/2017.
 */
public final class Post {
    private String id;

    private Date sendTime;

    private Date receiveTime;

    private String nowLocation;

    private String status;

    protected Post() {}

    public String getId() {
        return id;
    }

    public Date getSendTime() {
        return sendTime;
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
}

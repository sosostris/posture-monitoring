package com.example.xujia.posturemonitor.common;

public class MySensornode {

    private String mAddress;
    private String mName;
    private String mType;
    private String mBody;

    public MySensornode(String address, String name, String type, String body) {
        mAddress = address;
        mName = name;
        mType = type;
        mBody = body;
    }

    public String getAddress() {
        return mAddress;
    }

    public String getName() {
        return mName;
    }

    public String getType() {
        return mType;
    }

    public String getBody() {
        return mBody;
    }

    public void setBody(String newBody) {
        mBody = newBody;
    }

}

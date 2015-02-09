package model;

import java.sql.Timestamp;

/**
 * Created by hsanchez on 1/6/2015.
 */
public class AppendJobDetail {
    private int AppendJobDetailID;
    private int AppendJobID;
    private int BusinessID;
    private int CustomerID;
    private int FirstAppend;
    private String MatchLevelRequest;
    private String MatchLevelResult;
    private String AppendedEmail;
    private Timestamp LastModifiedDate;

    public int getAppendJobDetailID() {
        return AppendJobDetailID;
    }

    public void setAppendJobDetailID(int appendJobDetailID) {
        AppendJobDetailID = appendJobDetailID;
    }

    public int getAppendJobID() {
        return AppendJobID;
    }

    public void setAppendJobID(int appendJobID) {
        AppendJobID = appendJobID;
    }

    public int getBusinessID() {
        return BusinessID;
    }

    public void setBusinessID(int businessID) {
        BusinessID = businessID;
    }

    public int getCustomerID() {
        return CustomerID;
    }

    public void setCustomerID(int customerID) {
        CustomerID = customerID;
    }

    public int getFirstAppend() {
        return FirstAppend;
    }

    public void setFirstAppend(int firstAppend) {
        FirstAppend = firstAppend;
    }

    public String getMatchLevelRequest() {
        return MatchLevelRequest;
    }

    public void setMatchLevelRequest(String matchLevelRequest) {
        MatchLevelRequest = matchLevelRequest;
    }

    public String getMatchLevelResult() {
        return MatchLevelResult;
    }

    public void setMatchLevelResult(String matchLevelResult) {
        MatchLevelResult = matchLevelResult;
    }

    public String getAppendedEmail() {
        return AppendedEmail;
    }

    public void setAppendedEmail(String appendedEmail) {
        AppendedEmail = appendedEmail;
    }

    public Timestamp getLastModifiedDate() {
        return LastModifiedDate;
    }

    public void setLastModifiedDate(Timestamp lastModifiedDate) {
        LastModifiedDate = lastModifiedDate;
    }
}

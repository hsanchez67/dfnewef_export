package model;

import java.io.Serializable;
import java.sql.Timestamp;


public class ClientJobDetail implements Serializable {
	//default serial version id, required for serializable classes.
	private static final long serialVersionUID = 1L;

	private Integer id;
	private Integer clientJobid;
	private Integer consumerid;
	private String appendTypes = "EMAIL";
	private String fname = "";
	private String lname = "";
	private String address1 = "";
	private String city = "";
	private String state = "";
	private String zip = "";
	private String precisionReq = "I";
	private Integer vendorJobid;
	private String precisionRes;
	private String appendedEmail;
	private Timestamp createdTs;
	private Timestamp submitTs;
	private Timestamp receiveTs;
	private Timestamp downloadTs;
	private Timestamp lastModifiedTs;

	private ClientJob clientJob;
	public ClientJob getClientJob() { return this.clientJob; }
	public void setClientJob(ClientJob clientJob) { this.clientJob = clientJob; }
	
	public ClientJobDetail() { }
	public ClientJobDetail(Integer clientJobid) {
		this.clientJobid = clientJobid;
	}
	public ClientJobDetail(Integer clientJobid, Integer consumerid) {
		this.clientJobid = clientJobid;
		this.consumerid = consumerid;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getClientJobid() {
		return clientJobid;
	}
	public void setClientJobid(Integer clientJobid) {
		this.clientJobid = clientJobid;
	}
	public Integer getConsumerid() {
		return consumerid;
	}
	public void setConsumerid(Integer consumerid) {
		this.consumerid = consumerid;
	}
	public String getAppendTypes() {
		return appendTypes;
	}
	public void setAppendTypes(String appendTypes) {
		this.appendTypes = appendTypes;
	}
	public String getFname() {
		return fname;
	}
	public void setFname(String fname) {
		this.fname = fname;
	}
	public String getLname() {
		return lname;
	}
	public void setLname(String lname) {
		this.lname = lname;
	}
	public String getAddress1() {
		return address1;
	}
	public void setAddress1(String address1) {
		this.address1 = address1;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getZip() {
		return zip;
	}
	public void setZip(String zip) {
		this.zip = zip;
	}
	public String getPrecisionReq() {
		return precisionReq;
	}
	public void setPrecisionReq(String precisionReq) {
		this.precisionReq = precisionReq;
	}
	public Integer getVendorJobid() {
		return vendorJobid;
	}
	public void setVendorJobid(Integer vendorJobid) {
		this.vendorJobid = vendorJobid;
	}
	public String getPrecisionRes() {
		return precisionRes;
	}
	public void setPrecisionRes(String precisionRes) {
		this.precisionRes = precisionRes;
	}
	public String getAppendedEmail() {
		return appendedEmail;
	}
	public void setAppendedEmail(String appendedEmail) {
		this.appendedEmail = appendedEmail;
	}
	public Timestamp getCreatedTs() {
		return createdTs;
	}
	public void setCreatedTs(Timestamp createdTs) {
		this.createdTs = createdTs;
	}
	public Timestamp getSubmitTs() {
		return submitTs;
	}
	public void setSubmitTs(Timestamp submitTs) {
		this.submitTs = submitTs;
	}
	public Timestamp getReceiveTs() {
		return receiveTs;
	}
	public void setReceiveTs(Timestamp receiveTs) {
		this.receiveTs = receiveTs;
	}
	public Timestamp getDownloadTs() {
		return downloadTs;
	}
	public void setDownloadTs(Timestamp downloadTs) {
		this.downloadTs = downloadTs;
	}
	public Timestamp getLastModifiedTs() {
		return lastModifiedTs;
	}
	public void setLastModifiedTs(Timestamp lastModifiedTs) {
		this.lastModifiedTs = lastModifiedTs;
	}
	
	@Override
	public String toString() {
		return "ClientJobDetail [id=" + id
				+ ", clientJobid=" + clientJobid + ", consumerid=" + consumerid
				+ ", appendTypes=" + appendTypes + ", fname=" + fname
				+ ", lname=" + lname + ", address1=" + address1 + ", city="
				+ city + ", state=" + state + ", zip=" + zip
				+ ", precisionReq=" + precisionReq + ", vendorJobid="
				+ vendorJobid + ", precisionRes=" + precisionRes
				+ ", appendedEmail=" + appendedEmail + ", createdTs=" + createdTs
				+ ", submitTs=" + submitTs + ", receiveTs=" + receiveTs
				+ ", downloadTs=" + downloadTs + ", lastModifiedTs="
				+ lastModifiedTs + "]";
	}
}

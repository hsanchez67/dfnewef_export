package model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.sql.Timestamp;


public class ClientJob implements Serializable {
	//default serial version id, required for serializable classes.
	private static final long serialVersionUID = 1L;


	private Integer id;
	private Integer clientid;
	private Integer businessid;
	private Timestamp createdTs;
	private Timestamp submitTs;
	private Timestamp lastModifiedTs;
	private Integer numRequested;
	private Integer numMatched;
	private Timestamp appendCompleteTs;


	private Client client;
	public Client getClient() { return this.client; }
	public void setClient(Client client) { this.client = client; }
	
	public ClientJob() {}
	public ClientJob(Integer clientid, Integer businessid) {
		this.clientid = clientid;
		this.businessid = businessid;
	}
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getClientid() {
		return clientid;
	}
	public void setClientid(Integer clientid) {
		this.clientid = clientid;
	}
	public Integer getBusinessid() {
		return businessid;
	}
	public void setBusinessid(Integer businessid) {
		this.businessid = businessid;
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
	public Timestamp getLastModifiedTs() {
		return lastModifiedTs;
	}
	public void setLastModifiedTs(Timestamp lastModifiedTs) {
		this.lastModifiedTs = lastModifiedTs;
	}
	public Integer getNumRequested() {
		return numRequested;
	}
	public void setNumRequested(Integer numRequested) {
		this.numRequested = numRequested;
	}
	public Integer getNumMatched() {
		return numMatched;
	}
	public void setNumMatched(Integer numMatched) {
		this.numMatched = numMatched;
	}
	public Timestamp getAppendCompleteTs() {
		return appendCompleteTs;
	}
	public void setAppendCompleteTs(Timestamp appendCompleteTs) {
		this.appendCompleteTs = appendCompleteTs;
	}
	@JsonIgnore
	public boolean isSubmitted() {
		return this.submitTs != null;
	}
	
	@Override
	public String toString() {
		return "ClientJob [clientJobid=" + id + ", clientid="
				+ clientid + ", businessid=" + businessid + ", createdTs="
				+ createdTs + ", submitTs=" + submitTs + ", lastModifiedTs="
				+ lastModifiedTs + ", numRequested=" + numRequested
				+ ", numMatched=" + numMatched + ", appendCompleteTs="
				+ appendCompleteTs + "]";
	}
	
}


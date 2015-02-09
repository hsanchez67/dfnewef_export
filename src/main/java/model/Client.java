package model;

import java.io.Serializable;

public class Client implements Serializable {
	//default serial version id, required for serializable classes.
	private static final long serialVersionUID = 1L;

	private Integer id;
	private String name;


	public Client() {}
	public Client(String name) {
		this.name = name;
	}


	public Integer getId() {
		return id;
	}

	public void setId(Integer clientid) {
		this.id = clientid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Client [id=" + id + ", name=" + name + "]";
	}
}

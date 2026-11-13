package com.rayful.bulk.vo.df;

public class ConnectInfoVO {
	private final String rdbms;
	private final String driverClass;
	private final String url;
	private final String username;
	private final String password;
	
	public ConnectInfoVO(String rdbms, String driverClass, String url, String username, String password) {
		if(rdbms == null) {
			throw new NullPointerException("Must be dbms != null of connect_info tag in DF");
		}
		
		if(!"notes".equals(rdbms) && driverClass == null) {
			throw new NullPointerException("Must be driverClass != null of connect_info tag in DF");
		}
		if(url == null) {
			throw new NullPointerException("Must be url != null of connect_info tag in DF");
		}
		if(username == null) {
			throw new NullPointerException("Must be username != null of connect_info tag in DF");
		}
		if(password == null) {
			throw new NullPointerException("Must be password != null of connect_info tag in DF");
		}
		
		this.rdbms = rdbms;
		this.driverClass = driverClass;
		this.url = url;
		this.username = username;
		this.password = password;
	}

	public String getRdbms() {
		return rdbms;
	}

	public String getDriverClass() {
		return driverClass;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		return "ConnectInfoVO [rdbms=" + rdbms + ", driverClass=" + driverClass + ", url=" + url + ", username="
				+ username + ", password=****]";
	}
}

package com.rayful.bulk.vo.df;

public class FtpInfoVO {
	private final String url;
	private final int port;
	private final String username;
	private final String password;
	
	public FtpInfoVO(String url, String port, String username, String password) {
		if(url == null) {
			throw new NullPointerException("url == null");
		}
		if(port == null) {
			throw new NullPointerException("port == null");
		}
		if(username == null) {
			throw new NullPointerException("username == null");
		}
		if(password == null) {
			throw new NullPointerException("password == null");
		}
		
		this.url = url;
		this.port = Integer.parseInt(port);
		this.username = username;
		this.password = password;
	}

	public String getUrl() {
		return url;
	}

	public int getPort() {
		return port;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		return "FtpInfoVO [url=" + url + ", port=" + port + ", username=" + username + ", password=****]";
	}
}

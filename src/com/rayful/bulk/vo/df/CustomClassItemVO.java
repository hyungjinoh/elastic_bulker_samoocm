package com.rayful.bulk.vo.df;

public class CustomClassItemVO {
	private final String id;
	private final String className;
	private final boolean use;
	private final boolean async;
	private final int nThread;
	private final int nThreadCopy;
	
	public CustomClassItemVO(String id, String className, boolean use) {
		if(id == null || id.trim().length() == 0) {
			throw new IllegalArgumentException("Rquired id attribute of custom_class tag in DF");
		}
		if(className == null || className.trim().length() == 0) {
			throw new IllegalArgumentException("Rquired className attribute of custom_class tag in DF: " + id);
		}
		
		this.id = id;
		this.className = className;
		this.use = use;
		this.async = false;
		this.nThread = 1;
		this.nThreadCopy = 1;
	}
	
	public CustomClassItemVO(String id, String className, boolean use, int nThread, int nThreadCopy) {
		if(id == null || id.trim().length() == 0) {
			throw new IllegalArgumentException("Rquired id attribute of custom_class tag in DF");
		}
		if(className == null || className.trim().length() == 0) {
			throw new IllegalArgumentException("Rquired className attribute of custom_class tag in DF: " + id);
		}
		
		this.id = id;
		this.className = className;
		this.use = use;
		this.async = false;
		this.nThread = nThread;
		this.nThreadCopy = nThreadCopy;
	}
	
	public CustomClassItemVO(String id, String className, boolean use, boolean async) {
		if(id == null || id.trim().length() == 0) {
			throw new IllegalArgumentException("Rquired id attribute of custom_class tag in DF");
		}
		if(className == null || className.trim().length() == 0) {
			throw new IllegalArgumentException("Rquired className attribute of custom_class tag in DF: " + id);
		}
		
		this.id = id;
		this.className = className;
		this.use = use;
		this.async = async;
		this.nThread = 1;
		this.nThreadCopy = 1;
	}
	
	public CustomClassItemVO(String id, String className, boolean use, Object storageInfo) {
		this.id = id;
		this.className = className;
		this.use = use;
		this.async = false;
		this.nThread = 1;
		this.nThreadCopy = 1;
	}

	public String getId() {
		return id;
	}

	public String getClassName() {
		return className;
	}

	public boolean isUse() {
		return use;
	}

	public boolean isAsync() {
		return async;
	}

	public int getnThread() {
		return nThread;
	}

	public int getnThreadCopy() {
		return nThreadCopy;
	}

	@Override
	public String toString() {
		return "CustomClassItemVO [id=" + id + ", className=" + className + ", use=" + use + ", async=" + async
				+ ", nThread=" + nThread + ", nThreadCopy=" + nThreadCopy + "]";
	}
}

package com.fogofwar;
public enum EntityExclusionLimit {
	NONE("None", 0),
	LIMIT_16("16", 16),
	LIMIT_32("32", 32),
	LIMIT_64("64", 64),
	LIMIT_128("128", 128),
	ALL("All", Integer.MAX_VALUE);
	private final String name;
	private final int limit;
	EntityExclusionLimit(String name, int limit) {
		this.name = name;
		this.limit = limit;
	}
	public int getLimit() { return limit; }
	public boolean isEnabled() { return limit > 0; }
	@Override
	public String toString() { return name; }
}

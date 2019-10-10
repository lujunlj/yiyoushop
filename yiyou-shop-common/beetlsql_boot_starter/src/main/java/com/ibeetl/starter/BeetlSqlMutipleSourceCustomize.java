package com.ibeetl.starter;

import org.beetl.sql.core.SQLManager;

public interface  BeetlSqlMutipleSourceCustomize {
	public void customize(String dataSource ,SQLManager manager);
}

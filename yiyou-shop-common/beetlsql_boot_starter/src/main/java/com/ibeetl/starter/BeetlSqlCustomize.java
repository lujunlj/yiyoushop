package com.ibeetl.starter;

import org.beetl.sql.ext.spring4.SqlManagerFactoryBean;

public interface  BeetlSqlCustomize {
	public void customize(SqlManagerFactoryBean sqlManager);
}

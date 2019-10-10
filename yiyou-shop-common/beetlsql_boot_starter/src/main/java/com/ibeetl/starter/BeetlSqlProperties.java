package com.ibeetl.starter;

import org.springframework.core.env.Environment;

public class BeetlSqlProperties {
	String basePackage = null;// 配置beetlsql.daoSuffix来自动扫描com包极其子包下的所有以Dao结尾的Mapper类
	String daoSuffix = null;// 通过类后缀 来自动注入Dao
	String sqlPath = null;// 存放sql文件的根目录

	String nameConversion = null;// 数据库和javapojo的映射关系

	String dbStyle = null; // 何种数据库

	Boolean dev = true;// 是否输出debug
	Integer logicDeleteValue = 1;//逻辑删除标志位

	public BeetlSqlProperties(Environment env) {
		basePackage = env.getProperty("beetlsql.basePackage", "com");
		daoSuffix = env.getProperty("beetlsql.daoSuffix", "Dao");
		sqlPath = env.getProperty("beetlsql.sqlPath", "/sql");
		nameConversion = env.getProperty("beetlsql.nameConversion", "org.beetl.sql.core.UnderlinedNameConversion");
		dbStyle = env.getProperty("beetlsql.dbStyle", "org.beetl.sql.core.db.MySqlStyle");
		logicDeleteValue = env.getProperty("beetlsql.logicDeleteValue",Integer.class,1);

		dev = env.getProperty("beetl-beetlsql.dev", Boolean.class, true);

	}

	/**
	 * 数据源配置
	 * @param env
	 * @param datasourceName
	 */
	public BeetlSqlProperties(Environment env,String datasourceName) {
	    //提供统一的配置
	    this(env);
	    
		basePackage = env.getProperty("beetlsql.ds."+datasourceName+".basePackage", basePackage);
		daoSuffix = env.getProperty("beetlsql.ds."+datasourceName+".daoSuffix", daoSuffix);
		sqlPath = env.getProperty("beetlsql.ds."+datasourceName+".sqlPath", sqlPath);
		nameConversion = env.getProperty("beetlsql.ds."+datasourceName+".nameConversion", nameConversion);
		dbStyle = env.getProperty("beetlsql.ds."+datasourceName+".dbStyle", dbStyle);
		logicDeleteValue = env.getProperty("beetlsql.ds."+datasourceName+".logicDeleteValue",Integer.class,1);

	}
	public String getBasePackage() {
		return basePackage;
	}

	public void setBasePackage(String basePackage) {
		this.basePackage = basePackage;
	}

	public String getDaoSuffix() {
		return daoSuffix;
	}

	public void setDaoSuffix(String daoSuffix) {
		this.daoSuffix = daoSuffix;
	}

	public String getSqlPath() {
		return sqlPath;
	}

	public void setSqlPath(String sqlPath) {
		this.sqlPath = sqlPath;
	}

	public String getNameConversion() {
		return nameConversion;
	}

	public void setNameConversion(String nameConversion) {
		this.nameConversion = nameConversion;
	}

	public String getDbStyle() {
		return dbStyle;
	}

	public void setDbStyle(String dbStyle) {
		this.dbStyle = dbStyle;
	}

	public Boolean getDev() {
		return dev;
	}

	public void setDev(Boolean dev) {
		this.dev = dev;
	}

	public Integer getLogicDeleteValue() {
		return logicDeleteValue;
	}

	public void setLogicDeleteValue(Integer logicDeleteValue) {
		this.logicDeleteValue = logicDeleteValue;
	}
}

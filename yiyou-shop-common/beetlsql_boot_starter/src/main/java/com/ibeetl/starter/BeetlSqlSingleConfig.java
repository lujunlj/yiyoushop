package com.ibeetl.starter;

import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;


import org.beetl.core.fun.ObjectUtil;
import org.beetl.sql.core.ClasspathLoader;
import org.beetl.sql.core.Interceptor;
import org.beetl.sql.core.NameConversion;
import org.beetl.sql.core.db.DBStyle;
import org.beetl.sql.ext.DebugInterceptor;
import org.beetl.sql.ext.spring4.BeetlSqlDataSource;
import org.beetl.sql.ext.spring4.SqlManagerFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnBean(DataSource.class)
@Conditional(BeetlSqlSingleCondition.class)
@Import(BeetlMapperScannerRegister.class)
public class BeetlSqlSingleConfig {
	
	@Autowired(required=false)
	BeetlSqlCustomize cust;
	
	
	
	@Autowired(required=false)
	ApplicationContext context;
	
	
	
	
	@Bean(name = "sqlManagerFactoryBean")
	@ConditionalOnMissingBean(SqlManagerFactoryBean.class)
	public SqlManagerFactoryBean getSqlManagerFactoryBean(BeetlSqlDataSource source,Environment env ) throws Exception {
		
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if(classLoader==null) {
			classLoader = this.getClass().getClassLoader();
		}
		
		BeetlSqlProperties beetlSqlProperties = new BeetlSqlProperties(env);
		SqlManagerFactoryBean factory = new SqlManagerFactoryBean();
		
		factory.setCs(source);
		factory.setDbStyle((DBStyle) ObjectUtil.tryInstance(beetlSqlProperties.getDbStyle(),classLoader));
		factory.setInterceptors(beetlSqlProperties.dev ? new Interceptor[] { new DebugInterceptor() } : new Interceptor[0]);
		factory.setNc((NameConversion)ObjectUtil.tryInstance(beetlSqlProperties.getNameConversion(),classLoader));
		factory.setLogicDeleteValue(beetlSqlProperties.logicDeleteValue);//逻辑删除
		ClasspathLoader loader = new ClasspathLoader(beetlSqlProperties.getSqlPath());
		//不能直接设置通过loader的autocheck
		Properties ps = new Properties();
		ps.put("PRODUCT_MODE", beetlSqlProperties.dev?"false":"true");
		factory.setExtProperties(ps);
		factory.setSqlLoader(loader);
		
		if(cust!=null) {
			cust.customize(factory);
		}
		return factory;
	}
	
	@Bean
	@ConditionalOnMissingBean(BeetlSqlDataSource.class)
	public BeetlSqlDataSource beetlSqlDataSource(DataSource dataSources){
		BeetlSqlDataSource source = new BeetlSqlDataSource();
		source.setMasterSource(dataSources);
		return source;
	}
	
	

}

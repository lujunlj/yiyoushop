package com.ibeetl.starter;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.beetl.sql.core.SQLManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnBean(DataSource.class)
@Conditional(BeetlSqlMutipleCondition.class)
@Import(BeetlMapperMutipleScannerRegister.class)
public class BeetlSqlMutipleDataSourceConfig {
	
	@Autowired(required=false)
	BeetlSqlMutipleSourceCustomize cust;
	
	@Autowired()
	ApplicationContext context;
	
	@Autowired()
	Environment env;
	
	
	@PostConstruct
	public void init() {
		if(cust==null) {
			return ;
		}
		String sourceConfig = env.getProperty("beetlsql.mutiple.datasource");
		String[] sources = sourceConfig.split(",");
		for(String dbSource:sources) {
			String masterSource = dbSource;
			SQLManager sfb = (SQLManager)context.getBean(masterSource+"SqlManagerFactoryBean");
			cust.customize(dbSource,sfb);
		}
	}
	
	
//	@Bean(name = "mutipleSqlManager")
//	public MutipleSqlManager getSqlManagerFactoryBean(Environment env ) throws Exception {
//		
//		
//		
//		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//		if(classLoader==null) {
//			classLoader = this.getClass().getClassLoader();
//		}
//		MutipleSqlManager mutipleSqlManager = new MutipleSqlManager();
//		String sourceConfig = env.getProperty("beetlsql.mutiple.datasource");
//		String[] sources = sourceConfig.split(",");
//		for(String dbSource:sources) {
//			String masterSource = dbSource;
//			BeetlSqlDataSource sqlDataSource = new BeetlSqlDataSource();
//			sqlDataSource.setMasterSource((DataSource)context.getBean(masterSource));
//			BeetlSqlProperties beetlSqlProperties = new BeetlSqlProperties(env,masterSource);
//////			SqlManagerFactoryBean factory = new SqlManagerFactoryBean();
//////			factory.setCs(source);
//////			factory.setDbStyle((DBStyle)ObjectUtil.tryInstance(beetlSqlProperties.getDbStyle(),classLoader));
//////			factory.setInterceptors(beetlSqlProperties.dev ? new Interceptor[] { new DebugInterceptor() } : new Interceptor[0]);
//////			factory.setNc((NameConversion)ObjectUtil.tryInstance(beetlSqlProperties.getNameConversion(),classLoader));
//			ClasspathLoader loader = new ClasspathLoader(beetlSqlProperties.getSqlPath());
//////			//不能直接设置通过loader的autocheck
//			Properties ps = new Properties();
//			ps.put("PRODUCT_MODE", beetlSqlProperties.dev?"false":"true");
//////			factory.setExtProperties(ps);
//////			factory.setSqlLoader(loader);
////			
////			if(cust!=null) {
////				cust.customize(factory);
////			}
//			 mutipleSqlManager.addSqlmanager(masterSource, null);
//			
//			 BeanDefinitionBuilder bdb = BeanDefinitionBuilder.rootBeanDefinition(SqlManagerFactoryBean.class);
//			 bdb.addPropertyValue("cs", sqlDataSource);
//			 bdb.addPropertyValue("dbStyle", (DBStyle)ObjectUtil.tryInstance(beetlSqlProperties.getDbStyle(),classLoader));
//			 bdb.addPropertyValue("interceptors", beetlSqlProperties.dev ? new Interceptor[] { new DebugInterceptor() } : new Interceptor[0]);
//			 bdb.addPropertyValue("sqlLoader", loader);
//			 bdb.addPropertyValue("extProperties", ps);
////			 bdb.getBeanDefinition().set
//			 ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) context;  
//			 DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext  
//				        .getBeanFactory();  
//			 defaultListableBeanFactory.registerBeanDefinition(masterSource+"SqlManagerFactoryBean", bdb.getBeanDefinition());
//			
//		}
//		
//		return mutipleSqlManager;
//	}
//	
	
	
	

}

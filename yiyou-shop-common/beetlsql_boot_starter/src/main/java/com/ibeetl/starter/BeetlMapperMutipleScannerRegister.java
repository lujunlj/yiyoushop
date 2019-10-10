package com.ibeetl.starter;

import java.util.Properties;


import org.beetl.core.fun.ObjectUtil;
import org.beetl.sql.core.ClasspathLoader;
import org.beetl.sql.core.Interceptor;
import org.beetl.sql.core.NameConversion;
import org.beetl.sql.core.db.DBStyle;
import org.beetl.sql.ext.DebugInterceptor;
import org.beetl.sql.ext.spring4.BeetlSqlDataSource;
import org.beetl.sql.ext.spring4.SqlManagerFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

public class BeetlMapperMutipleScannerRegister
		implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

	private ResourceLoader resourceLoader;
	Environment env;


	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		readyBeetlSqlSource(registry);
		this.readySqlManager(registry);
		
		BeetlSqlStarterClassPathScanner scanner = new BeetlSqlStarterClassPathScanner(registry);

		// this check is needed in Spring 3.1

		if (resourceLoader != null) {
			scanner.setResourceLoader(resourceLoader);
		}

		String sourceConfig = env.getProperty("beetlsql.mutiple.datasource");
		String[] sources = sourceConfig.split(",");
		for (String dbSource : sources) {
			String masterSource = dbSource;
			BeetlSqlProperties ps = new BeetlSqlProperties(env, masterSource);
			scanner.setSqlManagerFactoryBeanName(masterSource + "SqlManagerFactoryBean");
			scanner.setSuffix(ps.getDaoSuffix());
			scanner.registerFilters();
			scanner.doScan(ps.getBasePackage().split(","));
		}

	}
	
	protected void readyBeetlSqlSource(BeanDefinitionRegistry registry) {
		String sourceConfig = env.getProperty("beetlsql.mutiple.datasource");
		String[] sources = sourceConfig.split(",");
		for(String dbSource:sources) {
			String masterSource = dbSource;
			
			 BeanDefinitionBuilder bdb = BeanDefinitionBuilder.rootBeanDefinition(BeetlSqlDataSource.class);
			 bdb.addPropertyValue("masterSource", new RuntimeBeanReference(masterSource));
			 registry.registerBeanDefinition(masterSource+"BeetlSqlDataSourceBean", bdb.getBeanDefinition());
			
		}
	}
	
	protected void readySqlManager(BeanDefinitionRegistry registry) {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if(classLoader==null) {
			classLoader = this.getClass().getClassLoader();
		}
		String sourceConfig = env.getProperty("beetlsql.mutiple.datasource");
		String[] sources = sourceConfig.split(",");
		for(String dbSource:sources) {
			String masterSource = dbSource;
			
			BeetlSqlProperties beetlSqlProperties = new BeetlSqlProperties(env,masterSource);
			ClasspathLoader loader = new ClasspathLoader(beetlSqlProperties.getSqlPath());
			Properties ps = new Properties();
			ps.put("PRODUCT_MODE", beetlSqlProperties.dev?"false":"true");
			
			 BeanDefinitionBuilder bdb = BeanDefinitionBuilder.rootBeanDefinition(SqlManagerFactoryBean.class);
			 bdb.addPropertyValue("cs", new RuntimeBeanReference(masterSource+"BeetlSqlDataSourceBean"));
			 bdb.addPropertyValue("dbStyle", (DBStyle)ObjectUtil.tryInstance(beetlSqlProperties.getDbStyle(),classLoader));
			 bdb.addPropertyValue("interceptors", beetlSqlProperties.dev ? new Interceptor[] { new DebugInterceptor() } : new Interceptor[0]);
			 bdb.addPropertyValue("sqlLoader", loader);
			 bdb.addPropertyValue("nc", (NameConversion) ObjectUtil.tryInstance(beetlSqlProperties.getNameConversion(),classLoader));
			 bdb.addPropertyValue("extProperties", ps);
//			 bdb.getBeanDefinition().set
		
			 registry.registerBeanDefinition(masterSource+"SqlManagerFactoryBean", bdb.getBeanDefinition());
			
		}
	}

	@Override
	public void setEnvironment(Environment env) {
		this.env = env;

	}

	
}

package com.ibeetl.starter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

public class BeetlMapperScannerRegister implements ImportBeanDefinitionRegistrar, ResourceLoaderAware ,EnvironmentAware{

    private ResourceLoader resourceLoader;
    
    Environment env;
    
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        BeetlSqlStarterClassPathScanner scanner = new BeetlSqlStarterClassPathScanner(registry);
        
        // this check is needed in Spring 3.1
        
        if (resourceLoader != null) {
            scanner.setResourceLoader(resourceLoader);
        }
        
        BeetlSqlProperties  ps = new BeetlSqlProperties(env);

        scanner.setSqlManagerFactoryBeanName("sqlManagerFactoryBean");

        scanner.setSuffix(ps.getDaoSuffix());
   
        scanner.registerFilters();

        scanner.doScan(ps.getBasePackage().split(","));
    }

	

	@Override
	public void setEnvironment(Environment env) {
		this.env = env;
		
	}
}

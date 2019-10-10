package com.ibeetl.starter;

import java.util.Properties;

import org.beetl.core.GroupTemplate;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.beetl.ext.spring.BeetlGroupUtilConfiguration;
import org.beetl.ext.spring.BeetlSpringViewResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "beetl.enabled", havingValue = 
"true", matchIfMissing = true)
public class BeetlTemplateConfig {
	@Value("${beetl.templatesPath:templates}")
	String templatesPath;// 模板跟目录
	
	@Value("${beetl.suffix:btl}")
	String suffix;// 模板后缀
	
	@Value("${beetl-beetlsql.dev:true}")
	boolean dev;
	
	
	@Autowired(required=false)
	BeetlTemplateCustomize beetlTemplateCustomize;
	
	@Bean( name = "beetlConfig")
	@ConditionalOnMissingBean(name={"beetlConfig"})
	public BeetlGroupUtilConfiguration getBeetlGroupUtilConfiguration() {
		BeetlGroupUtilConfiguration beetlGroupUtilConfiguration = new BeetlGroupUtilConfiguration();
		try {
			Properties extProperties = new Properties();
			if(dev){
				extProperties.put("RESOURCE.autoCheck", "true");
			}else{
				extProperties.put("RESOURCE.autoCheck", "false");
			}
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			if(loader==null){
				loader = BeetlTemplateConfig.class.getClassLoader();
			}
			beetlGroupUtilConfiguration.setConfigProperties(extProperties);
			ClasspathResourceLoader cploder = new ClasspathResourceLoader(loader,
					templatesPath);
			beetlGroupUtilConfiguration.setResourceLoader(cploder);
			beetlGroupUtilConfiguration.init();
			//如果使用了优化编译器，涉及到字节码操作，需要添加ClassLoader
			beetlGroupUtilConfiguration.getGroupTemplate().setClassLoader(loader);
			
			if(beetlTemplateCustomize!=null) {
				beetlTemplateCustomize.customize(beetlGroupUtilConfiguration.getGroupTemplate());
			}
			return beetlGroupUtilConfiguration;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Bean(name = "beetlViewResolver")
	@ConditionalOnMissingBean(name={"beetlViewResolver"})
	public BeetlSpringViewResolver getBeetlSpringViewResolver(
			@Qualifier("beetlConfig") BeetlGroupUtilConfiguration beetlGroupUtilConfiguration) {
		BeetlSpringViewResolver beetlSpringViewResolver = new BeetlSpringViewResolver();
		beetlSpringViewResolver.setContentType("text/html;charset=UTF-8");
		//ajax 支持
		beetlSpringViewResolver.setViewNames("*."+suffix,"*."+suffix+"#*");
		beetlSpringViewResolver.setOrder(0);
		beetlSpringViewResolver.setConfig(beetlGroupUtilConfiguration);
		return beetlSpringViewResolver;
	}
	
	@Bean(name = "groupTemplate")
	@ConditionalOnMissingBean(GroupTemplate.class)
	public GroupTemplate getGroupTemplate(
			@Qualifier("beetlConfig") BeetlGroupUtilConfiguration beetlGroupUtilConfiguration) {
		GroupTemplate gt =   beetlGroupUtilConfiguration.getGroupTemplate();
		return gt;
	}
	

	
	
	
}

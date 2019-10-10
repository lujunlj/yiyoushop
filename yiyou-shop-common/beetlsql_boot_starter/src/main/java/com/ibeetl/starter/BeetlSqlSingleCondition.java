package com.ibeetl.starter;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;
/**
 * 单数据源配置BeetlSql的条件
 * @author xiandafu
 *
 */
public class BeetlSqlSingleCondition implements  Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Environment env = context.getEnvironment();
		boolean enable = enableBeetlSql(env);
		boolean mutipleDataSource = hasMutipleDatasource(env);
		if(!enable) {
			return false;
		}
		
		if(enable&&!mutipleDataSource) {
			return true;
		}
		
		return false;
		
	}
	
	protected boolean enableBeetlSql(Environment env ) {
		String enableStr = env.getProperty("beetlsql.enabled");
		if(StringUtils.isEmpty(enableStr)) {
			//默认允许
			return true;
		}
		boolean enable = Boolean.parseBoolean(enableStr.trim());
		if(enable) {
			return true;
		}
		
		return false;
		
	}
	
	protected boolean hasMutipleDatasource(Environment env ) {
		String sourceList = env.getProperty("beetlsql.mutiple.datasource");
		if(StringUtils.isEmpty(sourceList)) {
			//默认允许
			return false;
		}else {
			return true;
		}
		
		
	}

}

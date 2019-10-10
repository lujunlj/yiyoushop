package com.ibeetl.starter;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * 多数据源配置条件
 * @author xiandafu
 *
 */
public class BeetlSqlMutipleCondition extends   BeetlSqlSingleCondition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Environment env = context.getEnvironment();
		boolean enable = enableBeetlSql(env);
		boolean mutipleDataSource = hasMutipleDatasource(env);
		if(!enable) {
			return false;
		}
		
		if(enable&&mutipleDataSource) {
			return true;
		}
		
		return false;
		
	}
	
	

}

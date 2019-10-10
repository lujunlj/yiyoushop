package org.beetl.sql.core.annotatoin;


import org.beetl.sql.core.annotatoin.builder.SampleInsertDeleteFlagBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 逻辑删除标记
 * @author xiandafu
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.FIELD})
@Builder(value = SampleInsertDeleteFlagBuilder.class)
public @interface LogicDelete {
	/**
	 *
	 * @return 设置逻辑删除值
	 */
	int value() default 0;
}



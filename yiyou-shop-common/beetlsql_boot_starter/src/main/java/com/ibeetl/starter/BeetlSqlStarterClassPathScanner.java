package com.ibeetl.starter;

import org.beetl.sql.ext.spring4.BeetlSqlClassPathScanner;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.Set;

public class BeetlSqlStarterClassPathScanner extends BeetlSqlClassPathScanner {

    public BeetlSqlStarterClassPathScanner(BeanDefinitionRegistry registry) {
        super(registry);
    }

    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        return super.doScan(basePackages);
    }
}

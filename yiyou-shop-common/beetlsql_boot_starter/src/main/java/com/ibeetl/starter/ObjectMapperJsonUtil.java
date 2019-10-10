package com.ibeetl.starter;

import org.beetl.ext.simulate.JsonUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperJsonUtil implements JsonUtil {

	ObjectMapper objMapper;
	public ObjectMapperJsonUtil(ObjectMapper objMapper){
		this.objMapper = objMapper;
	}
	@Override
	public String toJson(Object o) throws Exception {			
		return objMapper.writeValueAsString(o);			
	}

	@Override
	public Object toObject(String str, Class c) throws Exception {
		return objMapper.readValue(str, c);
	}

}

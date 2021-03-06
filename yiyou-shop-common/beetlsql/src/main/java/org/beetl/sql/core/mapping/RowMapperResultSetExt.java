package org.beetl.sql.core.mapping;

import org.beetl.sql.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author suxinjie
 *
 * 处理RowMapper.handleResultSet中的逻辑
 * 可以根据resultset对mapRow中的内容进行操作
 * 可用于关联表扩展和单表的属性增删改
 *
 */
public class RowMapperResultSetExt<T> implements ResultSetExt<List<T>> {

	BeanProcessor beanProcessor = null;
	private RowMapper<T> rowMapper;

	public RowMapperResultSetExt(RowMapper<T> _rowMapper, BeanProcessor beanProcessor) {
		this.rowMapper = _rowMapper;
		this.beanProcessor = beanProcessor;
	}

	@Override
	public List<T> handleResultSet(String sqlId, ResultSet rs, Class<?> z) throws SQLException {
		int rowNum = 0;
		List<T> resultList = new ArrayList<T>();
		if (Map.class.isAssignableFrom(z)) {
			while (rs.next()) {
				Object obj = beanProcessor.toMap(sqlId, z, rs);
				resultList.add(this.rowMapper.mapRow(obj, rs, rowNum++));
			}
		}
		while (rs.next()) {
			Object obj = beanProcessor.toBean(rs, z);
			resultList.add(this.rowMapper.mapRow(obj, rs, rowNum++));
		}
		return resultList;
	}


}

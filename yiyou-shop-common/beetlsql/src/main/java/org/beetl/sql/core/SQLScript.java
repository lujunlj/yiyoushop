package org.beetl.sql.core;

import org.beetl.core.Context;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.sql.core.annotatoin.AssignID;
import org.beetl.sql.core.annotatoin.builder.ObjectBuilderHolder;
import org.beetl.sql.core.annotatoin.builder.ObjectSelectBuilder;
import org.beetl.sql.core.db.*;
import org.beetl.sql.core.engine.SQLParameter;
import org.beetl.sql.core.kit.BeanKit;
import org.beetl.sql.core.kit.CaseInsensitiveOrderSet;
import org.beetl.sql.core.kit.StringKit;
import org.beetl.sql.core.mapping.BeanProcessor;
import org.beetl.sql.core.mapping.RowMapperResultSetExt;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

public class SQLScript {

    final SQLManager sm;
    final String id;
    final String sql;
    final SQLSource sqlSource;
    final String dbName;
    private List<SQLResultListener> listener;



//	final QueryMapping queryMapping = QueryMapping.getInstance();

    public SQLScript(SQLSource sqlSource, SQLManager sm) {
        this.sqlSource = sqlSource;
        this.sql = sqlSource.getTemplate();
        this.sm = sm;
        this.id = sqlSource.getId();
        this.dbName = sm.getDbStyle().getName();

    }

    private static boolean isBaseDataType(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return true;
        }

        if (clazz.getName().startsWith("java")) {
            return ((clazz == String.class) || clazz == Integer.class || clazz == Byte.class
                    || clazz == Long.class || clazz == Double.class || clazz == Float.class
                    || clazz == Character.class || clazz == Short.class || clazz == BigDecimal.class
                    || clazz == BigInteger.class || clazz == Boolean.class || clazz == java.util.Date.class
                    || clazz == java.sql.Date.class || clazz == Timestamp.class);
        } else {
            return false;
        }

    }

    protected SQLResult run(Map<String, Object> paras) {
        return this.run(paras, null);
    }

    protected SQLResult run(Map<String, Object> paras, Context ctx) {
        GroupTemplate gt = sm.beetl.getGroupTemplate();
        Template t = null;

        String parentId =  ctx==null?null:(String)ctx.getGlobal("_id");
        if (parentId != null) {
            t = gt.getTemplate(sqlSource.getId(), ctx);
        } else {

            t = gt.getTemplate(sqlSource.getId());
        }

        List<SQLParameter> jdbcPara = new LinkedList<SQLParameter>();
        if (paras != null) {
            for (Entry<String, Object> entry : paras.entrySet()) {
                t.binding(entry.getKey(), entry.getValue());
            }
        }

        t.binding("_paras", jdbcPara);
        t.binding("_manager", this.sm);
        t.binding("_id", id);

        String jdbcSql = t.render();
        SQLResult result = new SQLResult();
        result.jdbcSql = jdbcSql;
        result.jdbcPara = jdbcPara;
        //sql 脚本执行后回掉
        List<SQLResultListener> scriptListeners = (List<SQLResultListener>)t.getCtx().getGlobal("_listener");
        if(scriptListeners==null) {
            return  result;
        }
        if(this.listener==null) {
            this.listener = scriptListeners;
            return result;
        }
        this.listener.addAll(scriptListeners);
        return result;
    }

    /**
     * 检查目标类是否有申明ormquery,如果有，改写result.mapingEntrys
     *
     * @param target
     */
    protected void checkAnnotatonBeforeSelect(Class target,Map<String, Object> paras) {
        ClassAnnotation an = ClassAnnotation.getClassAnnotation(target);
        if(an.getObjectBuilders().isEmpty()) {
            return ;
        }
        for(ObjectBuilderHolder holder:an.getObjectBuilders()) {
            Object builder = holder.getInstance();
            if(builder instanceof ObjectSelectBuilder ) {
                ((ObjectSelectBuilder)builder).beforeSelect(target, this, holder.getBeanAnnotaton(),paras);
            }


        }

    }

    protected List checkAnnotatonAfterSelect(Class target,List entitys,SQLResult sqlResult){
        ClassAnnotation an = ClassAnnotation.getClassAnnotation(target);
        if(an.getObjectBuilders().isEmpty()) {
            return entitys;
        }
        List newList = entitys;
        for(ObjectBuilderHolder holder:an.getObjectBuilders()) {
            Object builder = holder.getInstance();
            if(builder instanceof ObjectSelectBuilder ) {
                newList =  ((ObjectSelectBuilder)builder).afterSelect(target, newList,this, holder.getBeanAnnotaton(),sqlResult);
            }

        }
        return newList;


    }



    public int insert(Object paras) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("_root", paras);
        addParaIfAssignId(paras);

        PreparedStatement ps = null;
        Connection conn = null;


        SQLResult result = this.run(map);
        String sql = result.jdbcSql;
        List<SQLParameter> objs = result.jdbcPara;
        InterceptorContext ctx = this.callInterceptorAsBefore(this.id, sql, true, objs, map);
        sql = ctx.getSql();
        objs = ctx.getParas();
        try {

            conn = sm.getDs().getConn(this.id, true, sql, objs);
            ps = conn.prepareStatement(sql);
            this.setPreparedStatementPara(ps, objs);
            int ret = ps.executeUpdate();
            this.callInterceptorAsAfter(ctx, ret);
            return ret;
        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(true, conn, ps);
        }
    }

    public int insert(Object paras, KeyHolder holder) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("_root", paras);
        addParaIfAssignId(paras);

        PreparedStatement ps = null;
        Connection conn = null;
        InterceptorContext ctx = null;
        try {


            SQLResult result = this.run(map);
            String sql = result.jdbcSql;
            List<SQLParameter> objs = result.jdbcPara;
            ctx = this.callInterceptorAsBefore(this.id, sql, true, objs, map);
            sql = ctx.getSql();
            objs = ctx.getParas();

            if (conn == null) {
                conn = sm.getDs().getConn(id, true, sql, objs);
            }
            int idType = ((SQLTableSource)sqlSource).getIdType();
            if (idType== DBStyle.ID_ASSIGN) {
                ps = conn.prepareStatement(sql);
            } else if (idType == DBStyle.ID_AUTO) {
                ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            } else if (idType == DBStyle.ID_SEQ) {
                CaseInsensitiveOrderSet idCols = (CaseInsensitiveOrderSet) ((SQLTableSource)sqlSource).getTableDesc().getIdNames();
                if (idCols.size() != 1) {
                    throw new BeetlSQLException(BeetlSQLException.ID_EXPECTED_ONE_ERROR);
                }
                ps = conn.prepareStatement(sql, new String[]{idCols.getFirst()});
            } else {
                ps = conn.prepareStatement(sql);
            }

            this.setPreparedStatementPara(ps, objs);

            int ret = ps.executeUpdate();

            if (idType == DBStyle.ID_AUTO || idType == DBStyle.ID_SEQ) {
                ResultSet seqRs = ps.getGeneratedKeys();
                seqRs.next();
                Object key = seqRs.getObject(1);
                holder.setKey(key);
                seqRs.close();
            }
            this.callInterceptorAsAfter(ctx, ret);
            return ret;
        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(true, conn, ps);
        }
    }

    public int insertBySqlId(Map map, KeyHolder holder, String keyName) {

        boolean getKey = holder != null;

        PreparedStatement ps = null;
        Connection conn = null;
        InterceptorContext ctx = null;
        try {

            SQLResult result = this.run(map);
            String sql = result.jdbcSql;
            List<SQLParameter> objs = result.jdbcPara;
            ctx = this.callInterceptorAsBefore(this.id, sql, true, objs, map);
            sql = ctx.getSql();
            objs = ctx.getParas();

            if (conn == null) {
                conn = sm.getDs().getConn(id, true, sql, objs);
            }

            if (getKey) {
                ps = conn.prepareStatement(sql, new String[]{keyName});
            } else {
                ps = conn.prepareStatement(sql);
            }


            this.setPreparedStatementPara(ps, objs);

            int ret = ps.executeUpdate();

            if (getKey) {
                ResultSet seqRs = ps.getGeneratedKeys();
                seqRs.next();
                Object key = seqRs.getObject(1);
                holder.setKey(key);
                seqRs.close();
            }

            this.callInterceptorAsAfter(ctx, ret);
            return ret;
        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(true, conn, ps);
        }
    }

    public <T> T singleSelect(Object paras, Class<T> target) {

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("_root", paras);
        return this.selectSingle(map, target);
    }

    public <T> T selectSingle(Map<String, Object> map, Class<T> target) {

        List<T> result = select(target, map);

        if (result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    public <T> T selectUnique(Map<String, Object> map, Class<T> target) {

        List<T> result = select(target, map);
        int size = result.size();
        if (size == 1) {
            return result.get(0);
        }else if(size==0) {
            throw new BeetlSQLException(BeetlSQLException.UNIQUE_EXCEPT_ERROR, "unique查询，但数据库未找到结果集:参数是" + map);
        }else {
            throw new BeetlSQLException(BeetlSQLException.UNIQUE_EXCEPT_ERROR, "unique查询，找到多条记录:参数是" + map);
        }

    }

    public <T> List<T> select(Class<T> clazz, Object paras) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("_root", paras);
        return this.select(clazz, map);
    }

    public <T> List<T> select(Class<T> clazz, Map<String, Object> paras, RowMapper<T> mapper) {
        //
        checkAnnotatonBeforeSelect(clazz, paras);
        //运行sql模板，获取实际的sql语句
        SQLResult result = run(paras);
        String sql = result.jdbcSql;
        List<SQLParameter> objs = result.jdbcPara;
        ResultSet rs = null;
        PreparedStatement ps = null;
        List<T> resultList = null;
        InterceptorContext ctx = this.callInterceptorAsBefore(this.id, sql, false, objs, paras);
        if (ctx.getResult() != null) {
            this.callInterceptorAsAfter(ctx, ctx.getResult());
            return (List<T>) ctx.getResult();
        }
        //再次获取参数，因为有可能被改变
        sql = ctx.getSql();
        objs = ctx.getParas();
        Connection conn = null;
        try {
            conn = sm.getDs().getConn(id, false, sql, objs);
            ps = conn.prepareStatement(sql);
            this.setPreparedStatementPara(ps, objs);


            rs = ps.executeQuery();

            if (mapper != null) {
                BeanProcessor beanProcessor = this.getBeanProcessor();
                resultList = new RowMapperResultSetExt<T>(mapper, beanProcessor).handleResultSet(this.id,rs, clazz);

            } else {
                resultList = mappingSelect(rs, clazz);


            }
            this.callInterceptorAsAfter(ctx, resultList);
            //通过注解实现后处理
            resultList = this.checkAnnotatonAfterSelect(clazz, resultList, result);
            //sql 脚本里通过listener 实现最后处理
            if (this.getListener() != null) {
                for (SQLResultListener listener : getListener()) {
                    listener.dataSelectd(resultList,paras,this.sm,result);
                }

            }


            return resultList;
        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(false, conn, ps, rs);
        }

    }

    public <T> List<T> select(Class<T> clazz, Map<String, Object> paras) {
        return this.select(clazz, paras, null);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> mappingSelect(ResultSet rs, Class<T> clazz) throws SQLException {
        List<T> resultList = null;
        BeanProcessor beanProcessor = this.getBeanProcessor();
        //类型判断需要做性能优化
        if (Map.class.isAssignableFrom(clazz)) {
            // 如果是Map的子类或者父类，返回List<Map<String,Object>>
            resultList = new ArrayList<T>();
            while (rs.next()) {

                Map map = beanProcessor.toMap(this.sqlSource.getId(), clazz, rs);
                resultList.add((T) map);
            }
            return resultList;

        } else if (isBaseDataType(clazz)) {

            resultList = new ArrayList<T>(1);

            while (rs.next()) {
                Object result = beanProcessor.toBaseType(this.sqlSource.getId(), clazz, rs);
                resultList.add((T) result);
            }
        } else {
            resultList = beanProcessor.toBeanList(this.sqlSource.getId(), rs, clazz);
            return resultList;
        }

        return resultList;

    }

    private BeanProcessor getBeanProcessor() {
        String sqlId = this.sqlSource.getId();
        BeanProcessor bp = this.sm.getProcessors().get(sqlId);
        if (bp != null) {
            return bp;
        }
        String ns = sqlId.substring(0, sqlId.indexOf("."));
        bp = this.sm.getProcessors().get(ns);
        if (bp != null) {
            return bp;
        } else {
            return sm.getDefaultBeanProcessors();
        }


    }

    public <T> List<T> select(Map<String, Object> paras, Class<T> mapping, RowMapper<T> mapper, long start, long size) {
        SQLScript pageScript = this.sm.getPageSqlScript(this.id);
        if (paras == null)
            paras = new HashMap<String, Object>();
        this.sm.getDbStyle().initPagePara(paras, start, size);
        return pageScript.select(mapping, paras, mapper);
        // return pageScript.se
    }

    public <T> List<T> select(Object paras, Class<T> mapping, RowMapper<T> mapper, long start, long end) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("_root", paras);
        return this.select(map, mapping, mapper, start, end);
    }

    public long selectCount(Object paras) {
        return this.singleSelect(paras, Long.class);
    }

    public long selectCount(Map<String, Object> paras) {
        return this.selectSingle(paras, Long.class);
    }

    public int update(Map<String, Object> paras) {

        SQLResult result = run(paras);
        String sql = result.jdbcSql;
        List<SQLParameter> objs = result.jdbcPara;

        InterceptorContext ctx = this.callInterceptorAsBefore(this.id, sql, true, objs, paras);
        sql = ctx.getSql();
        objs = ctx.getParas();
        int rs = 0;
        PreparedStatement ps = null;
        // 执行jdbc
        Connection conn = null;
        try {
            conn = sm.getDs().getConn(id, true, sql, objs);
            ps = conn.prepareStatement(sql);
            this.setPreparedStatementPara(ps, objs);
            rs = ps.executeUpdate();
            this.callInterceptorAsAfter(ctx, rs);
        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(true, conn, ps);
        }
        return rs;
    }

    public int update(Object obj) {
        Map<String, Object> paras = new HashMap<String, Object>();
        paras.put("_root", obj);
        return this.update(paras);
    }

    public int[] updateBatch(Map<String, Object>[] maps) {
        int[] rs = null;
        PreparedStatement ps = null;
        // 执行jdbc
        Connection conn = null;
        InterceptorContext ctx = null;
        try {
            conn = sm.getDs().getMaster();
            for (int k = 0; k < maps.length; k++) {
                Map<String, Object> paras = maps[k];
                SQLResult result = run(paras);
                List<SQLParameter> objs = result.jdbcPara;
                if (ps == null) {
                    ps = conn.prepareStatement(result.jdbcSql);
                    ctx = this.callInterceptorAsBefore(this.id, sql, true, Collections.EMPTY_LIST, paras);
                }
                this.setPreparedStatementPara(ps, objs);
                ps.addBatch();

            }
            rs = ps.executeBatch();

            this.callInterceptorAsAfter(ctx, rs);

        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(true, conn, ps);
        }
        return rs;
    }


    public int[] insertBatch(List<?> list,LinkedList keys,boolean autoDbAssignKey) {
        //与updateBatch区别是需要考虑到id生成
        if (list.size() == 0) {
            return new int[0];
        }
        int[] rs = null;
        PreparedStatement ps = null;
        Connection conn = null;
        // 执行jdbc
        InterceptorContext ctx = null;
        try {

            for (int k = 0; k < list.size(); k++) {
                Map<String, Object> paras = new HashMap<String, Object>();
                Object entity = list.get(k);
                this.addParaIfAssignId(entity);
                paras.put("_root", entity);
                SQLResult result = run(paras);
                List<SQLParameter> objs = result.jdbcPara;

                if (ps == null) {
                    conn = sm.getDs().getConn(id, true, result.jdbcSql, objs);
                    ps = autoDbAssignKey?conn.prepareStatement(result.jdbcSql,Statement.RETURN_GENERATED_KEYS):conn.prepareStatement(result.jdbcSql);
                    ctx = this.callInterceptorAsBefore(this.id, result.jdbcSql, true, new ArrayList<SQLParameter>(0), paras);
                }

                this.setPreparedStatementPara(ps, objs);

                ps.addBatch();

            }
            rs = ps.executeBatch( );
            if(autoDbAssignKey){
                ResultSet primaryKeys = ps.getGeneratedKeys();
                while(primaryKeys.next()) {
                    keys.add(primaryKeys.getObject(1));
                }
            }


            this.callInterceptorAsAfter(ctx, rs);

        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(true, conn, ps);
        }
        return rs;
    }


    public int[] updateBatch(List<?> list) {
        if (list.size() == 0) {
            return new int[0];
        }
        Connection conn = null;
        InterceptorContext lastCtx = null;
        int[] jdbcRets = new int[list.size()];
        // 执行jdbc
        try {
            //记录不同sql对应的PreparedStatement
            Map<String,PreparedStatement> batchPs = new HashMap<String,PreparedStatement>();
            //上下文
            Map<String,InterceptorContext> batchCtx = new HashMap<String,InterceptorContext>();
            //不同sql产生的批处理结果，汇总到jdbcRets
            Map<String,List<Integer>> batchRet = new HashMap<String,List<Integer>>();
            conn = sm.getDs().getMaster();
            for (int k = 0; k < list.size(); k++) {
                Map<String, Object> paras = new HashMap<String, Object>();
                paras.put("_root", list.get(k));
                SQLResult result = run(paras);
                List<SQLParameter> objs = result.jdbcPara;
                PreparedStatement ps = batchPs.get(result.jdbcSql);
                List<Integer> rets = batchRet.get(result.jdbcSql);
                InterceptorContext ctx = batchCtx.get(result.jdbcSql);
                if (ps == null) {
                    ps = conn.prepareStatement(result.jdbcSql);
                    ctx = new InterceptorContext(id, result.jdbcSql, new ArrayList<SQLParameter>(0), paras, true);
                    rets = new ArrayList<Integer> ();
                    batchCtx.put(result.jdbcSql, ctx);
                    batchPs.put(result.jdbcSql, ps);
                    batchRet.put(result.jdbcSql, rets);
                }

                this.setPreparedStatementPara(ps, objs);
                ps.addBatch();
                rets.add(k);
                ctx.getParas().add(new SQLParameter(objs));
            }

            for(Entry<String,PreparedStatement> entry:batchPs.entrySet()) {
                PreparedStatement ps = entry.getValue();
                lastCtx = batchCtx.get(entry.getKey());
                List<Integer> rets  = batchRet.get(entry.getKey());
                for (Interceptor in : sm.inters) {
                    in.before(lastCtx);
                }
                int[] rs = ps.executeBatch();
                for(int i=0;i<rs.length;i++) {
                    int realIndex = rets.get(i);
                    jdbcRets[realIndex] = rs[i];
                }
                this.callInterceptorAsAfter(lastCtx, rs);
            }


        } catch (SQLException e) {
            this.callInterceptorAsException(lastCtx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(conn);
        }
        return jdbcRets;
    }


    public <T> T unique(Class<T> clazz, RowMapper<T> mapper, Object objId) {
        return single(clazz, mapper, objId, true);
    }

    public <T> T single(Class<T> clazz, RowMapper<T> mapper, Object objId) {
        return single(clazz, mapper, objId, false);
    }


    public boolean existById(Class clazz,Object objId){
		MetadataManager mm = this.sm.getDbStyle().getMetadataManager();
		TableDesc table = mm.getTable(this.sm.getNc().getTableName(clazz));
		ClassDesc classDesc = table.getClassDesc(clazz, this.sm.getNc());
		Map<String, Object> paras = new HashMap<String, Object>();
		this.setIdsParas(classDesc, objId, paras);
		SQLResult result = run(paras);
		String sql = result.jdbcSql;
		List<SQLParameter> objs = result.jdbcPara;
		ResultSet rs = null;
		PreparedStatement ps = null;

		InterceptorContext ctx = this.callInterceptorAsBefore(this.id, sql, false, objs, paras);
		if (ctx.getResult() != null) {
			this.callInterceptorAsAfter(ctx, ctx.getResult());
			return (Boolean) ctx.getResult();
		}
		sql = ctx.getSql();
		objs = ctx.getParas();
		Connection conn = null;
		boolean hasResult = false ;
		try {
			conn = sm.getDs().getConn(id, false, sql, objs);
			ps = conn.prepareStatement(sql);
			this.setPreparedStatementPara(ps, objs);
			rs = ps.executeQuery();

			try {
				BeanProcessor beanProcessor = this.getBeanProcessor();
				rs.next();
				int count = rs.getInt(1);
				hasResult =  count!=0;
			} catch (BeetlSQLException ex) {

				if (ex.code == BeetlSQLException.UNIQUE_EXCEPT_ERROR) {
					throw new BeetlSQLException(BeetlSQLException.UNIQUE_EXCEPT_ERROR, "exsit" + table.getName() + ",但数据库未找到结果集:主键是" + objId);
				} else {
					throw ex;
				}
			}
			this.callInterceptorAsAfter(ctx, hasResult);
		} catch (SQLException e) {
			this.callInterceptorAsException(ctx, e);
			throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
		} finally {
			clean(false, conn, ps, rs);
		}
		return hasResult;

	}


    public <T> T single(Class<T> clazz, RowMapper<T> mapper, Object objId, boolean throwException) {

        MetadataManager mm = this.sm.getDbStyle().getMetadataManager();
        TableDesc table = mm.getTable(this.sm.getNc().getTableName(clazz));
        ClassDesc classDesc = table.getClassDesc(clazz, this.sm.getNc());
        Map<String, Object> paras = new HashMap<String, Object>();
        this.setIdsParas(classDesc, objId, paras);
        checkAnnotatonBeforeSelect(clazz, paras);
        SQLResult result = run(paras);
        String sql = result.jdbcSql;
        List<SQLParameter> objs = result.jdbcPara;
        ResultSet rs = null;
        PreparedStatement ps = null;
        T model = null;
        InterceptorContext ctx = this.callInterceptorAsBefore(this.id, sql, false, objs, paras);
        if (ctx.getResult() != null) {
            this.callInterceptorAsAfter(ctx, ctx.getResult());
            return (T) ctx.getResult();
        }
        sql = ctx.getSql();
        objs = ctx.getParas();
        Connection conn = null;
        try {
            conn = sm.getDs().getConn(id, false, sql, objs);
            ps = conn.prepareStatement(sql);
            this.setPreparedStatementPara(ps, objs);
            rs = ps.executeQuery();
            try {
                BeanProcessor beanProcessor = this.getBeanProcessor();
                if (rs.next()) {
                    model = beanProcessor.toBean(rs, clazz);
                } else {
                    if (throwException) {
                        throw new BeetlSQLException(BeetlSQLException.UNIQUE_EXCEPT_ERROR, "unique查询，但数据库未找到结果集");
                    } else {
                        this.callInterceptorAsAfter(ctx, ctx.getResult());
                        return null;
                    }
                }
                //row mapper
                if (mapper != null) {
                    model = mapper.mapRow(model, rs, 1);
                }
                //orm
                if (model != null && this.getListener()!=null) {
                    for (SQLResultListener listener : this.getListener()) {
                        listener.dataSelectd(Arrays.asList(model),paras,this.sm,result);

                    }
                }
            } catch (BeetlSQLException ex) {

                if (ex.code == BeetlSQLException.UNIQUE_EXCEPT_ERROR) {
                    throw new BeetlSQLException(BeetlSQLException.UNIQUE_EXCEPT_ERROR, "unique查询" + table.getName() + ",但数据库未找到结果集:主键是" + objId);
                } else {
                    throw ex;
                }
            }
            this.callInterceptorAsAfter(ctx, model);
        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(false, conn, ps, rs);
        }
        return model;
    }

    public int deleteById(Class<?> clazz, Object objId) {

        MetadataManager mm = this.sm.getDbStyle().getMetadataManager();
        TableDesc table = mm.getTable(this.sm.getNc().getTableName(clazz));
        ClassDesc classDesc = table.getClassDesc(clazz, this.sm.getNc());

        Map<String, Object> paras = new HashMap<String, Object>();
        this.setIdsParas(classDesc, objId, paras);

        SQLResult result = run(paras);
        String sql = result.jdbcSql;
        List<SQLParameter> objs = result.jdbcPara;

        InterceptorContext ctx = this.callInterceptorAsBefore(this.id, sql, true, objs, paras);

        sql = ctx.getSql();
        objs = ctx.getParas();
        int rs = 0;
        PreparedStatement ps = null;
        Connection conn = null;
        try {
            conn = sm.getDs().getConn(id, true, sql, objs);
            ps = conn.prepareStatement(sql);
            this.setPreparedStatementPara(ps, objs);
            rs = ps.executeUpdate();
            this.callInterceptorAsAfter(ctx, rs);
        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(true, conn, ps);
        }
        return rs;
    }

    public <T> List<T> sqlReadySelect(Class<T> clazz, SQLReady p) {
        String sql = this.sql;
        List<SQLParameter> objs = toSQLParameters(p.getArgs());
        ResultSet rs = null;
        PreparedStatement ps = null;
        List<T> resultList = null;
        InterceptorContext ctx = this.callInterceptorAsBefore(this.id, sql, false, objs, this.getSQLReadyParas(Arrays.asList(p.getArgs())));
        sql = ctx.getSql();
        objs = ctx.getParas();
        Connection conn = null;
        try {
            conn = sm.getDs().getConn(id, false, sql, objs);
            ps = conn.prepareStatement(sql);
            this.setPreparedStatementPara(ps, objs);
            rs = ps.executeQuery();
            resultList = mappingSelect(rs, clazz);

            this.callInterceptorAsAfter(ctx, resultList);
            return resultList;
        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(false, conn, ps, rs);
        }
    }

    public int sqlReadyExecuteUpdate(SQLReady p) {

        String sql = this.sql;
        List<SQLParameter> objs = toSQLParameters(p.getArgs());
        InterceptorContext ctx = this.callInterceptorAsBefore(this.id, sql, true, objs, this.getSQLReadyParas(Arrays.asList(p.getArgs())));

        sql = ctx.getSql();
        objs = ctx.getParas();
        int rs = 0;
        PreparedStatement ps = null;
        // 执行jdbc
        Connection conn = null;
        try {
            conn = sm.getDs().getConn(id, true, sql, objs);
            ps = conn.prepareStatement(sql);
            this.setPreparedStatementPara(ps, objs);
            rs = ps.executeUpdate();
            this.callInterceptorAsAfter(ctx, rs);
        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(true, conn, ps);
        }
        return rs;
    }

    public int[] sqlReadyBatchExecuteUpdate(SQLBatchReady  batch) {

        String sql = this.sql;
        List<Object[] > args = batch.getArgs();
        if(args.isEmpty()) {
            return new int[0];
        }
        InterceptorContext ctx = null;
        Connection conn  =null;
        PreparedStatement ps =null;
        int[] rs = null;
        try {

            for(int i=0;i<args.size();i++) {
                Object[] jdbcArgs = args.get(i);
                List<SQLParameter> objs = toSQLParameters(jdbcArgs);
                if(i==0) {
                    conn  = sm.getDs().getConn(id, true, sql, objs);
                    ctx = this.callInterceptorAsBefore(this.id, sql, true, objs, this.getSQLReadyParas(Arrays.asList(jdbcArgs)));
                    ps = conn.prepareStatement(sql);
                }
                this.setPreparedStatementPara(ps, objs);
                ps.addBatch();

            }
            rs = ps.executeBatch();
            this.callInterceptorAsAfter(ctx, rs);
        } catch (SQLException e) {
            this.callInterceptorAsException(ctx, e);
            throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
        } finally {
            clean(true, conn, ps);
        }
        return rs;




    }

    private void setPreparedStatementPara(PreparedStatement ps, List<SQLParameter> objs) throws SQLException {
        if (objs.isEmpty()) {
            return;
        }
        BeanProcessor beanProcessor = this.getBeanProcessor();
        beanProcessor.setPreparedStatementPara(this.sqlSource.getId(), ps, objs);

    }


    protected void clean(boolean isUpdate, Connection conn, PreparedStatement ps, ResultSet rs) {
        try {
            if (rs != null)
                rs.close();
            if (ps != null)
                ps.close();
            if (!this.sm.getDs().isTransaction()) {
                try {

                    if (conn != null) {
                        // colse 不一定能保证能自动commit
                        if (isUpdate && !conn.getAutoCommit()) {

                            conn.commit();
                        }

                        conn.close();
                    }
                } catch (SQLException e) {
                    throw new BeetlSQLException(BeetlSQLException.SQL_EXCEPTION, e);
                }

            }
        } catch (SQLException e) {
            // ignore
        }
    }

    protected void clean(boolean isUpdate, Connection conn, PreparedStatement ps) {
        this.clean(isUpdate, conn, ps, null);
    }

    protected void clean(Connection conn) {
        this.clean(true, conn, null, null);
    }

    private InterceptorContext callInterceptorAsBefore(String sqlId, String sql,
                                                       boolean isUpdate, List<SQLParameter> jdbcParas, Map<String, Object> inputParas) {

        InterceptorContext ctx = new InterceptorContext(sqlId, sql, jdbcParas, inputParas, isUpdate);
        for (Interceptor in : sm.inters) {
            in.before(ctx);
        }
        return ctx;
    }

    private void callInterceptorAsAfter(InterceptorContext ctx, Object result) {
        if (sm.inters == null)
            return;
        if (!ctx.isUpdate()) {
            ctx.setResult(result);

        } else {
            ctx.setResult(result);
        }
        for (Interceptor in : sm.inters) {
            in.after(ctx);
        }
        return;
    }

    private void callInterceptorAsException(InterceptorContext ctx, Exception ex) {
        if (ctx == null) {
            return;
        }
        if (sm.inters == null)
            return;

        for (Interceptor in : sm.inters) {
            in.exception(ctx, ex);
        }
        return;
    }

    private Map<String, Object> getSQLReadyParas(List<Object> paras) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("_root", paras);
        return map;
    }

    public String getId() {
        return id;
    }

    /**
     * 为主键设置参数
     *
     * @param desc
     * @param obj
     * @param paras
     */
    private void setIdsParas(ClassDesc desc, Object obj, Map<String, Object> paras) {
        List<String> idAttrs = desc.getIdAttrs();
        if (idAttrs.size() == 1) {
            paras.put(idAttrs.get(0), obj);
        } else {
            //来自对象id的属性.

            Map<String, Object> map = desc.getIdMethods();
            for (int i = 0; i < idAttrs.size(); i++) {
                String idCol = idAttrs.get(i);
                String idAttr = idAttrs.get(i);
                Method m = (Method) map.get(idAttr);
                try {
                    Object os = m.invoke(obj, new Object[0]);
                    paras.put(idAttr, os);
                } catch (Exception ex) {
                    throw new BeetlSQLException(BeetlSQLException.ID_VALUE_ERROR, "无法设置复合主键:" + idCol, ex);
                }
            }

        }
    }

    private void addParaIfAssignId(Object obj) {
        if (obj instanceof Map) {
            return;
        }
        if (obj == null) {
            return;
        }
        SQLTableSource tableSource = (SQLTableSource)this.sqlSource;
        Class clz = obj.getClass();
        if (tableSource.getIdType() == DBStyle.ID_ASSIGN && tableSource.getAssignIds() != null) {
            Map<String, AssignID> ids = tableSource.getAssignIds();
            for (Entry<String, AssignID> entry : ids.entrySet()) {
                String attrName = entry.getKey();
                Object value = BeanKit.getBeanProperty(obj, attrName);
                // 已经有值的列尊重调用者设置的值，@lidaoguang
                // 严格判断 null 和 empty 的 value，支持 ID 类型为 String 或者 Char 类型的情况 @larrykoo
                if (!StringKit.isNullOrEmpty(value)) {
                    continue;
                }
                AssignID assignId = entry.getValue();
                String algorithm = assignId.value();
                String param = assignId.param();
                Object o = this.sm.getAssignIdByIdAutonGen(algorithm, param, tableSource.getTableDesc().getName());
                BeanKit.setBeanProperty(obj, o, attrName);

            }

        }

    }

    private List<SQLParameter> toSQLParameters(Object[] args) {
        List<SQLParameter> paras = new ArrayList<SQLParameter>(args.length);
        for (Object arg : args) {
            paras.add(new SQLParameter(arg));

        }
        return paras;
    }

    public String getSql() {
        return sql;
    }

    public  List<SQLResultListener> getListener() {
        return listener;
    }

    public void setListener(List<SQLResultListener> listener) {
        this.listener = listener;
    }


}


package org.beetl.sql.core.mapper.internal;

import org.beetl.sql.core.SQLManager;
import org.beetl.sql.core.mapper.MapperInvoke;

import java.lang.reflect.Method;

/**
 * create time : 2017-04-27 16:08
 *
 * @author luoyizhu@gmail.com
 */
public class TemplateAmi implements MapperInvoke {

    @Override
    public Object call(SQLManager sm, Class entityClass, String sqlId, Method m, Object[] args) {
        if (args.length == 1) {
            return sm.template(args[0]);
        }
        
        return sm.template(args[0], (Integer) args[1], (Integer) args[2]);
    }

}

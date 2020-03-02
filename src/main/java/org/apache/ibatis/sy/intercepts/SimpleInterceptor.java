package org.apache.ibatis.sy.intercepts;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

/**
 * @Description: TODO
 * @Author: tona.sun
 * @Date: 2020/03/02 15:10
 */
@Intercepts(@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
public class SimpleInterceptor implements Interceptor {
  private String name;

  public SimpleInterceptor(String name) {
    this.name = name;
  }


  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    System.out.println("before intercept");
    Object proceed = invocation.proceed();
    System.out.println("after intercept");
    return proceed;
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
  }
}

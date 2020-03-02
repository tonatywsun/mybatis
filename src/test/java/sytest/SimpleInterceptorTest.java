package sytest;

import org.apache.ibatis.binding.MapperProxyFactory;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LoggingCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.decorators.SynchronizedCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.defaults.DefaultSqlSession;
import org.apache.ibatis.sy.intercepts.SimpleInterceptor;
import org.apache.ibatis.sy.mapper.TUserMapper;
import org.apache.ibatis.sy.model.TUser;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransaction;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: TODO
 * @Author: tona.sun
 * @Date: 2020/03/02 15:15
 */
public class SimpleInterceptorTest {
  @Test
  public void test() throws IOException, SQLException {
    //MyBatis 会按照默认的顺序去寻找日志实现类，日志的优先级顺序为SLF4j>Apache Commons Logging> Log4j 2> Log 4j >JDK Logging> StdOut Logging>NO Logging,
    //只要MyBatis找到了对应的依赖，就会停止继续找。因此,如果想要指定某种日志实现， 可以手动在LogFactory初次初始化前调用指定的方法，
    //下面代码通过调用useLog4JLogging方法指定使用Log4j,这个方法生效的前提是项目中必须有Log4j的依赖，否则将不会生效。
    LogFactory.useLog4JLogging();
    //创建配置对象
    final Configuration config = new Configuration();
    //配置settings中的部分属性
    config.setCacheEnabled(true);
    config.setLazyLoadingEnabled(false);
    config.setAggressiveLazyLoading(true);
    //这里按照顺序依次将拦截器1和2添加到config中，在后面的执行过程中可以看到这两个拦截器的执行顺序
    SimpleInterceptor interceptor1 = new SimpleInterceptor("interceptor1");
    SimpleInterceptor interceptor2 = new SimpleInterceptor("interceptor2");
    config.addInterceptor(interceptor1);
    config.addInterceptor(interceptor2);
    //使用MyBatis提供的最简单的UnpooledDataSource创建数据源
    UnpooledDataSource dataSource = new UnpooledDataSource();
    dataSource.setDriver("com.mysql.cj.jdbc.Driver");
    dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/member?serverTimezone=Hongkong");
    dataSource.setUsername("root");
    dataSource.setPassword("ssss1111");
    //使用JDBC事务
    Transaction transaction = new JdbcTransaction(dataSource, null, false);
    //Executor是MyBatis底层执行数据库操作的直接对象,大多数MyBatis方便调用的方式都是对该对象的封装。
    //通过Configuration的newExecutor方法创建的Executor会自动根据配置的拦截器对Executor进行多层代理。
    //通过这种代理机制使得MyBatis的扩展更方便，更强大。
    final Executor executor = config.newExecutor(transaction);
    //无论通过XML方式还是注解方式配置SQL语句,在MyBatis中,SQL语句都会被封装成SqlSource对象。
    //XML中配置的静态SQL会对应生成StaticSqlSource,带有if标签或者${}用法的SQL会按动态SQL被处理为DynamicSqlSource。使用Provider类注解标记的方法会生成ProviderSqlSource.
    //所有类型的SqlSource在最终执行前,都会被处理成StaticSqlSource
    StaticSqlSource sqlSource = new StaticSqlSource(config, "select * from t_user where id = ?");
    //由于上面的SQL有一个参数id,因此这里需要提供ParameterMapping（参数映射）
    //MyBatis文档中建议在XML中不去配置parameterMap属性,因为MyBatis会自动根据参数去判断和生成这个配置.在底层中,这个配置是必须存在的。
    List<ParameterMapping> parameterMappings = new ArrayList<>();
    TypeHandlerRegistry registry = new TypeHandlerRegistry();
    parameterMappings.add(new ParameterMapping.Builder(config, "id", registry.getTypeHandler(Integer.class)).build());
    ParameterMap.Builder paramBuilder = new ParameterMap.Builder(config, "defaultParameterMap", TUser.class, parameterMappings);
    ResultMap resultMap = new ResultMap.Builder(config, "defaultParameterMap", TUser.class, new ArrayList<ResultMapping>() {{
      add(new ResultMapping.Builder(config, "id", "id", Integer.class).build());
      add(new ResultMapping.Builder(config, "uNo", "u_no", Long.class).build());
    }}).build();
    ResultMap resultMap2 = new ResultMap.Builder(config, "defaultResultMap", TUser.class, new ArrayList<ResultMapping>()).build();
    //这是MyBatis根据装饰模式创建的缓存对象，通过层层装饰使得简单的缓存拥有了可配置的复杂功能。各级装饰缓存的含义可以参考代码中对应的注释。
    final Cache userCache = new SynchronizedCache(//同步缓存
      new SerializedCache(//序列化缓存
        new LoggingCache(//日志缓存
          new LruCache(//最少使用缓存
            new PerpetualCache("user_cache")//持久缓存
          )
        )
      )
    );
    MappedStatement.Builder msBuilder = new MappedStatement.Builder(config, "org.apache.ibatis.sy.mapper.TUserMapper.selectByPrimaryKey", sqlSource, SqlCommandType.SELECT);
    msBuilder.parameterMap(paramBuilder.build());
    List<ResultMap> resultMaps = new ArrayList<ResultMap>();
    resultMaps.add(resultMap);
    msBuilder.resultMaps(resultMaps);
    msBuilder.cache(userCache);
    MappedStatement ms = msBuilder.build();
    //该方法的第1个参数为MappedStatement对象，第2个参数是方法执行的参数，这里就是参数映射对应的参数值。第3个参数是MyBatis内存分页的参数，默认情况下使用RowBounds.DEFAULT,这种情况会获取全部数据,不会执行分页操作。
    //第4个参数在大多数情况下都是null,用于处理结果，因为MyBatis本身对结果映射己经做得非常好了，所以这里设置为null 时可以使用默认的结果映射处理器。
    List<TUser> tUsers = executor.query(ms, 1, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER);
    System.out.println(tUsers);

    //这里的代码使用Executor执行并不方便，因此可以再提高一个层次，将操作封装起来，使其更方便调用，代码如下。
    config.addMappedStatement(ms);
    SqlSession sqlSession = new DefaultSqlSession(config, executor, false);
    TUser tUser = sqlSession.selectOne("org.apache.ibatis.sy.mapper.TUserMapper.selectByPrimaryKey", 1);
    System.out.println(tUser);

    MapperProxyFactory<TUserMapper> mapperProxyFactory = new MapperProxyFactory<TUserMapper>(TUserMapper.class);
    TUserMapper tUserMapper = mapperProxyFactory.newInstance(sqlSession);
    TUser tUser1 = tUserMapper.selectByPrimaryKey(1);
    System.out.println(tUser);
  }
}

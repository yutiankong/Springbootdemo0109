package com.demo.config;

import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.session.mgt.eis.MemorySessionDAO;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {
    //创建ShiroFilterFactoryBean
    @Bean
    public ShiroFilterFactoryBean getShiroFilterFactoryBean(@Qualifier("securityManager") DefaultWebSecurityManager securityManager){
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        //设置安全管理器
        shiroFilterFactoryBean.setSecurityManager(securityManager);

        //添加Shiro内置过滤器
        /*Shiro内置过滤器，实现权限相关的拦截器
        *常用过滤器：
        *   anon:无需认证可以访问
        *   authc:必须认证才可以访问
        *   user:如果使用rememberMe功能可以直接访问
        *   perms:该资源必须得到资源权限才可以访问
        *   role:给资源必须得到角色权限才可以访问
        *
        * */
        Map<String,String> filterMap = new LinkedHashMap<>();
        filterMap.put("/test","anon");
        filterMap.put("/login","anon");
        filterMap.put("/loginin","anon");
        filterMap.put("/doRegister","anon");
        filterMap.put("/register","anon");
        //授权过滤器
        //当前授权拦截和，shiro自动跳转到未授权页面
        filterMap.put("/add","perms[user:add]");
        filterMap.put("/update","perms[user:update]");
        filterMap.put("/*","authc");

        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterMap);

        //默认跳转到login.jsp，修改为toLogin
        shiroFilterFactoryBean.setLoginUrl("/toLogin");
        //设置未授权页面
        shiroFilterFactoryBean.setUnauthorizedUrl("/unAuth");


        return shiroFilterFactoryBean;
    }


    //创建DefaultWebSecurityManager
    @Bean("securityManager")
    public DefaultWebSecurityManager getDefaultWebSecurityManager(@Qualifier("userRealm") UserRealm userRealm){
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        //关联Realm
        securityManager.setRealm(userRealm);
        //
        securityManager.setSessionManager( getDefaultWebSessionManager() );
        securityManager.setCacheManager(getCacheManager());
        return securityManager;
    }
    //创建Realm
    @Bean("userRealm")
    public UserRealm getRealm(){
        return new UserRealm();
    }

    //配置ShiroDialect,用于thymeleaf和shiro标签配合使用
    @Bean
    public ShiroDialect getShiroDialect(){
        return new ShiroDialect();
    }

    /**
     * 密码校验规则HashedCredentialsMatcher
     * 这个类是为了对密码进行编码的 ,
     * 防止密码在数据库里明码保存 , 当然在登陆认证的时候 ,
     * 这个类也负责对form里输入的密码进行编码
     * 处理认证匹配处理器：如果自定义需要实现继承HashedCredentialsMatcher
     */

    @Bean("hashedCredentialsMatcher")
    public HashedCredentialsMatcher hashedCredentialsMatcher() {
        HashedCredentialsMatcher hashedCredentialsMatcher = new HashedCredentialsMatcher();
        hashedCredentialsMatcher.setHashAlgorithmName("MD5");// 散列算法:这里使用MD5算法;
        hashedCredentialsMatcher.setHashIterations(1024);// 散列的次数，比如散列两次，相当于md5(md5(""));
        hashedCredentialsMatcher.setStoredCredentialsHexEncoded(true);//是否16进制
        return hashedCredentialsMatcher;
    }

    /**
     * 身份认证 realm
     */
    @Bean("myShiroRealm")
    public UserRealm myShiroRealm(){
        UserRealm myShiroRealm = new UserRealm();
        myShiroRealm.setCredentialsMatcher(hashedCredentialsMatcher());
        System.out.println("myShiroRealm 注入成功");
        return myShiroRealm;
    }

  /*  @Bean
    public DefaultSecurityManager securityManager() {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        // 注入自定义的realm;
        securityManager.setRealm(myShiroRealm());
        System.out.println("注入securtyManager");
        // 注入缓存管理器;
        //securityManager.setCacheManager(ehCacheManager());

        return securityManager;
    }*/

    //禁止重复登录
    // 配置sessionDAO
    @Bean(name="sessionDAO")
    public MemorySessionDAO getMemorySessionDAO(){
        MemorySessionDAO sessionDAO = new MemorySessionDAO();
        return sessionDAO;
    }

    //配置shiro session 的一个管理器
    @Bean(name = "sessionManager")
    public DefaultWebSessionManager getDefaultWebSessionManager(){
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        //DefaultWebSessionManager sessionManager = new MySessionManager();
        // 设置session过期时间
        sessionManager.setGlobalSessionTimeout(60*60*1000);
        sessionManager.setSessionDAO(getMemorySessionDAO());
        //解决url携带jessionid问题
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        return sessionManager;
    }

    //
    @Bean
    public EhCacheManager getCacheManager(){
        EhCacheManager ehCacheManager = new EhCacheManager();
        //ehCacheManager.setCacheManagerConfigFile("src\\main\\resources\\shiro-ehcache.xml");
        return ehCacheManager;
    }

}

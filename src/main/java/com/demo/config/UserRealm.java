package com.demo.config;

import com.demo.domain.Perms;
import com.demo.domain.Role;
import com.demo.domain.Teacher;
import com.demo.mapper.PermsMapper;
import com.demo.mapper.RoleMapper;
import com.demo.service.Inter.TeacherInter;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserRealm extends AuthorizingRealm {
    @Autowired
    TeacherInter tInter;
    @Autowired
    RoleMapper rMapper;
    @Autowired
    PermsMapper pMapper;

    @Autowired
    private SessionDAO sessionDAO;
    //执行授权逻辑
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        System.out.println("执行授权");
      /*  //给资源执行授权
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        //info.addStringPermission("user:add");
        //从数据库获取授权字符串
        //获取当前登录用户
        Subject subject = SecurityUtils.getSubject();
        Teacher principal = (Teacher) subject.getPrincipal();
        //Object principal = subject.getPrincipal();
        Teacher perms = tInter.perms(principal.getId());
        info.addStringPermission(perms.getPerms());
        return info;*/
        Teacher teacher = (Teacher) principalCollection.getPrimaryPrincipal();
        if (teacher != null) {
            //权限信息对象info,用来存放查出的用户的所有的角色（role）及权限（permission）
            SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
            //用户的角色集合
            info.addRoles(teacher.getRoleStrlist());
            //用户的权限集合
            info.addStringPermissions(teacher.getPerminsStrlist());

            return info;
        }
        // 返回null的话，就会导致任何用户访问被拦截的请求时，都会自动跳转到unauthorizedUrl指定的地址
        return null;
    }

    //执行认证逻辑
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        System.out.println("执行认证");
        //shiro判断逻辑
        //1.判断用户名
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        String tname = token.getUsername();
        //String password = new String((char[])authenticationToken.getCredentials());
        String tpassword = String.valueOf(token.getPassword());
        Teacher login = tInter.login(tname);

        //

        //
        if (login == null) {
            return null;
        }
            String md5 = new SimpleHash("MD5", tpassword, ByteSource.Util.bytes(tname), 1024).toHex();
            token.setPassword(md5.toCharArray());
            System.out.println("cred:"+md5);
            //
        List<Role> rlist = rMapper.roleList(login.getRid());//获取用户角色
        List<Perms> plist = pMapper.permList(login.getRid());//获取用户权限
        List<String> roleStrlist=new ArrayList<>();////用户的角色集合
        List<String> perminsStrlist=new ArrayList<>();//用户的权限集合
        for (Role role : rlist) {
            roleStrlist.add(role.getRole_name());
        }
        for (Perms uPermission : plist) {
            perminsStrlist.add(uPermission.getPerm());
        }
        login.setRoleStrlist(roleStrlist);
        login.setPerminsStrlist(perminsStrlist);
            //

          //禁止同时登陆
        if( tname.equals( login.getName() ) && md5.equals( login.getPassword() ) ){
            // 获取所有session
            Collection<Session> sessions = sessionDAO.getActiveSessions();
            for (Session session: sessions) {
                Teacher sysUser = (Teacher) session.getAttribute("USER_SESSION");
                // 如果session里面有当前登陆的，则证明是重复登陆的，则将其剔除
                System.out.println("seesion:"+session.getId());
                if( sysUser!=null ){
                    if( tname.equals( sysUser.getName() ) ){
                        session.setTimeout(0);
                    }
                }
            }
        }

        //
            SimpleAuthenticationInfo info = null;
            // info = new SimpleAuthenticationInfo(principal, credentials, credentialsSalt, realmName);
            String realname = getName();
            // 盐值加密，可以用用户名
            ByteSource salt = ByteSource.Util.bytes(tname);
            info = new SimpleAuthenticationInfo(login,login.getPassword(),salt, realname);
        Session session = SecurityUtils.getSubject().getSession();
        session.setAttribute("USER_SESSION", login);
            return info;



    }

}

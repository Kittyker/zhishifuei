package org.dromara.web.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.ObjectUtil;
import com.google.gson.JsonObject;
import com.xkcoding.http.HttpUtil;
import com.xkcoding.http.support.SimpleHttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWechatMiniProgramRequest;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.model.XcxLoginBody;
import org.dromara.common.core.domain.model.XcxLoginUser;
import org.dromara.common.core.domain.model.XcxPhoneNumberBody;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.controller.config.GlobalYunConfig;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import org.dromara.utils.OcrService;
import org.dromara.web.domain.vo.LoginVo;
import org.dromara.web.service.IAuthStrategy;
import org.dromara.web.service.SysLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 小程序认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service("xcx" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class XcxAuthStrategy implements IAuthStrategy {

    private final SysLoginService loginService;

    @Autowired
    private GlobalYunConfig.WX wx;

    @Autowired
    private OcrService ocrService;


    @Override
    public LoginVo login(String body, SysClientVo client) {
        XcxLoginBody loginBody = JsonUtils.parseObject(body, XcxLoginBody.class);
        ValidatorUtils.validate(loginBody);
        // xcxCode 为 小程序调用 wx.login 授权后获取
        String xcxCode = loginBody.getXcxCode();
        // 多个小程序识别使用
        String appid = loginBody.getAppid();

        // 校验 appid + appsrcret + xcxCode 调用登录凭证校验接口 获取 session_key 与 openid
        AuthRequest authRequest = new AuthWechatMiniProgramRequest(AuthConfig.builder()
            .clientId(wx.getAppId()).clientSecret(wx.getAppSecret())
            .ignoreCheckRedirectUri(true).ignoreCheckState(true).build());
        AuthCallback authCallback = new AuthCallback();
        authCallback.setCode(xcxCode);
        AuthResponse<AuthUser> resp = authRequest.login(authCallback);
        String openid, unionId;
        if (resp.ok()) {
            AuthToken token = resp.getData().getToken();
            openid = token.getOpenId();
            // 微信小程序只有关联到微信开放平台下之后才能获取到 unionId，因此unionId不一定能返回。
            unionId = token.getUnionId();
        } else {
            throw new ServiceException(resp.getMsg());
        }
        // 获取用户手机号
        String phoneNumber;
        try {
            phoneNumber = getGetPhoneNumber(loginBody.getPhoneCode());
        } catch (Exception e) {
            throw new ServiceException("手机号获取失败");
        }
        if (StringUtils.isEmpty(phoneNumber)) {
            throw new ServiceException("手机号获取失败");
        }
        // 框架登录不限制从什么表查询 只要最终构建出 LoginUser 即可
        SysUserVo user = loadUserByOpenid(openid, phoneNumber, loginBody);
        // 此处可根据登录用户的数据不同 自行创建 loginUser 属性不够用继承扩展就行了
        XcxLoginUser loginUser = new XcxLoginUser();
        loginUser.setTenantId(user.getTenantId());
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickname(user.getNickName());
        loginUser.setUserType(user.getUserType());
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        loginUser.setOpenid(openid);
        loginUser.setUnionid(unionId);
        SaLoginParameter model = new SaLoginParameter();
        model.setDeviceType(client.getDeviceType());
        // 自定义分配 不同用户体系 不同 token 授权时间 不设置默认走全局 yml 配置
        // 例如: 后台用户30分钟过期 app用户1天过期
        model.setTimeout(client.getTimeout());
        model.setActiveTimeout(client.getActiveTimeout());
        model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        // 生成token
        LoginHelper.login(loginUser, model);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        loginVo.setOpenid(openid);
        return loginVo;
    }
    private final ISysUserService sysUserService;
    private SysUserVo loadUserByOpenid(String openid, String phoneNumber, XcxLoginBody loginBody) {
        // 使用 openid 查询绑定用户 如未绑定用户 则根据业务自行处理 例如 创建默认用户
        // todo 自行实现 userService.selectUserByOpenid(openid);
        SysUserVo user =  sysUserService.queryUser(openid, phoneNumber);
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", openid);
            // todo 用户不存在 业务逻辑自行实现
            SysUserBo userBo = new SysUserBo();
            userBo.setPhonenumber(phoneNumber);
            userBo.setOpenid(openid);
            userBo.setNickName(loginBody.getNickName());
          return   sysUserService.registerUser(null, "000000");
        } else if (SystemConstants.DISABLE.equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", openid);
            // todo 用户已被停用 业务逻辑自行实现
            throw new ServiceException(String.format("登录用户：{} 已被停用.", openid));
        }else {
            SysUserBo userBo = new SysUserBo();
            userBo.setOpenid(openid);
            userBo.setUserId(user.getUserId());
            sysUserService.updateUserOpenId(userBo);
        }
        return user;
    }

    /**
     * 获取手机号码
     *
     * @param phoneCode
     * @return
     */
    private String getGetPhoneNumber(String phoneCode) {
        JsonObject param = new JsonObject();
        param.addProperty("code", phoneCode);
        String  url= "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token="+ocrService.getAccessToken();
        SimpleHttpResponse phoneRes = HttpUtil.post(url, param.toString());
        if (phoneRes.isSuccess() && StringUtils.isNotBlank(phoneRes.getBody())) {
            XcxPhoneNumberBody xcxPhoneNumberBody = JsonUtils.parseObject(phoneRes.getBody(), XcxPhoneNumberBody.class);
            if (ObjectUtil.isNotNull(xcxPhoneNumberBody) && ObjectUtil.isNotNull(xcxPhoneNumberBody.getPhone_info())
                && StringUtils.isNotBlank(xcxPhoneNumberBody.getPhone_info().getPhoneNumber())) {
                return xcxPhoneNumberBody.getPhone_info().getPhoneNumber();
            }
        }
        return "";
    }


}

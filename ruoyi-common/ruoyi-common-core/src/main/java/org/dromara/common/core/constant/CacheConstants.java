package org.dromara.common.core.constant;

/**
 * 缓存的key 常量
 *
 * @author Lion Li
 */
public interface CacheConstants {

    /**
     * 在线用户 redis key
     */
    String ONLINE_TOKEN_KEY = "online_tokens:";

    /**
     * 参数管理 cache key
     */
    String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    String SYS_DICT_KEY = "sys_dict:";

    /**
     * 登录账户密码错误次数 redis key
     */
    String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 微信小程序access_token redis key
     */
    String WX_MINI_ACCESS_TOKEN_KEY = "wx_mini_access_token";

    /**
     * 修改手机号验证码错误次数 redis key
     */
    String UPDATE_PHONE_SMS_CODE_CNT_KEY = "update_phone_sms_code_cnt:";
}

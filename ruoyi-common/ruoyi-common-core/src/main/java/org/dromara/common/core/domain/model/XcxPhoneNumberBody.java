package org.dromara.common.core.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class XcxPhoneNumberBody {
    private Integer errcode;
    private String errmsg;
    private XcxPhoneInfo phone_info;
}

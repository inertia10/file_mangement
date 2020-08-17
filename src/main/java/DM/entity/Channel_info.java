package DM.entity;

import lombok.Data;

@Data
public class Channel_info {
    //数据类型
    private String dataType;
    //数据间隔
    private int dataInterval;
    //数据个数
    private int dataNums;
    //数据长度
    private int dataLen;
}

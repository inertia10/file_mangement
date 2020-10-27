package DM.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User_fault {

    //@ExcelProperty(value = "ID",index = 0)
    private int id = 0;
   // @ExcelProperty(value = "文件名",index = 1)
    private String fileName;
   // @ExcelProperty(value = "故障名称")
    private String faultName;
    //@ExcelProperty(value = "试验类型")
    private String testType;
    //@ExcelProperty(value = "人员")
    private String member;
    //@ExcelProperty(value = "设备")
    private String device;
    //@ExcelProperty(value = "试验程度")
    private String degree;
    //@ExcelProperty(value = "温度")
    private String temperature;
    //@ExcelProperty(value = "压力")
    private String pressure;
   // @ExcelProperty(value = "流量")
    private String traffic;
    //@ExcelProperty(value = "文件路径")
    private String path;
    //@ExcelProperty(value = "试验时间")
    private Timestamp fileTime;
//    private DateTime InsertTime;


}

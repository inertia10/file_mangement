package DM.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User_fault {

    //@ExcelProperty(value = "ID",index = 0)
    private int Id;
    //@ExcelProperty(value = "Name",index = 1)
    private String FileName;
    private String Name;
    private String Type;
    private String Member;
    private String Device;
    private String Degree;
    private double Temperature;
    private double Pressure;
    private double Traffic;
    private String Path;
    private String Time;
    private DateTime InsertTime;

}

package DM;

import org.apache.tika.Tika;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Ignore;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;

/**
 * Created by Administrator on 2019-01-07 下午 2:22.
 */
@Ignore
public class Test {

    @org.junit.Test
    public void test() throws ParseException {
        String s1 = "20200907141132";
        StringBuffer s = new StringBuffer();
        s.append(s1.substring(0,4)+"-"+s1.substring(4,6)+"-"+s1.substring(6,8)+" "+s1.substring(8,10)+":"+s1.substring(10,12)+":"+s1.substring(12,14));
        String temp =s.toString();
        System.out.println(temp);

        System.out.println(DateTime.parse(s1, DateTimeFormat.forPattern("yyyyMMddHHmmss")));
        DateTime dateTime = DateTime.parse(s1, DateTimeFormat.forPattern("yyyyMMddHHmmss"));


        //string转date类型
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date1;
        date1 = sdf1.parse(s1);
        //date转datetime
        long longTime = date1.getTime();
        Timestamp timestamp = new Timestamp(longTime);
        System.out.println(timestamp);

    }
}

package DM;

import org.apache.tika.Tika;
import org.junit.Ignore;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;

/**
 * Created by Administrator on 2019-01-07 下午 2:22.
 */
@Ignore
public class Test {

    @org.junit.Test
    public void test() throws IOException {
        String s1 = "20200907141132";
        StringBuffer s = new StringBuffer();
        s.append(s1.substring(0,4)+"-"+s1.substring(4,6)+"-"+s1.substring(6,8)+" "+s1.substring(8,10)+":"+s1.substring(10,12)+":"+s1.substring(12,14));
        String temp =s.toString();
        System.out.println(temp);

    }
}

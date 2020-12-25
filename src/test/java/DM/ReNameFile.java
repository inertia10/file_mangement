package DM;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class ReNameFile {

    //设置文件前缀
    private static final String prefix = "VA";
	public static void main(String[] args) throws ParseException, IOException {
		copyFile("C:\\Users\\Wang.Cao\\Desktop\\需重命名文件\\zhengc\\高速\\VA20170624164435.dat");
//		recursion(new File("C:\\Users\\Wang.Cao\\Desktop\\需重命名文件"));
	}

	private static void copyFile(String path) throws ParseException, IOException {
		File file = new File(path);
		for(int i=1;i<2;i++){
			String name = file.getName();
			//获得文件名中的日期不包含最后两位毫秒数
			String text = name.substring(2, 16);
			SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
			//获得日期对应的毫秒数
			long time1 = sf.parse(text).getTime();
			long time2 = time1+ i*1000;
			Date date = new Date();
			date.setTime(time2);
			String rightDate = sf.format(date);
			//正确的文件名
			String rightName = file.getParent()+"\\"+prefix+rightDate+".dat";
			//重命名文件
			Files.copy(file.toPath(),new File(rightName).toPath());
		}
	}
	private static void recursion(File file) throws ParseException {
		//如果文件夹名字是高速，开始执行重命名该文件夹下的文件并退出递归
	    	if(file.getName().equals("高速")){
				File[] files = file.listFiles();
				assert files != null;
				for (int i = 0,len = files.length; i < len; i++) {
                    String name = files[i].getName();
                    //获得文件名中的日期不包含最后两位毫秒数
                    String text = name.substring(0, 14);
                    //获得文件名中最后两位毫秒数
                    String text2 = name.substring(14,16);
                    SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                    //获得日期对应的毫秒数
                    long time1 = sf.parse(text).getTime();
                    //获得最后两位毫秒数
                    long ms = Long.parseLong(text2) * 1000;
                    //将日期对应的毫秒数加上最后两位的毫秒数
                    time1 +=ms;
                    //将毫秒数转换为日期
                    Date date = new Date();
                    date.setTime(time1);
                    String rightDate = sf.format(date);
                    //正确的文件名
                    String rightName = prefix+rightDate;
                    //重命名文件
                    files[i].renameTo(new File(files[i].getParent()+"\\"+rightName+".dat"));
                    System.out.println("当前文件夹"+files[i].getParent()+"   进度: "+(i+1)+"/"+len);
				}
			}else{
	    		if (file.isDirectory()){
					File[] files = file.listFiles();
					assert files != null;
					for (File file1 : files) recursion(file1);
				}
			}
	    }
	}
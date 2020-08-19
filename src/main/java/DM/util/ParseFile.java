package DM.util;

import DM.entity.Channel_info;
import DM.entity.FileData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class ParseFile {

    public static FileData parse(String source) throws IOException {

        FileData fileData = new FileData();
        File sourceFile = new File(source);
        InputStream fis = new FileInputStream(sourceFile);
        long fileSize = sourceFile.length(); // 文件总字节数
        byte[] buffer = new byte[4];// 缓冲区字节数组

        int haveread = 0;//已读取的总字节数
        int readed = fis.read(buffer);//读取到字节数组的字节数
        int channel_num = ByteToInt(buffer);
        System.out.println("通道数->" + channel_num);
        fileData.setChannelNums(channel_num);
        //while (fis.read(buffer) != -1){
        //System.out.println("读取进度："  + (haveread += readed) * 100 / fileSize + "%   单次读取：" + readed + "个Byte");

        //通道信息
        List<Channel_info> channelLists = new ArrayList<>();
        for(int i=0;i<channel_num;i++){
            Channel_info channel_info = new Channel_info();
            fis.read(buffer);
            String datatype = ByteToStr(buffer);
            channel_info.setDataType(datatype);

            fis.read(buffer);
            int dataInterval = ByteToInt(buffer);
            channel_info.setDataInterval(dataInterval);

            fis.read(buffer);
            int dataNums = ByteToInt(buffer);
            channel_info.setDataNums(dataNums);

            fis.read(buffer);
            int dataLen = ByteToInt(buffer);
            channel_info.setDataLen(dataLen);

            channelLists.add(channel_info);
        }
        fileData.setChannelInfos(channelLists);

        //时域数据信息
        List<List<Double>> dataLists = new ArrayList<>();
        for (Channel_info channel : channelLists) {
            List<Double> dataList = new ArrayList<>();
            for(int i=0;i<channel.getDataNums();i++){
                fis.read(buffer);
                double dataTemp = ByteToDouble(buffer);
                dataList.add(dataTemp);
            }
            dataLists.add(dataList);
        }
        fileData.setTimeDomain(dataLists);

        //频域变换
        List<List<Complex>> frequencyLists = new ArrayList<>();
        for (List<Double> dataList : dataLists) {
            List<Complex> frequencyList = new ArrayList<>();
            Complex[] complexes = fft(dataList);
            CollectionUtils.addAll(frequencyList,complexes);

            frequencyLists.add(frequencyList);
        }
        fileData.setFrequencyDomain(frequencyLists);

        return fileData;
    }

    //将四位字节数组转为int
    public static int ByteToInt(byte[] bytes){
        int value=0;
        for(int i = 0; i < 4; i++) {
            int shift= (3-i) * 8;
            value +=(bytes[i] & 0xFF) << shift;
        }
        return value;
    }

    //将四位字节数组转为double
    public static int ByteToDouble(byte[] bytes){
        int value=0;
        for(int i = 0; i < 4; i++) {
            int shift= (3-i) * 8;
            value +=(bytes[i] & 0xFF) << shift;
        }
        return value;
    }

    public static char ByteToChar(byte[] b) {
        char c = (char) (((b[0] & 0xFF) << 8) | (b[1] & 0xFF));
        return c;
    }

    //将四位字节数组转为string
    public static String ByteToStr(byte[] bytes) throws UnsupportedEncodingException {
        return new String(bytes,"UTF-8");
    }

    //时域变换为频域
    public static Complex[] fft(List<Double> arr){

        int len = arr.size();//时域序列的元素个数
        int M = (int)((Math.log(len)/Math.log(2)==(int)(Math.log(len)/Math.log(2))?Math.log(len)/Math.log(2):(Math.log(len)/Math.log(2))+1));
        if(Math.pow(2,M)!=len){
            for(int i = 0;i<Math.pow(2,M)-len;i++)  arr.add(0.0);
        }
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] res= fft.transform(arr.stream().mapToDouble(Double::doubleValue).toArray(), TransformType.FORWARD);
        return res;
    }
}

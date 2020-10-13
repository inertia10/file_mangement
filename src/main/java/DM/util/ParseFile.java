package DM.util;

import DM.entity.Channel_info;
import DM.entity.Complex_diy;
import DM.entity.FileData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ParseFile {

    public static void main(String[] args) throws IOException {
        parse("D:\\receive\\主泵数据20200907145134.dat");
    }
    public static FileData parse(String source) throws IOException {
        FileData fileData = new FileData();
        File sourceFile = new File(source);
        InputStream fis = new FileInputStream(sourceFile);
        long fileSize = sourceFile.length(); // 文件总字节数
        byte[] buffer = new byte[4];// 缓冲区字节数组
        byte[] buffer2 = new byte[8];// 缓冲区字节数组

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
            if (channel.getDataLen()==64)
                for(int i=0;i<channel.getDataNums();i++){
                    fis.read(buffer2);
                    double dataTemp = ByteToDouble(buffer2);
                    dataList.add(dataTemp);
                }
            else if (channel.getDataLen()==32)
                for(int i=0;i<channel.getDataNums();i++){
                    fis.read(buffer);
                    double dataTemp = ByteToFloat(buffer);
                    dataList.add(dataTemp);
                }
            dataLists.add(dataList);
        }
        fileData.setTimeDomain(dataLists);

        //求取有效值
        List<Double> effectiveList = new ArrayList<>();
        double effective;
        int dataSize ;
        double average = 0;
        for (List<Double> dataList : dataLists) {
            int sum =0;
            dataSize = dataList.size();
            for (Double aDouble : dataList)
                sum += Math.abs(aDouble);
            try{
                average = (sum / dataSize);
            }catch (
                  Exception e
            ){
                log.error("除数不能为0");
            }
            effective = Math.pow(average,0.5);
            effectiveList.add(effective);
        }
        fileData.setEffectValue(effectiveList);

        //求取峰值
        List<Double> peakList = new ArrayList<>();
        double peak;
        for (List<Double> dataList : dataLists) {
            peak = Collections.max(dataList);
            peakList.add(peak);
        }
        fileData.setPeak(peakList);

        //求取方差
        List<Double> varianceList = new ArrayList<>();
        double sum ;
        for (List<Double> dataList : dataLists) {
            double sumSquare = 0;
            sum = dataList.stream().mapToDouble(Double::doubleValue).sum();
            dataSize = dataList.size();
            try{
                average = (sum / dataSize);
            }catch (
                    Exception e
            ){
                log.error("除数不能为0");
            }
            for (Double aDouble : dataList){
                sumSquare += Math.pow((aDouble-average),2);
            }
            varianceList.add(sumSquare / dataSize);
        }
        fileData.setVariance(varianceList);

        //频域变换
        List<List<Complex_diy>> frequencyLists = new ArrayList<>();
        Complex_diy comp = null;
        for (List<Double> dataList : dataLists) {
            List<Complex_diy> frequencyList = new ArrayList<>();
            Complex[] complexes = fft(dataList);
            for (Complex complex : complexes) {
                if(complex.getImaginary()>=0)
                    comp = new Complex_diy(complex.getReal(),complex.getImaginary());
                frequencyList.add(comp);
            }
            frequencyLists.add(frequencyList);
        }
        fileData.setFrequencyDomain(frequencyLists);

        return fileData;
    }

    //将四位字节数组转为int
    private static int ByteToInt(byte[] bytes){
        int value=0;
        for(int i = 0; i < 4; i++) {
            int shift= (3-i) * 8;
            value +=(bytes[i] & 0xFF) << shift;
        }
        return value;
    }

    //将8位字节数组转为double
    private static double ByteToDouble(byte[] arr){
        int[] unsigned = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            unsigned[i] = arr[i] & 0xFF;
        }
        long value = 0;
        double res = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) (unsigned[i])) << (8 * (7-i));
            res += Double.longBitsToDouble(value);
        }


        return res;

    }

    //将4位字节数组转为float
    private static float ByteToFloat(byte[] arr) {
        int[] unsigned = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            unsigned[i] = arr[i] & 0xFF;
        }
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value |= ((long) (unsigned[i])) << (8 * i);
        }
        return Float.intBitsToFloat(value);
    }

    //将四位字节数组转为string
    private static String ByteToStr(byte[] bytes) throws UnsupportedEncodingException {
        return new String(bytes,"UTF-8");
    }

    //时域变换为频域
    private static Complex[] fft(List<Double> arr){

        int len = arr.size();//时域序列的元素个数
        if(len==0) return null;
        int M = (int)((Math.log(len)/Math.log(2)==(int)(Math.log(len)/Math.log(2))?Math.log(len)/Math.log(2):(Math.log(len)/Math.log(2))+1));
        if(Math.pow(2,M)!=len){
            for(int i = 0;i<Math.pow(2,M)-len;i++)  arr.add(0.0);
        }
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] res= fft.transform(arr.stream().mapToDouble(Double::doubleValue).toArray(), TransformType.FORWARD);
        return res;
    }
}

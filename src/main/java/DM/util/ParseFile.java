package DM.util;

import DM.entity.Channel_info;
import DM.entity.FileData;
import com.mathworks.toolbox.javabuilder.MWException;
import com.mathworks.toolbox.javabuilder.MWNumericArray;
import fft_m.FFT_M;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

@Slf4j
public class ParseFile {

    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static long start;
    private static Boolean flag_evs = false;
    public static byte[] buffer_4 = new byte[4];// 缓冲区字节数组
    public static byte[] buffer_8 = new byte[8];// 缓冲区字节数组
    public static byte[] buffer_timeDomain;// 缓冲区字节数组

    public static void main(String[] args) throws IOException, MWException {
        long start = System.currentTimeMillis();
        parse("C:\\Users\\Wang.Cao\\Desktop\\CRDMA20181109130447.dat");
        long end = System.currentTimeMillis();
        System.out.println("总用时：" + (end - start) + "ms");
    }

    public static FileData parse(String source) throws IOException, MWException {
        start = System.currentTimeMillis();
        //int haveread = 0;//已读取的总字节数
        FileData fileData = new FileData();
        File sourceFile = new File(source);
        //long fileSize = sourceFile.length(); // 文件总字节数
        BufferedInputStream fis = new BufferedInputStream(new FileInputStream(sourceFile));
        int strLen = 0;
        //计算文件名字母个数，设置字节缓冲数组大小用来读取试验名称
        for (int i = 0, len = sourceFile.getName().length(); i < len; i++)
            if (Character.isDigit(sourceFile.getName().charAt(i))) {
                strLen = i;
                break;
            }
        byte[] buffer_str = new byte[strLen];// 缓冲区字节数组

        //判断是否是开关文件，开关文件不显示频域
        if (sourceFile.getName().substring(0, strLen).equals("EVS"))
            flag_evs = true;

        fis.read(buffer_4);//读取到字节数组的字节数
        int channel_num = ByteToInt(buffer_4);
        fileData.setChannelNums(channel_num);

        //通道信息
        List<Channel_info> channelLists = channelInfo(fis, channel_num, buffer_str);
        fileData.setChannelInfos(channelLists);

        //时域数据信息
        List<List<Float>> domainLists = dataDomain(fis, channelLists);
        fileData.setTimeDomain(domainLists);

        System.out.println("未采样时域数据总用时：" + (System.currentTimeMillis() - start) + "ms");

        //频域变换
        System.out.println("开始FFT变换");

        List<List<Float>> frequencyLists = fft_pure(domainLists, flag_evs);
        fileData.setFrequencyDomain(frequencyLists);

        System.out.println("FFT变换结束");

        //求取有效值
        List<Float> effectiveList = dataEffective(domainLists);
        fileData.setEffectValue(effectiveList);

        //求取峰值
        List<Float> peakList = dataPeak(domainLists);
        fileData.setPeak(peakList);

        //求取方差
        List<Float> varianceList = dataVariance(domainLists);
        fileData.setVariance(varianceList);



        return fileData;
    }

    //解析每个通道的信息
    public static List<Channel_info> channelInfo(BufferedInputStream fis, int channel_num, byte[] buffer3) throws IOException {
        List<Channel_info> channelLists = new ArrayList<>();
        for (int i = 0; i < channel_num; i++) {
            if (0 < i) {
                fis.read(buffer_4);
            }
            Channel_info channel_info = new Channel_info();
            fis.read(buffer3);
            String datatype = ByteToStr(buffer3);
            channel_info.setDataType(datatype);

            fis.read(buffer_4);
            int dataInterval = ByteToInt(buffer_4);
            channel_info.setDataInterval(dataInterval);

            fis.read(buffer_4);
            int dataNums = ByteToInt(buffer_4);
            channel_info.setDataNums(dataNums);
//            channel_info.setDataNums(50000);

            fis.read(buffer_4);
            int dataLen = ByteToInt(buffer_4);
            channel_info.setDataLen(dataLen);

            channelLists.add(channel_info);
        }
        return channelLists;
    }

    //解析每个通道时域数据
    public static List<List<Float>> dataDomain(BufferedInputStream fis, List<Channel_info> channelLists) throws IOException {
        List<List<Float>> domainLists = new ArrayList<>();
        List<Float> dataLi;
        for (Channel_info channel : channelLists) {
            int nums = channel.getDataNums();
            dataLi = new ArrayList<>();
            if (channel.getDataLen() == 64)
                for (int i = 0; i < nums; i++) {
                    fis.read(buffer_8);
                    double dataTemp = ByteToDouble(buffer_8);
                    //dataLi.add(Double.parseDouble(df.format(dataTemp)));
                    dataLi.add((float) dataTemp);
                }
            else {
                buffer_timeDomain = new byte[nums * 4];
                fis.read(buffer_timeDomain);
                dataLi = ByteToFloat(buffer_timeDomain);
            }
            domainLists.add(dataLi);
        }
        return domainLists;
    }

    //求取每个通道时域数据的有效值
    private static List<Float> dataEffective(List<List<Float>> domainLists) {
        List<Float> effectiveList = new LinkedList<>();
        Float effective;
        int dataSize;
        double average = 0;
        for (List<Float> dataList : domainLists) {
            int sum = 0;
            dataSize = dataList.size();
            for (Float aDouble : dataList)
                sum += Math.abs(aDouble);
            try {
                average = (sum / dataSize);
            } catch (
                    Exception e
            ) {
                log.error("除数不能为0");
            }
            effective = (float) Math.pow(average, 0.5);
            effectiveList.add(effective);
        }
        return effectiveList;
    }

    //求取每个通道时域数据的峰值
    private static List<Float> dataPeak(List<List<Float>> domainLists) {
        List<Float> peakList = new LinkedList<>();

        for (List<Float> dataList : domainLists)
            peakList.add(Collections.max(dataList));

        return peakList;
    }

    //求取每个通道时域数据的方差
    private static List<Float> dataVariance(List<List<Float>> domainLists) {
        List<Float> varianceList = new LinkedList<>();
        double sum;
        int dataSize;
        double average = 0;
        double temp;
        for (List<Float> dataList : domainLists) {
            double sumSquare = 0;
            sum = dataList.stream().mapToDouble(Float::floatValue).sum();
            dataSize = dataList.size();
            try {
                average = (sum / dataSize);
            } catch (
                    Exception e
            ) {
                log.error("除数不能为0");
            }
            for (Float aDouble : dataList) {
                sumSquare += Math.pow((aDouble - average), 2);
            }
            temp = sumSquare / dataSize;
            varianceList.add((float) temp);
        }
        return varianceList;
    }

    //将四位字节数组转为int
    public static int ByteToInt(byte[] bytes) {
        int[] unsigned = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            unsigned[i] = bytes[i] & 0xFF;
        }
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (3 - i) * 8;
            value += (unsigned[i] & 0xFF) << shift;
        }
        return value;
    }

    //将8位字节数组转为double
    private static double ByteToDouble(byte[] arr) {
        int[] unsigned = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            unsigned[i] = arr[i] & 0xFF;
        }
        long value = 0;
        double res = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) (unsigned[i])) << (8 * (7 - i));
            res += Double.longBitsToDouble(value);
        }
        return res;
    }

    //将4位字节数组转为float
    private static List<Float> ByteToFloat(byte[] arr) {
        int value = 0;
        float temp;
        int[] unsigned = new int[arr.length];
        List<Float> res = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            unsigned[i] = arr[i] & 0xFF;
        }
        for (int j = 0; j < arr.length; j += 4) {
            value = 0;
            for (int i = j % 4; i < 4; i++) {
                value |= ((long) (unsigned[j + i])) << (8 * (3 - i));
            }
            temp = Float.intBitsToFloat(value);
            res.add(temp);
        }
        return res;

    }

    //将四位字节数组转为string
    private static String ByteToStr(byte[] bytes) throws UnsupportedEncodingException {
        return new String(bytes, "UTF-8");
    }

    //fft
    public static List<List<Float>> fft_pure(List<List<Float>> datas, boolean flag_evs) throws MWException {
        if (flag_evs) return new ArrayList<>();
        if (CollectionUtils.isEmpty(datas)) return new ArrayList<>();
        List<List<Float>> resFinal = new ArrayList<>();
        List<Float> resTemp3;
        FFT_M fftM = new FFT_M();
        Object[] resTemp;
        double temp;
        int index=1;
        for (List<Float> data : datas) {
            resTemp3 = new ArrayList<>();
            start = System.currentTimeMillis();
            System.out.println("开始fft"+ index++ +"轮变换！");
            resTemp = fftM.fft_m(1, data.stream().mapToDouble(Float::floatValue).toArray());
            System.out.print("fft用时：");
            System.out.println(System.currentTimeMillis()-start);
            MWNumericArray resTemp2 = (MWNumericArray) resTemp[0];
            double[][] re = (double[][]) resTemp2.toDoubleArray();
            double[][] imag = (double[][]) resTemp2.toImagDoubleArray();
            for (int i = 0, len = re[0].length; i < len; i++) {
                temp = Math.sqrt(Math.pow(re[0][i], 2) + Math.pow(imag[0][i], 2));
                resTemp3.add((float) temp);
            }
            resFinal.add(resTemp3);
        }
        return resFinal;
    }

}

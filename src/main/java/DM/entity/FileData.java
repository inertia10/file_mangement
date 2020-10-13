package DM.entity;

import lombok.Data;
import org.apache.commons.math3.complex.Complex;

import java.util.List;

@Data
public class FileData {
   //通道数
   private int channelNums;
   //各个通道的信息
   private List<Channel_info>  channelInfos;
   //通道时域数据
   private List<List<Double>> timeDomain;
   //有效值
   private List<Double> effectValue;
   //峰值
   private List<Double> peak;
   //方差
   private List<Double> variance;
   //通道数据频域
   private List<List<Complex_diy>> frequencyDomain;

}

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
   private List<List<Float>> timeDomain;
   //有效值
   private List<Float> effectValue;
   //峰值
   private List<Float> peak;
   //方差
   private List<Float> variance;
   //通道数据频域
   private List<List<Float>> frequencyDomain;
   //是否需要区间输入框显示标志位
   private boolean tooBigNums = false;

}

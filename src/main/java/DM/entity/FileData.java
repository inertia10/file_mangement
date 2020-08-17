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
   //通道数据频域
   private List<List<Complex>> frequencyDomain;

}

package DM;

import org.junit.Test;

public class demo {

    @Test
    public void dd(){
        long a = 5,b = 10;
        double c = Double.longBitsToDouble(a);
        double d = Double.longBitsToDouble(b);
        double e = Double.longBitsToDouble(Long.MAX_VALUE-1);
        System.out.println(c+d==e);
        int ab = Integer.MAX_VALUE;
        ab = ab+2;
        int aaa_ = 5;

    }
}

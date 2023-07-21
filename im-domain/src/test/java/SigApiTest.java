import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.utils.SigAPI;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/2 21:06
 */
public class SigApiTest {

    @Test
    public void test_sigApi() {
        SigAPI asd = new SigAPI(10001, "bantanger");
        String sign = asd.genUserSig("10001", 100000000);
        JSONObject jsonObject = asd.decodeUserSig(sign);
        System.out.println("sign: " + sign);
        System.out.println("decoder: " + jsonObject.toString());
    }

}

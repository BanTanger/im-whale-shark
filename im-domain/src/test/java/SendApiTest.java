import com.bantanger.im.domain.message.service.P2PMessageService;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/26 17:20
 */
public class SendApiTest {

    @Resource
    P2PMessageService p2PMessageService;

    @Test
    public void send_test() {
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 100000; i ++) {

            }
        });
    }

}

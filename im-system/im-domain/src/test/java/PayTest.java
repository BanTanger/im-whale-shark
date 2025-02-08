import com.bantanger.im.Application;
import com.bantanger.im.service.strategy.pay.Payment;
import com.bantanger.im.service.strategy.pay.PaymentFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author BanTanger 半糖
 * @Date 2023/7/28 20:44
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class PayTest {

    @Autowired
    private PaymentFactory paymentFactory;

    @Test
    public void Test_Payment() {
        String type = "wechat";
        Payment payment = paymentFactory.getPayment(type);
        double amount = 100.00;
        payment.pay(amount);
        System.out.println("您已使用 " + type + " 支付了 " + amount + " 元");
    }

}

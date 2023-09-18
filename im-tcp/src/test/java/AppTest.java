import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * @author: chensongmin
 * @create: 2023-09-16 12:21
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = com.bantanger.im.tcp.Application.class)
public class AppTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    public void contextLoads() {
        redissonClient.getBucket("hello").set("bug");
        String test = (String) redissonClient.getBucket("hello").get();
        System.out.println(test);
    }

}

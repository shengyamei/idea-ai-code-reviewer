import java.io.IOException;

import org.springframework.transaction.annotation.Transactional;

/**
 * 演示样本（供功能演示与现场答辩使用）。
 * 场景：Spring 订单业务代码；演示工程不带 Spring 依赖，插件按短名降级匹配 @Transactional。
 * 内置缺陷：
 *   1. 事务失效：createOrder 声明受检异常 IOException 且未配置 rollbackFor（核心演示点）；
 *   2. 规则类缺陷：createOrder 中局部变量 traceId 未被使用；
 *   3. 规则类缺陷：warmup 中空 catch 块。
 */
class DemoOrderService {

    private final DemoInventoryMapper inventoryMapper = new DemoInventoryMapper();
    private final DemoPaymentGateway paymentGateway = new DemoPaymentGateway();
    private final DemoNotifier notifier = new DemoNotifier();

    @Transactional
    public String createOrder(String orderId, String skuId, int quantity) throws IOException {
        String traceId = orderId + "-" + System.currentTimeMillis();
        inventoryMapper.decrease(skuId, quantity);
        paymentGateway.charge(orderId, quantity);
        notifier.send(orderId);
        return orderId;
    }

    public void warmup() {
        try {
            notifier.ping();
        } catch (IOException e) {
        }
    }

    static class DemoInventoryMapper {
        void decrease(String skuId, int quantity) {
        }
    }

    static class DemoPaymentGateway {
        void charge(String orderId, int quantity) {
        }
    }

    static class DemoNotifier {
        void send(String orderId) throws IOException {
        }

        void ping() throws IOException {
        }
    }
}

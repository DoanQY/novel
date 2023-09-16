package io.github.xxyopen.novel.manager.mq;

import io.github.xxyopen.novel.core.constant.AmqpConsts;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * AMQP 消息管理类
 *
 * @author xiongxiaoyang
 * @date 2022/5/25
 */
@Component
// 自动为标记了final或@NonNull注解的字段生成一个构造函数，自动生成的构造函数会自动注入相关的字段
@RequiredArgsConstructor
public class AmqpMsgManager {

    private final AmqpTemplate amqpTemplate; // 会在构造函数中自动注入

    @Value("${spring.amqp.enabled:false}")
    private boolean amqpEnabled;

    /**
     * 发送小说信息改变消息
     */
    public void sendBookChangeMsg(Long bookId) {
        if (amqpEnabled) {
            sendAmqpMessage(amqpTemplate, AmqpConsts.BookChangeMq.EXCHANGE_NAME, null, bookId);
        }
    }

    private void sendAmqpMessage(AmqpTemplate amqpTemplate, String exchange, String routingKey,
        Object message) {
        // 如果在事务中则在事务执行完成后再发送，否则可以直接发送
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        amqpTemplate.convertAndSend(exchange, routingKey, message);
                    }
                });
            return;
        }
        /* exchange：交换机名称，用于指定消息的发送目的地
         * routingKey：路由键，用于指定消息的路由规则，交换机根据路由键的值，将消息发送到符合匹配规则的队列中
         * message：发送的消息
         */
        amqpTemplate.convertAndSend(exchange, routingKey, message);
    }

}

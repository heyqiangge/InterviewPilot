package dev.interviewpilot;

import dev.interviewpilot.Domain.Knowledge;
import java.util.List;

/** Small, auditable seed corpus. Add licensed source documents before claiming broad RAG coverage. */
public final class KnowledgeBase {
    private KnowledgeBase() {}
    public static final List<String> TOPICS = List.of("MySQL索引", "Redis缓存", "并发控制", "网络与TCP", "系统设计", "RAG检索");
    public static final List<Knowledge> DOCUMENTS = List.of(
            new Knowledge("MySQL索引", "复合索引与回表", "B+树索引支持最左前缀。覆盖索引可避免回表；范围条件之后的列能否用于定位取决于查询形态。应通过 EXPLAIN 与实际执行指标验证。"),
            new Knowledge("MySQL索引", "MVCC与隔离", "MVCC依赖事务版本与读视图。可重复读和读已提交生成读视图的时机不同。讨论幻读时要区分快照读和当前读。"),
            new Knowledge("Redis缓存", "缓存失效治理", "缓存穿透可用空值缓存或布隆过滤器；击穿需热点互斥或逻辑过期；雪崩可加过期抖动、多级缓存和限流。还需考虑一致性与回源压力。"),
            new Knowledge("并发控制", "任务并发与背压", "IO密集型任务可提高并发，但需要限制下游连接数、队列长度和内存占用。吞吐、P95延迟、错误率与CPU利用率应一起观测。"),
            new Knowledge("网络与TCP", "TCP拆包粘包", "TCP提供字节流而非消息边界。应用层可用定长、分隔符或长度字段分帧；还需处理半包、超长帧、超时和恶意输入。"),
            new Knowledge("系统设计", "限流与幂等", "令牌桶允许一定突发并控制平均速率。写操作幂等可用业务键与唯一约束，结合超时重试、状态机和补偿处理失败场景。"),
            new Knowledge("RAG检索", "混合检索与评测", "关键词检索适合精确术语，向量检索适合语义近似。融合检索后应评测 Recall@K、错误引用率和答案有据率，人工标注集应与调参集分离。")
    );
}

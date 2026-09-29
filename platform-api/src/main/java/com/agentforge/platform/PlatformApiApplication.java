package com.agentforge.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentForge 平台服务启动类。
 *
 * <p>启动时会依次发生：
 * <ol>
 *   <li>Spring Boot 读取 application.yml（可用环境变量覆盖，见 yml 里的 ${...} 占位符）；</li>
 *   <li>自动配置 DataSource / JPA，并建立到 MySQL 的连接池；</li>
 *   <li>Flyway 自动配置生效：扫描 {@code classpath:db/migration}，把没执行过的
 *       {@code V{版本}__{描述}.sql} 按版本号顺序执行，结果记录在 {@code flyway_schema_history} 表；</li>
 *   <li>Flyway 执行完毕后才初始化 JPA / 启动 Web 容器，因此迁移失败会直接让启动失败，
 *       不会出现"表没建好但服务已对外"的情况。</li>
 * </ol>
 */
@SpringBootApplication
public class PlatformApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }
}

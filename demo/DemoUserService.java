import java.io.IOException;
import java.sql.SQLException;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示样本二：用户服务模块（现场评审用，模拟真实业务代码，
 * 用于验证插件在不同文件上的实时评审效果）。
 */
class DemoUserService {

    private final DemoUserRepository repository = new DemoUserRepository();
    private final DemoAuditClient auditClient = new DemoAuditClient();

    @Transactional(propagation = Propagation.REQUIRED)
    public void register(String userId) throws SQLException {
        int retryLimit = 3;
        repository.insert(userId);
        auditClient.record("register:" + userId);
    }

    public void syncProfile(String userId) throws IOException {
        String cacheKey = "user:" + userId;
        repository.updateProfile(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void archive(String userId) throws SQLException {
        repository.moveToArchive(userId);
    }

    @Transactional
    public void markFailed(String userId) {
        throw new IllegalStateException("用户状态异常: " + userId);
    }

    public void purge(String userId) {
        try {
            repository.delete(userId);
        } catch (SQLException e) {
        }
    }

    static class DemoUserRepository {
        void insert(String userId) throws SQLException {
        }

        void updateProfile(String userId) throws IOException {
        }

        void moveToArchive(String userId) throws SQLException {
        }

        void delete(String userId) throws SQLException {
        }
    }

    static class DemoAuditClient {
        void record(String message) {
        }
    }
}

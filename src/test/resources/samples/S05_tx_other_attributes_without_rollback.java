import org.springframework.transaction.annotation.Transactional;

class S05Service {
    @Transactional(timeout = 30, readOnly = false)
    public void batch() throws java.io.IOException {
    }
}

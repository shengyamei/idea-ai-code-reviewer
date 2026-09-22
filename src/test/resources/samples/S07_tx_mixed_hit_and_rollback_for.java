import org.springframework.transaction.annotation.Transactional;

class S07Service {
    @Transactional
    public void risky() throws java.io.IOException {
    }

    @Transactional(rollbackFor = Exception.class)
    public void safe() throws java.io.IOException {
    }
}

import org.springframework.transaction.annotation.Transactional;

class S10Service {
    public void plainThrows() throws java.io.IOException {
    }

    @Transactional
    public void txNoThrows() {
    }
}

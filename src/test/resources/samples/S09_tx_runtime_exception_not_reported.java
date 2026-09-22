import org.springframework.transaction.annotation.Transactional;

class S09Service {
    @Transactional
    public void op() throws IllegalStateException {
    }
}

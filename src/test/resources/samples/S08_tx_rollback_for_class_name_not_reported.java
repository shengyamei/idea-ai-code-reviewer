import org.springframework.transaction.annotation.Transactional;

class S08Service {
    @Transactional(rollbackForClassName = "java.lang.Exception")
    public void op() throws java.io.IOException {
    }
}

import org.springframework.transaction.annotation.Transactional;

class S06Outer {

    static class Inner {
        @Transactional
        public void innerOp() throws java.io.IOException {
        }
    }
}

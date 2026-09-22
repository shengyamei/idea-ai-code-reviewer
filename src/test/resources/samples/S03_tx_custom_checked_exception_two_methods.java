import org.springframework.transaction.annotation.Transactional;

class BizException extends Exception {
}

class S03Service {
    @Transactional
    public void create() throws BizException {
    }

    @Transactional
    public void cancel() throws BizException {
    }
}

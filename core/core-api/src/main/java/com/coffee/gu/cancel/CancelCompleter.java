package com.coffee.gu.cancel;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CancelCompleter {

    private final CancelRollbacker cancelRollbacker;
    private final CancelRecorder cancelRecorder;

    public CancelCompleter(CancelRollbacker cancelRollbacker, CancelRecorder cancelRecorder) {
        this.cancelRollbacker = cancelRollbacker;
        this.cancelRecorder = cancelRecorder;
    }

    @Transactional
    public void complete() {
    }
}

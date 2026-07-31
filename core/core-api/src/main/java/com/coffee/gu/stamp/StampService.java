package com.coffee.gu.stamp;

import com.coffee.gu.Principal;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StampService {

    private final StampReader stampReader;

    public StampService(StampReader stampReader) {
        this.stampReader = stampReader;
    }

    public StampCount count(Principal principal) {
        Long count = stampReader.getAvailableStampCounts(principal);
        return new StampCount(principal.getKey(), count);
    }

    public StampExpiringSoonCount countExpiringSoon(Principal principal) {
        LocalDateTime now = LocalDateTime.now();
        Long expiringSoonCount = stampReader.getExpiringSoonCount(principal, now);
        return new StampExpiringSoonCount(principal.getKey(), expiringSoonCount);
    }

    public List<StampHistory> getStampHistories(Principal principal) {
        return stampReader.getStampHistories(principal);
    }

}

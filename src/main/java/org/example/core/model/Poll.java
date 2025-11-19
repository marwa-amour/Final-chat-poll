package org.example.core.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Poll {
    public final long id;
    public final long createdByUserId;
    public final long createdAtMs;
    public final long closesAtMs;
    public final java.util.List<Question> questions;
    public volatile boolean closed = false;

    public final Map<Long, int[]> answersByUser = new ConcurrentHashMap<>();

    public Poll(long id, long createdByUserId, long closesAtMs, java.util.List<Question> qs) {
        this.id = id;
        this.createdByUserId = createdByUserId;
        this.createdAtMs = System.currentTimeMillis();
        this.closesAtMs = closesAtMs;
        this.questions = java.util.Collections.unmodifiableList(qs);
    }
}

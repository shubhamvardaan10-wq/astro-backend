package com.astro.util;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public final class LimitedHttpBody implements HttpResponse.BodySubscriber<byte[]> {
    private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
    private final long limit;
    private Flow.Subscription subscription;
    private long count;
    private boolean failed;

    public LimitedHttpBody(long limit) {
        if (limit < 1) throw new IllegalArgumentException("Response limit must be positive");
        this.limit = limit;
    }

    public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
    public void onSubscribe(Flow.Subscription value) {
        subscription = value;
        delegate.onSubscribe(value);
    }
    public void onNext(List<ByteBuffer> buffers) {
        if (failed) return;
        for (ByteBuffer buffer : buffers) count += buffer.remaining();
        if (count > limit) {
            failed = true;
            subscription.cancel();
            delegate.onError(new IOException("Response exceeds limit"));
        } else delegate.onNext(buffers);
    }
    public void onError(Throwable error) { if (!failed) delegate.onError(error); }
    public void onComplete() { if (!failed) delegate.onComplete(); }
}

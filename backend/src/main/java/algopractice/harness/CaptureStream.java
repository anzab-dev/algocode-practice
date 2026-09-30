package algopractice.harness;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** An output stream that keeps at most {@code limit} bytes and remembers whether it dropped any. */
public final class CaptureStream extends OutputStream {

    private final int limit;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private boolean truncated;

    public CaptureStream(int limit) {
        this.limit = limit;
    }

    @Override
    public synchronized void write(int b) {
        if (buffer.size() < limit) {
            buffer.write(b);
        } else {
            truncated = true;
        }
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
        int room = limit - buffer.size();
        if (len > room) {
            truncated = true;
            len = Math.max(room, 0);
        }
        buffer.write(b, off, len);
    }

    public synchronized boolean truncated() {
        return truncated;
    }

    /** Returns the captured text and clears the buffer. */
    public synchronized String drain() {
        String text = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();
        return text;
    }
}

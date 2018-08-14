package kilanny.shamarlymushaf.data;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Shared {
    private final Lock lock = new ReentrantLock(true);
    private int _data;

    public void setData(int data) {
        try {
            lock.lock();
            _data = data;
        } finally {
            lock.unlock();
        }
    }

    public int getData() {
        try {
            lock.lock();
            return _data;
        } finally {
            lock.unlock();
        }
    }

    public void increment() {
        try {
            lock.lock();
            _data = _data + 1;
        } finally {
            lock.unlock();
        }
    }
}

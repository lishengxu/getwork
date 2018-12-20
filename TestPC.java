import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestPC {

    private static int size = 10;
    private static PriorityQueue<Integer> queue = new PriorityQueue<>(size);
    private static int data = 0;

    class ObjectCosumer extends Thread {
        @Override
        public void run() {
            cosume();
        }

        private void cosume() {
            while (true) {
                synchronized (queue) {
                    while (queue.size() == 0) {
                        try {
                            System.out.println("队列空，等待消耗数据");
                            queue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            queue.notify();
                        }
                    }
                    int i = queue.poll();
                    System.out.println(
                            "消费:" + i + ",队列中剩余:" + queue.size() + "个元素");
                    queue.notify();
                }
            }
        }
    }

    class ObjectProducer extends Thread {
        @Override
        public void run() {
            produce();
        }

        private void produce() {
            while (true) {
                synchronized (queue) {
                    while (queue.size() == size) {
                        try {
                            System.out.println("队列满，等待数据被消耗");
                            queue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            queue.notify();
                        }
                    }
                    queue.offer(data);
                    System.out.println("待消费数据:" + data + ",队列剩余空间:"
                            + (size - queue.size()));
                    ++data;
                    queue.notify();
                }
            }
        }
    }

    private Lock lock = new ReentrantLock();
    private Condition notFull = lock.newCondition();
    private Condition notEmpty = lock.newCondition();

    class ConditionCosumer extends Thread {
        @Override
        public void run() {
            cosume();
        }

        private void cosume() {
            while (true) {
                try {
                    lock.lock();
                    while (queue.size() == 0) {
                        System.out.println("队列空，等待消耗数据");
                        try {
                            notEmpty.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    int i = queue.poll();
                    System.out.println(
                            "消费:" + i + ",队列中剩余:" + queue.size() + "个元素");
                    notFull.signal();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    class ConditionProducer extends Thread {
        @Override
        public void run() {
            produce();
        }

        private void produce() {
            while (true) {
                try {
                    lock.lock();
                    while (queue.size() == size) {
                        System.out.println("队列满，等待数据被消耗");
                        try {
                            notFull.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    queue.offer(data);
                    System.out.println("待消费数据:" + data + ",队列剩余空间:"
                            + (size - queue.size()));
                    ++data;
                    notEmpty.signal();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    public static void main(String[] args) {
        TestPC pc = new TestPC();
//        ObjectProducer producer = pc.new ObjectProducer();
//        ObjectCosumer cosumer = pc.new ObjectCosumer();
//
//        producer.start();
//        cosumer.start();
        ConditionProducer producer = pc.new ConditionProducer();
        ConditionCosumer cosumer = pc.new ConditionCosumer();
        
        cosumer.start();
        producer.start();
    }

}

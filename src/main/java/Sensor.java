import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Target;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.concurrent.Semaphore;

public class Sensor {
    private String sensorName;
    private double x;
    private double y;
    private double xVelocity;
    private double yVelocity;
    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");

    private Semaphore semaphore1;
    private Semaphore semaphore2;

    public Sensor(String sensorName,double x, double y, double xVelocity, double yVelocity) {
        this.sensorName=sensorName;
        this.x = x;
        this.y = y;
        this.xVelocity = xVelocity;
        this.yVelocity = yVelocity;
        decimalFormat.setRoundingMode(RoundingMode.UP);
        semaphore1 = new Semaphore(1);
        semaphore2 = new Semaphore(0);
    }


    public void createServer(int port) {
            try (ServerSocket server = new ServerSocket(port)) {
            server.setReuseAddress(true);
            while (true) {
                Socket target = server.accept();
                target.setKeepAlive(true);
                System.out.println("Target Connected:" + target.getInetAddress().getHostAddress());
                TargetHandler targetHandler = new TargetHandler(target, this);
                Thread thread = new Thread(targetHandler);
                thread.start();
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    private static class TargetHandler implements Runnable {
        Socket target;
        Sensor sensor;

        public TargetHandler(Socket target, Sensor sensor) {
            this.target = target;
            this.sensor = sensor;
        }

        @Override
        public void run() {
            try (
                    ObjectInputStream ois = new ObjectInputStream(target.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(target.getOutputStream())
            ) {
                while (true) {
                    Target receivedData=(Target)ois.readObject();
                    System.out.println(receivedData.toString());
                    oos.writeObject(sensor.sensorName+":data received.");
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Target connection is broken");
            }
        }
    }

    class Target {
        private String targetName;
        private double x;
        private double y;
        private double xVelocity;
        private double yVelocity;

        public Target(String targetName, double x, double y, double xVelocity, double yVelocity) {
            this.targetName = targetName;
            this.x = x;
            this.y = y;
            this.xVelocity = xVelocity;
            this.yVelocity = yVelocity;
        }

        @Override
        public String toString() {
            return "Target{" +
                    "targetName='" + targetName + '\'' +
                    ", x=" + x +
                    ", y=" + y +
                    ", xVelocity=" + xVelocity +
                    ", yVelocity=" + yVelocity +
                    '}';
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getxVelocity() {
            return xVelocity;
        }

        public void setxVelocity(double xVelocity) {
            this.xVelocity = xVelocity;
        }

        public double getyVelocity() {
            return yVelocity;
        }

        public void setyVelocity(double yVelocity) {
            this.yVelocity = yVelocity;
        }
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getxVelocity() {
        return xVelocity;
    }

    public void setxVelocity(double xVelocity) {
        this.xVelocity = xVelocity;
    }

    public double getyVelocity() {
        return yVelocity;
    }

    public void setyVelocity(double yVelocity) {
        this.yVelocity = yVelocity;
    }

    public Semaphore getSemaphore1() {
        return semaphore1;
    }

    public void setSemaphore1(Semaphore semaphore1) {
        this.semaphore1 = semaphore1;
    }

    public Semaphore getSemaphore2() {
        return semaphore2;
    }

    public void setSemaphore2(Semaphore semaphore2) {
        this.semaphore2 = semaphore2;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public static void main(String[] args) {
        Sensor sensor=new Sensor("Sensor-1",200,300,0,0);
        sensor.createServer(8080);

    }
}


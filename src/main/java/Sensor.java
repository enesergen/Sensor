import org.apache.commons.lang3.SerializationUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;

public class Sensor implements Serializable {

    private String sensorName;
    private double x;
    private double y;
    private double xVelocity;
    private double yVelocity;
    private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private static int sensorServerPort;
    private static int processServerPort;
    private static String processServerAddress;

    private String message;

    public synchronized String getMessage() {
        return message;
    }

    public synchronized void setMessage(String message) {
        this.message = message;
    }

    public Sensor(String sensorName, double x, double y, double xVelocity, double yVelocity) {
        this.sensorName = sensorName;
        this.x = x;
        this.y = y;
        this.xVelocity = xVelocity;
        this.yVelocity = yVelocity;
        decimalFormat.setRoundingMode(RoundingMode.UP);
        message = "";
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

    private void sendProcessServer(String address, int portNumber) {
        Socket socket = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        boolean isConnected = provideFirstConnection(address, portNumber, socket, oos, ois);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        while (true) {

            if (isConnected) {
                try {
                    assert oos != null;
                    oos.writeObject(getMessage());
                    oos.flush();
                    String msg = (String) ois.readObject();
                    System.out.println("Process Server Response:" + msg);
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

            } else {
                try {
                    socket = new Socket(address, portNumber);
                    oos = new ObjectOutputStream(socket.getOutputStream());
                    ois = new ObjectInputStream(socket.getInputStream());
                    isConnected = true;
                    Thread.sleep(400);
                } catch (IOException | InterruptedException e) {
                    isConnected = false;
                    System.out.println("Connection failed. It will try to provide connection");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            }
        }
    }

    public String sensorToString() {
        return sensorName + "," + x + "," + y + "," + xVelocity + "," + yVelocity + ",";
    }

    public boolean provideFirstConnection(String address, int portNumber, Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
        boolean isConnected = false;
        while (!isConnected) {
            try {
                socket = new Socket(address, portNumber);
                oos = new ObjectOutputStream(socket.getOutputStream());
                ois = new ObjectInputStream(socket.getInputStream());
                isConnected = true;
                System.out.println("Connection is provided for port:" + portNumber);
            } catch (IOException e) {
                System.out.println("Try to connect sensor");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return isConnected;
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
                    String data = (String) ois.readObject();
                    sensor.setMessage(sensor.sensorToString() + data);
                    oos.writeObject(sensor.sensorName + ":Data received");
                    System.out.println(data);
                }

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Target connection is broken");
            }


        }
    }

    public void updateCoordinates() {
        while (true) {
            synchronized (this) {
                if (x > 500 | x < -500) {
                    xVelocity *= -1;
                }
                if (y > 500 | y < -500) {
                    yVelocity *= -1;
                }
                this.x += xVelocity;
                this.y -= yVelocity;
                this.x = formatAndRoundNumber(x);
                this.y = formatAndRoundNumber(y);
                System.out.println("Update Func " + "X:" + x + " Y:" + y);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public double formatAndRoundNumber(double number) {
        return Double.parseDouble(decimalFormat.format(number));
    }

    public static Sensor readObjectFromXML(String xmlPath) {
        Sensor sensor = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(xmlPath));
            doc.getDocumentElement().normalize();
            NodeList list = doc.getElementsByTagName("Sensor");

            for (int temp = 0; temp < list.getLength(); temp++) {
                Node node = list.item(temp);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    sensor = new Sensor(element.getElementsByTagName("sensorName").item(0).getTextContent(),
                            Double.parseDouble(element.getElementsByTagName("x").item(0).getTextContent()),
                            Double.parseDouble(element.getElementsByTagName("y").item(0).getTextContent()),
                            Double.parseDouble(element.getElementsByTagName("xVelocity").item(0).getTextContent()),
                            Double.parseDouble(element.getElementsByTagName("yVelocity").item(0).getTextContent()));
                    sensorServerPort = Integer.parseInt(element.getElementsByTagName("sensorServerPort").item(0).getTextContent());
                    processServerPort = Integer.parseInt(element.getElementsByTagName("processServerPort").item(0).getTextContent());
                    processServerAddress = element.getElementsByTagName("processServerAddress").item(0).getTextContent();
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
        return sensor;
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


    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public static void main(String[] args) {
        Sensor sensor = readObjectFromXML("C:\\Users\\stj.eergen\\Desktop\\Sensor\\src\\main\\resources\\sensor1.xml");
        Thread t1 = new Thread(sensor::updateCoordinates);
        t1.start();
        new Thread(() -> {
            sensor.createServer(sensorServerPort);
        }).start();
        new Thread(() -> {
            sensor.sendProcessServer(processServerAddress, processServerPort);
        }).start();
    }
}


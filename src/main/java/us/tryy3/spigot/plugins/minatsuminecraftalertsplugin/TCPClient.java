package us.tryy3.spigot.plugins.minatsuminecraftalertsplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.management.OperatingSystemMXBean;
import net.minecraft.server.v1_8_R3.MinecraftServer;

import java.io.DataInputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.Socket;

/**
 * Created by tryy3 on 2016-02-15.
 */
public class TCPClient extends Thread {
    Socket socket;
    int port;
    String ip;
    Alerts alerts;
    PrintWriter out;
    DataInputStream in;
    boolean connected = false;

    public TCPClient(Alerts alerts) {
        this.alerts = alerts;
        this.port = alerts.getConfig().getInt("TCP-PORT");
        this.ip = alerts.getConfig().getString("TCP-IP");
    }

    @Override
    public void run() {
        while (true)
        {
            try {
                System.out.println("Connecting to " + ip + ":" + port);
                socket = new Socket(ip, port);

                System.out.println("Connected.");

                while(true) {
                    out = new PrintWriter(socket.getOutputStream());

                    if (!connected) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("status", "connection");
                        obj.addProperty("name", alerts.getConfig().getInt("Server-Name"));
                        out.println(obj.toString());
                        connected = true;
                    }

                    in = new DataInputStream(socket.getInputStream());
                    String msg = in.readLine();

                    JsonObject json = new JsonParser().parse(msg).getAsJsonObject();

                    if (json == null || !json.has("status")) {
                        alerts.getLogger().severe("Got a request from the TCP server, but json was invalid.");
                        continue;
                    }

                    if (json.get("status").getAsString().equalsIgnoreCase("get")) {
                        JsonArray array = json.getAsJsonArray("messages");

                        JsonArray outArray = new JsonArray();

                        for (JsonElement element : array) {
                            JsonObject object = element.getAsJsonObject();

                            switch (object.get("get").getAsInt()) {
                                case 0:
                                    if (object.get("side").getAsBoolean()) {
                                        JsonObject obj = new JsonObject();
                                        obj.addProperty("channel", object.get("channel").getAsString());
                                        obj.addProperty("message", "Current TPS: " + MinecraftServer.getServer().recentTps[2]);
                                        outArray.add(obj);
                                    }
                                    //obj.addProperty();
                                    break;
                                case 1:
                                    if (object.get("side").getAsBoolean()) {
                                        boolean side = object.get("side").getAsBoolean();
                                        JsonObject max = new JsonObject();
                                        JsonObject total = new JsonObject();
                                        JsonObject free = new JsonObject();

                                        max.addProperty("channel", object.get("channel").getAsString());
                                        total.addProperty("channel", object.get("channel").getAsString());
                                        free.addProperty("channel", object.get("channel").getAsString());


                                        OperatingSystemMXBean OSMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

                                        max.addProperty("message", String.format("Maximum memory: %s MB", (side) ?
                                                Runtime.getRuntime().maxMemory() / 1024 / 1024 :
                                                OSMXBean.getTotalPhysicalMemorySize() / 1024 / 1024));

                                        total.addProperty("message", String.format("%s memory: %s MB", (side) ? "Allocated" : "Used", (side) ?
                                                Runtime.getRuntime().totalMemory() / 1024 / 1024 :
                                                (OSMXBean.getTotalPhysicalMemorySize() - OSMXBean.getFreePhysicalMemorySize()) / 1024 / 1024));

                                        free.addProperty("message", String.format("Free memory: %s MB", (side) ?
                                                Runtime.getRuntime().freeMemory() / 1024 / 1024 :
                                                OSMXBean.getFreePhysicalMemorySize()));

                                        outArray.add(max);
                                        outArray.add(total);
                                        outArray.add(free);
                                    }
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    break;
                            }

                            if (object.get("side").getAsBoolean()) {
                                // Front


                            } else {
                                // Back
                            }
                        }
                    }
                }
            }
        }
    }
}

package us.tryy3.spigot.plugins.minatsuminecraftalertsplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.management.OperatingSystemMXBean;
import net.minecraft.server.v1_8_R3.MinecraftServer;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * Created by tryy3 on 2016-02-15.
 */
public class TCPClient extends Thread {
    Socket socket;
    int port;
    String ip;
    Alerts alerts;
    PrintWriter out;
    BufferedReader in;
    boolean connected = false;

    public TCPClient(Alerts alerts) {
        this.alerts = alerts;
        this.port = alerts.getConfig().getInt("TCP-PORT");
        this.ip = alerts.getConfig().getString("TCP-IP");
        connect();
    }

    private void connect() {
        try {
            System.out.println("Connecting to " + ip + ":" + port);
            socket = new Socket(ip, port);

            System.out.println("Connected.");
            out = new PrintWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = false;
        } catch (IOException e) {
            System.out.println("Connection failed");
            connect();
        }
    }

    @Override
    public void run() {
        while (true)
        {
            try {
                while(true) {
                    System.out.println(connected);

                    if (!connected) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("status", "connection");
                        obj.addProperty("name", alerts.getConfig().getString("Server-Name"));
                        out.println(obj.toString() + "\n");
                        connected = true;
                    }

                    System.out.println(out);
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
                                    break;
                                case 2:
                                    if (object.get("side").getAsBoolean()) {
                                        JsonObject pluginFolder = new JsonObject();
                                        JsonObject serverFolder = new JsonObject();

                                        pluginFolder.addProperty("channel", object.get("channel").getAsString());
                                        serverFolder.addProperty("channel", object.get("channel").getAsString());

                                        pluginFolder.addProperty("message", String.format("Plugin folder: %s MB",
                                                getSize(alerts.getDataFolder().getParentFile(), null) / 1024 / 1024));

                                        serverFolder.addProperty("message", String.format("Server folder: %s MB",
                                                getSize(alerts.getServer().getWorldContainer(), null) / 1024 / 1024));

                                        outArray.add(pluginFolder);
                                        outArray.add(serverFolder);
                                    } else {
                                        File[] roots = File.listRoots();

                                        for (File root : roots) {
                                            if (root.getTotalSpace() == 0) continue;

                                            JsonObject disk = new JsonObject();

                                            disk.addProperty("channel", object.get("channel").getAsString());
                                            disk.addProperty("message", String.format("File system %s: %s GB free of %s GB (%s GB left)",
                                                    root.getAbsoluteFile(),
                                                    root.getFreeSpace(),
                                                    root.getTotalSpace(),
                                                    root.getUsableSpace()));
                                            outArray.add(disk);
                                        }
                                    }
                                    break;
                                case 3:
                                    if (object.get("side").getAsBoolean()) {
                                        JsonObject uptime = new JsonObject();
                                        uptime.addProperty("channel", object.get("channel").getAsString());
                                        uptime.addProperty("message", String.format("%s uptime: %s",
                                                alerts.getConfig().getString("Server-Name"),
                                                DateUtils.formatDateDiff(ManagementFactory.getRuntimeMXBean().getStartTime())));
                                        outArray.add(uptime);
                                    }
                                    break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection interrupted.");
                connect();
            }
        }
    }


    private long getSize(File file, Pattern pattern) {

        int size = 0;
        for (File f : file.listFiles()) {
            if (f.isFile()) {
                if (pattern != null && (pattern.matcher(f.getName())).find()) continue;
                size+=f.length();
            } else {
                size+=getSize(f, pattern);
            }
        }
        return size;
    }
}

package org.graalvm.argo.lambda_manager.metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class LambdaMetricsUtils {

    private static final double KB_IN_MB = 1024;

    private static double kilobytesToMegabytes(double sizeKb) {
        return sizeKb / KB_IN_MB;
    }

    private static InputStream executeCommand(String... command) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        process.waitFor();
        return process.getInputStream();
    }

    // Metrics collection for Docker containers.
    public static Map<String, Double> collectMemoryMetricsContainer() {
        try {
            InputStream stream = executeCommand("bash", "-c", "docker stats --all --no-stream --no-trunc --format \"{{.Name}} {{.MemUsage}}\"");
            return readMemoryToMapContainer(stream);
        } catch (Throwable thr) {
            thr.printStackTrace();
            return new HashMap<>();
        }
    }

    private static Map<String, Double> readMemoryToMapContainer(InputStream stream) {
        Map<String, Double> rssRecords = new HashMap<>(128);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // The inner try/catch is needed to skip lambdas for which we cannot collect memory.
                // Structure of the line: "lambda_228_HYDRA 73.84MiB / 256MiB"
                try {
                    String lambdaName = line.substring(0, line.indexOf(' '));
                    String rssString = line.substring(line.indexOf(' ') + 1, line.indexOf("MiB"));
                    double rss = rssString.isEmpty() ? 0 : Double.parseDouble(rssString);
                    rssRecords.put(lambdaName, rss);
                } catch (Exception e) {
                    // e.printStackTrace();
                    // System.err.println(e.getMessage());
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return rssRecords;
    }

    // Metrics collection for system footprint.
    public static double collectSystemFootprint() {
        try (InputStream stream = executeCommand("bash", "-c", "free")) {
            return readSystemUsedMemory(stream);
        } catch (Throwable thr) {
            thr.printStackTrace();
            return 0;
        }
    }

    // Metrics collection for system footprint.
    public static double[] collectCpuNumbers() {
        try (InputStream stream = executeCommand("bash", "-c", "top -bn 1 | grep \"Cpu(s)\"")) {
            return readCpu(stream);
        } catch (Throwable thr) {
            thr.printStackTrace();
            return new double[]{0, 0};
        }
    }

    private static double[] readCpu(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Line format:
                // %Cpu(s):  0.0 us,  0.2 sy,  0.0 ni, 99.8 id,  0.0 wa,  0.0 hi,  0.0 si,  0.0 st
                if (line.contains("%Cpu(s):")) {
                    String[] tokens = line.split("\\s+");
                    return new double[]{ Double.parseDouble(tokens[1]), Double.parseDouble(tokens[3]) };
                }
            }
        }
        return new double[]{0, 0};
    }

    private static double readSystemUsedMemory(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Line format:
                //               total        used        free      shared  buff/cache   available
                // Mem:      131664676     5704164    95283312        1340    30677200   124887432
                if (line.contains("Mem:")) {
                    String usedString = line.split("\\s+")[2];
                    return kilobytesToMegabytes(Double.parseDouble(usedString));
                }
            }
        }
        return 0;
    }
}

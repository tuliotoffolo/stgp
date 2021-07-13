package be.kuleuven.stgp.mip.column_generation;

import com.google.gson.*;

import java.io.*;
import java.nio.file.*;

public class Parameters {

    private final static Parameters singleton = new Parameters();

    public static Parameters get() {
        return singleton;
    }

    public final int nThreads;
    public final boolean cyclicBFS;
    public final boolean limitPricingIters;
    public final int maxItersPricing;
    public final boolean removeColumns;
    public final int itersColumnLife;
    public final double columnMinReducedCost;
    public final boolean populate;
    public final int populateLimit;
    public final boolean printPricing;
    public final int strongBranching;

    private Parameters() {
        JsonObject json = null;
        boolean hasConfigFile;

        try {
            json = ( JsonObject ) new JsonParser().parse(Files.newBufferedReader(Paths.get("bp.json")));
            hasConfigFile = json.isJsonObject();
        }
        catch (Exception ignore) {
            hasConfigFile = false;
        }

        if (hasConfigFile) {

            // nThreads
            if (json.has("nThreads"))
                nThreads = json.get("nThreads").getAsInt();
            else
                nThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

            cyclicBFS = json.get("cyclicBFS").getAsBoolean();
            limitPricingIters = json.get("limitPricingIters").getAsBoolean();
            maxItersPricing = json.get("maxItersPricing").getAsInt();
            removeColumns = json.get("removeColumns").getAsBoolean();
            itersColumnLife = json.get("itersColumnLife").getAsInt();
            columnMinReducedCost = json.get("columnMinReducedCost").getAsDouble();
            populate = json.get("populate").getAsBoolean();
            populateLimit = json.get("populateLimit").getAsInt();
            printPricing = json.get("printPricing").getAsBoolean();
            strongBranching = json.get("strongBranching").getAsInt();
        }
        else {
            nThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

            cyclicBFS = false;

            limitPricingIters = false;
            maxItersPricing = 50;

            removeColumns = true;
            itersColumnLife = 1000;
            columnMinReducedCost = 1.0;

            populate = false;
            populateLimit = 1000;

            printPricing = false;

            strongBranching = 1;
        }
    }

    public void writeJson() throws IOException {
        String outPath = "bp.json";
        JsonObject json = new JsonObject();

        //json.addProperty("nThreads", nThreads);

        json.addProperty("cyclicBFS", cyclicBFS);

        json.addProperty("limitPricingIters", limitPricingIters);
        json.addProperty("maxItersPricing", maxItersPricing);

        json.addProperty("removeColumns", removeColumns);
        json.addProperty("itersColumnLife", itersColumnLife);
        json.addProperty("columnMinReducedCost", columnMinReducedCost);

        json.addProperty("populate", populate);
        json.addProperty("populateLimit", populateLimit);

        json.addProperty("printPricing", printPricing);

        json.addProperty("strongBranching", strongBranching);

        PrintWriter printer = new PrintWriter(Files.newBufferedWriter(Paths.get(outPath)));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        printer.printf(gson.toJson(json));
        printer.close();
    }
}

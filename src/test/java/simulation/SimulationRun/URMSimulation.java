package simulation.SimulationRun;

import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import simulation.configuration.Config;
import simulation.simulationApi.URMApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static io.gatling.javaapi.core.CoreDsl.*;

public class URMSimulation extends Simulation {
    private Config config;
    private URMApi urmApi;

    private static final FeederBuilder<String> URMPropertyFeeder = csv("testData/URMDatasource/URM_PropertyInfoNew.csv").circular();
    private static final FeederBuilder<String> URMInventoryFeeder = csv("testData/URMDatasource/URMInventory.csv").random();
    private static final FeederBuilder<String> URMLOSMax = csv("testData/URMDatasource/URMLOSMax.csv").circular();
    private static final FeederBuilder<String> URMLOSMin = csv("testData/URMDatasource/URMLOSMin.csv").random();
    private static final FeederBuilder<String> URMLOSArray = csv("testData/URMDatasource/URMLOSArray.csv").random();
    private static final FeederBuilder<String> URMProdInventory = csv("testData/URMDatasource/URMProdInventory.csv").random();
    private static final FeederBuilder<String> URMRate = csv("testData/URMDatasource/URMRate.csv").random();

    private ScenarioBuilder scenarioURMApi;

    @Override
    public void before() {
        try {
            this.config = new Config();
            this.urmApi = new URMApi(config);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    {
        before(); // `before` method is called to initialize config and URMApi
        scenarioURMApi = scenario("URM API")
                .feed(urmApi.startFeeder)
                .feed(urmApi.endFeeder)
                .feed(URMInventoryFeeder)
                .feed(URMLOSMax)
                .feed(URMRate)
                .feed(URMLOSArray)
                .feed(URMPropertyFeeder)
                .feed(URMProdInventory)
                .exec(urmApi.postInvPropApi).pause(2)
                .exec(urmApi.postInvRoomApi).pause(2)
                .exec(urmApi.postInvProductApi).pause(2)
                .exec(urmApi.postRateUpdateApi).pause(2);

        setUp(
                scenarioURMApi.injectOpen(rampUsers(urmApi.USER_COUNT).during(urmApi.RAMP_DURATION))
        ).protocols(urmApi.getHttpProtocol())// Used`getHttpProtocol` to access httpProtocol
                .assertions(global().responseTime().mean().lt(2000),
                        forAll().responseTime().mean().lt(2000),
                        global().responseTime().max().lte(10000),
                        global().successfulRequests().percent().gt(90d));
    }

    @Override
    public void after() {
        // Call the checkReports method after simulation completes
        runSimulation();
        try {
            Thread.sleep(10000); // Ensure time for reports to generate before checking
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        //checkReports();
    }

    @Step("Execute URM API Simulation")
    @Description("Runs the URM API simulation and logs the results.")
    public void runSimulation() {
        //System.out.println("Started URM API Simulation with " + urmApi.USER_COUNT + " users...");
        try {
            Thread.sleep(urmApi.RAMP_DURATION * 1000L + 5000); // Wait for simulation to complete
            System.out.println("URM API Simulation completed.");
            generateAllureReport();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Simulation interrupted: " + e.getMessage());
        }
    }

    @Step("Generate Allure Report")
    @Description("Generates the Allure report from the results.")
    public void generateAllureReport() {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "mvn allure:report");
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor(); // Wait for the report generation to complete
            System.out.println("Allure report generated.");
        } catch (IOException e) {
            System.err.println("Error generating Allure report: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Allure report generation interrupted: " + e.getMessage());
        }
    }

    @Step("Check Gatling and Allure Reports")
    @Description("Compares Gatling performance metrics with Allure results.")
    public void checkReports() {
        System.out.println("Checking Gatling and Allure reports...");

        String reportPath = getLatestGatlingReportPath();
        if (reportPath == null) {
            System.err.println("Gatling report not found.");
            return;
        }

        double avgResponseTime = getAvgResponseTimeFromGatlingReport(reportPath);
        double expectedResponseTime = Double.parseDouble(config.getProperty("expectedResponseTime"));

        if (avgResponseTime <= expectedResponseTime) {
            System.out.println("Gatling report: Average response time is within expected limits: " + avgResponseTime);
        } else {
            System.out.println("Gatling report: Average response time exceeds expected limits: " + avgResponseTime);
        }

        double allureTestDuration = getAllureTestDuration();
        System.out.println("Allure report: Test duration was " + allureTestDuration);
    }

    private double getAvgResponseTimeFromGatlingReport(String reportPath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(reportPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Mean Response Time")) {
                    String[] parts = line.split(">");
                    String responseTime = parts[parts.length - 1].split("<")[0].trim();
                    return Double.parseDouble(responseTime);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading Gatling report: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error parsing response time: " + e.getMessage());
        }
        return 0;
    }

    private String getLatestGatlingReportPath() {
        String simulationName = this.getClass().getSimpleName().toLowerCase();
        File dir = new File("target/gatling/");
        File[] reportFolders = dir.listFiles((d, name) -> name.startsWith(simulationName));

        if (reportFolders == null || reportFolders.length == 0) {
            return null; // No report folder found
        }

        // Assume the latest folder is the last one in the array
        File latestReport = reportFolders[reportFolders.length - 1];
        return new File(latestReport, "index.html").getAbsolutePath();
    }

    private double getAllureTestDuration() {
        String allurePath = "allure-results/"; // Adjust based on your actual path
        String summaryFile = allurePath + "widgets/summary.json";
        try {
            String content = new String(Files.readAllBytes(Paths.get(summaryFile)));
            String durationString = content.split("\"duration\":")[1].split(",")[0].trim();
            return Double.parseDouble(durationString) / 1000; // Convert milliseconds to seconds
        } catch (IOException e) {
            System.err.println("Error reading Allure report: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error parsing duration from Allure report: " + e.getMessage());
        }
        return 0;
    }
}

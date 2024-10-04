package simulation.utils;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import simulation.configuration.Config;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.http.HttpDsl.http;

public class BaseSimulation {

    protected final Config config;
    protected HttpProtocolBuilder httpProtocol;
    public Iterator<Map<String, Object>> startFeeder;
    public Iterator<Map<String, Object>> endFeeder;

    public int USER_COUNT;
    public int RAMP_DURATION;

    public BaseSimulation(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        this.config = config;
        initializeHttpProtocol();
        this.startFeeder = Stream.generate((Supplier<Map<String, Object>>) () -> {
            String startDate = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now().plusDays(4).plusDays(new Random().nextInt(30)));
            return Map.of("startDate", startDate);
        }).iterator();

        this.endFeeder = Stream.generate((Supplier<Map<String, Object>>) () -> {
            String startDate = (String) ((Map<?, ?>) startFeeder.next()).get("startDate");
            LocalDate startLocalDate = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
            String endDate = DateTimeFormatter.ISO_LOCAL_DATE.format(startLocalDate.plusDays(new Random().nextInt(180) + 1));
            return Map.of("endDate", endDate);
        }).iterator();

        this.USER_COUNT = Integer.parseInt(config.getProperty("USERS"));
        this.RAMP_DURATION = Integer.parseInt(config.getProperty("RAMP_DURATION"));
    }

    private void initializeHttpProtocol() {
        this.httpProtocol = http
                .baseUrl(config.getProperty("api.baseurl"))
                .acceptHeader("application/json")
                .contentTypeHeader("application/json");
    }

    public HttpProtocolBuilder getHttpProtocol() {
        return this.httpProtocol;
    }

    public OpenInjectionStep.RampRate.RampRateOpenInjectionStep postEndpointInjectionProfile() {
        int totalDesiredUserCount = Integer.parseInt(config.getProperty("totalDesiredUserCount"));
        double userRampUpPerInterval = Double.parseDouble(config.getProperty("userRampUpPerInterval"));
        double rampUpIntervalSeconds = Double.parseDouble(config.getProperty("rampUpIntervalSeconds"));

        int totalRampUptimeSeconds = Integer.parseInt(config.getProperty("totalRampUptimeSeconds"));
        int steadyStateDurationSeconds = Integer.parseInt(config.getProperty("steadyStateDurationSeconds"));
        return rampUsersPerSec(userRampUpPerInterval / (rampUpIntervalSeconds / 60)).to(totalDesiredUserCount)
                .during(Duration.ofSeconds(totalRampUptimeSeconds + steadyStateDurationSeconds));
    }
}

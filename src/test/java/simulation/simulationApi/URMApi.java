package simulation.simulationApi;

import io.gatling.javaapi.core.ChainBuilder;
import simulation.configuration.Config;
import simulation.utils.BaseSimulation;
import simulation.utils.TokenGeneration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class URMApi extends BaseSimulation {

    TokenGeneration tokenGeneration = new TokenGeneration(config);

    public URMApi(Config config) {
        super(config);
    }

    // Fetch token only if it has expired or doesn't exist
    public ChainBuilder ensureToken() {
        return exec(session -> {
            if (!session.contains("access_token") || TokenGeneration.isTokenExpired()) {
                return session.set("tokenExpired", true);
            }
            return session.set("tokenExpired", false);
        }).doIf(session -> session.getBoolean("tokenExpired"))
                .then(exec(tokenGeneration.fetchToken())).exitHereIfFailed();
    }

    public ChainBuilder postInvPropApi =
            ensureToken().exec(http("Post URM Inv-Property API")
                    .post("/urm/v1/hotel/#{HotelId}/hotelAri/update")
                    .header("host", config.getProperty("host"))
                    .headers(tokenGeneration.sessionHeaders())
                    .body(ElFileBody("testData/URMPayload/URMInsert.json")).asJson()
                    .check(status().is(200))
                    .check(jsonPath("$.success").notNull())
            );

    public ChainBuilder postInvRoomApi =
            ensureToken().exec(http("Post URM Inv-Room API")
                    .post("/urm/v1/hotel/#{HotelId}/hotelAri/update")
                    .header("host", config.getProperty("host"))
                    .headers(tokenGeneration.sessionHeaders())
                    .body(ElFileBody("testData/URMPayload/URMInventory.json")).asJson()
                    .check(status().is(200))
                    .check(jsonPath("$.success").notNull())
            );

    public ChainBuilder postInvProductApi =
            ensureToken().exec(http("Post URM Inv-Product API")
                    .post("/urm/v1/hotel/#{HotelId}/hotelAri/update")
                    .header("host", config.getProperty("host"))
                    .headers(tokenGeneration.sessionHeaders())
                    .body(ElFileBody("testData/URMPayload/URMProduct.json")).asJson()
                    .check(status().is(200))
                    .check(jsonPath("$.success").notNull())
            );

    public ChainBuilder postRateUpdateApi =
            ensureToken().exec(http("Post URM Rate-Update API")
                    .post("/urm/v1/hotel/#{HotelId}/hotelAri/update")
                    .header("host", config.getProperty("host"))
                    .headers(tokenGeneration.sessionHeaders())
                    .body(ElFileBody("testData/URMPayload/URMRateUpdate.json")).asJson()
                    .check(status().is(200))
                    .check(jsonPath("$.success").notNull())
            ).exitHereIfFailed();
}

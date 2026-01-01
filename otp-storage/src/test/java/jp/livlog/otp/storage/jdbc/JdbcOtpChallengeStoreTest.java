package jp.livlog.otp.storage.jdbc;

import jp.livlog.otp.model.OtpChallenge;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class JdbcOtpChallengeStoreTest {

    @Test
    void save_find_update_delete() throws Exception {
        DataSource ds = h2();
        initSchema(ds);

        JdbcOtpChallengeStore store = new JdbcOtpChallengeStore(ds);

        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        OtpChallenge c1 = new OtpChallenge(
                "ch-1",
                "user-1",
                "hash-1",
                now.plusSeconds(300),
                0,
                0,
                now,
                OtpChallenge.Status.PENDING
        );

        store.save(c1);
        var found = store.find("ch-1").orElseThrow();
        assertEquals("user-1", found.userId());
        assertEquals(OtpChallenge.Status.PENDING, found.status());

        // update
        OtpChallenge c2 = new OtpChallenge(
                "ch-1",
                "user-1",
                "hash-2",
                now.plusSeconds(120),
                1,
                0,
                now,
                OtpChallenge.Status.VERIFIED
        );
        store.save(c2);

        var found2 = store.find("ch-1").orElseThrow();
        assertEquals("hash-2", found2.otpHash());
        assertEquals(1, found2.attempts());
        assertEquals(OtpChallenge.Status.VERIFIED, found2.status());

        // delete
        store.delete("ch-1");
        assertTrue(store.find("ch-1").isEmpty());
    }

    private DataSource h2() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:otp;MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private void initSchema(DataSource ds) throws Exception {
        try (Connection con = ds.getConnection();
             Statement st = con.createStatement()) {
            st.execute("""
                CREATE TABLE otp_challenges (
                  challenge_id VARCHAR(64) PRIMARY KEY,
                  user_id      VARCHAR(128) NOT NULL,
                  otp_hash     VARCHAR(512) NOT NULL,
                  expires_at   TIMESTAMP NOT NULL,
                  attempts     INT NOT NULL,
                  resends      INT NOT NULL,
                  last_sent_at TIMESTAMP NULL,
                  status       VARCHAR(16) NOT NULL
                )
                """);
            st.execute("CREATE INDEX idx_otp_user_status_sent ON otp_challenges(user_id, status, last_sent_at)");
        }
    }
}

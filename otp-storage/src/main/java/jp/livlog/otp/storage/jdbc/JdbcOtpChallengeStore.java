package jp.livlog.otp.storage.jdbc;

import jp.livlog.otp.model.OtpChallenge;
import jp.livlog.otp.storage.OtpChallengeStore;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;

public class JdbcOtpChallengeStore implements OtpChallengeStore {

    private final DataSource ds;

    public JdbcOtpChallengeStore(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public void save(OtpChallenge c) {
        // upsert（H2 / MySQL想定で方言があるので、まずは汎用に「UPDATE→なければINSERT」）
        int updated = update(c);
        if (updated == 0) {
            insert(c);
        }
    }

    private int update(OtpChallenge c) {
        final String sql = """
            UPDATE otp_challenges
               SET user_id = ?,
                   otp_hash = ?,
                   expires_at = ?,
                   attempts = ?,
                   resends = ?,
                   last_sent_at = ?,
                   status = ?
             WHERE challenge_id = ?
            """;
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            bind(ps, c, false);
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update otp_challenges", e);
        }
    }

    private void insert(OtpChallenge c) {
        final String sql = """
            INSERT INTO otp_challenges (
              challenge_id, user_id, otp_hash, expires_at, attempts, resends, last_sent_at, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            bind(ps, c, true);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert otp_challenges", e);
        }
    }

    /**
     * insertMode=true なら challenge_id を先頭に置く
     */
    private void bind(PreparedStatement ps, OtpChallenge c, boolean insertMode) throws SQLException {
        int i = 1;
        if (insertMode) {
            ps.setString(i++, c.challengeId());
        }
        ps.setString(i++, c.userId());
        ps.setString(i++, c.otpHash());
        ps.setTimestamp(i++, Timestamp.from(c.expiresAt()));
        ps.setInt(i++, c.attempts());
        ps.setInt(i++, c.resends());

        if (c.lastSentAt() != null) {
            ps.setTimestamp(i++, Timestamp.from(c.lastSentAt()));
        } else {
            ps.setNull(i++, Types.TIMESTAMP);
        }

        ps.setString(i++, c.status().name());

        if (!insertMode) {
            ps.setString(i, c.challengeId());
        }
    }

    @Override
    public Optional<OtpChallenge> find(String challengeId) {
        final String sql = """
            SELECT challenge_id, user_id, otp_hash, expires_at, attempts, resends, last_sent_at, status
              FROM otp_challenges
             WHERE challenge_id = ?
            """;
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, challengeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find otp_challenges", e);
        }
    }

    @Override
    public void delete(String challengeId) {
        final String sql = "DELETE FROM otp_challenges WHERE challenge_id = ?";
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, challengeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete otp_challenges", e);
        }
    }

    @Override
    public Optional<OtpChallenge> findLatestPendingByUser(String userId) {
        final String sql = """
            SELECT challenge_id, user_id, otp_hash, expires_at, attempts, resends, last_sent_at, status
              FROM otp_challenges
             WHERE user_id = ?
               AND status = 'PENDING'
             ORDER BY last_sent_at DESC
             LIMIT 1
            """;
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find latest pending otp_challenges", e);
        }
    }

    private OtpChallenge map(ResultSet rs) throws SQLException {
        String challengeId = rs.getString("challenge_id");
        String userId = rs.getString("user_id");
        String otpHash = rs.getString("otp_hash");

        Instant expiresAt = rs.getTimestamp("expires_at").toInstant();

        int attempts = rs.getInt("attempts");
        int resends = rs.getInt("resends");

        Timestamp lastSentTs = rs.getTimestamp("last_sent_at");
        Instant lastSentAt = (lastSentTs != null) ? lastSentTs.toInstant() : null;

        OtpChallenge.Status status = OtpChallenge.Status.valueOf(rs.getString("status"));

        return new OtpChallenge(
                challengeId,
                userId,
                otpHash,
                expiresAt,
                attempts,
                resends,
                lastSentAt,
                status
        );
    }
}

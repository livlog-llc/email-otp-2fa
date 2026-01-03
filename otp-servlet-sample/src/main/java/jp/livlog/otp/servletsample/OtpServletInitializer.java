package jp.livlog.otp.servletsample;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.EnumSet;

import javax.sql.DataSource;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.livlog.otp.mail.LoggingOtpMailer;
import jp.livlog.otp.mail.OtpMailer;
import jp.livlog.otp.policy.Clock;
import jp.livlog.otp.policy.OtpPolicy;
import jp.livlog.otp.storage.OtpChallengeStore;
import jp.livlog.otp.storage.jdbc.JdbcOtpChallengeStore;
import jp.livlog.otp.web.servlet.OtpWebConfig;
import jp.livlog.otp.web.servlet.filter.MfaEnforcerFilter;
import jp.livlog.otp.web.servlet.mail.DefaultOtpMailTemplate;
import jp.livlog.otp.web.servlet.service.ServletEmailOtp2faService;
import jp.livlog.otp.web.servlet.servlet.OtpStartServlet;
import jp.livlog.otp.web.servlet.servlet.OtpVerifyServlet;

public class OtpServletInitializer implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpServletInitializer.class);

    private DataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Web設定（デフォルト: /mfa, /mfa/start, /mfa/verify, 成功時は /app へ）
        OtpWebConfig config = OtpWebConfig.defaultConfig();

        // OTPポリシー
        OtpPolicy policy = OtpPolicy.defaultPolicy();

        // 永続化用DataSource（インメモリH2）
        this.dataSource = createInMemoryDataSource();
        initializeSchema(this.dataSource);

        // 永続化・メール送信
        OtpChallengeStore store = new JdbcOtpChallengeStore(this.dataSource);
        OtpMailer mailer = new LoggingOtpMailer();

        // MFAサービス
        var service = new ServletEmailOtp2faService(
                "OTP Servlet Sample",
                policy,
                Clock.systemUTC(),
                store,
                mailer,
                new DefaultOtpMailTemplate()
        );

        var ctx = sce.getServletContext();

        // OTP開始/検証Servlet登録
        ctx.addServlet("otpStartServlet", new OtpStartServlet(service))
           .addMapping(config.startPath());
        ctx.addServlet("otpVerifyServlet", new OtpVerifyServlet(service, config))
           .addMapping(config.verifyPath());

        // MFA未完了なら /mfa へ誘導するFilter
        ctx.addFilter("mfaEnforcerFilter", new MfaEnforcerFilter(config))
           .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");

        LOGGER.info("otp-servlet-sample initialized. OTP emails are logged instead of sent.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // H2メモリDBを明示的に閉じる
        if (this.dataSource != null) {
            try (var conn = this.dataSource.getConnection()) {
                conn.createStatement().execute("SHUTDOWN");
            } catch (SQLException e) {
                LOGGER.warn("Failed to close in-memory database", e);
            }
        }
    }

    private void initializeSchema(DataSource ds) {
        String ddl = """
                CREATE TABLE IF NOT EXISTS otp_challenges (
                  challenge_id VARCHAR(64) PRIMARY KEY,
                  user_id      VARCHAR(128) NOT NULL,
                  otp_hash     VARCHAR(512) NOT NULL,
                  expires_at   TIMESTAMP NOT NULL,
                  attempts     INT NOT NULL,
                  resends      INT NOT NULL,
                  last_sent_at TIMESTAMP NULL,
                  status       VARCHAR(16) NOT NULL
                );

                CREATE INDEX IF NOT EXISTS idx_otp_user_status_sent
                  ON otp_challenges(user_id, status, last_sent_at);
                """;
        try (var conn = ds.getConnection(); var stmt = conn.createStatement()) {
            for (String sql : ddl.split(";")) {
                if (!sql.isBlank()) {
                    stmt.execute(sql.trim());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize schema", e);
        }
    }

    private static DataSource createInMemoryDataSource() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("H2 driver not found. Ensure com.h2database:h2 is on the classpath.", e);
        }

        return new DriverManagerDataSource(
                "jdbc:h2:mem:otp-servlet-sample;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
    }

    private static class DriverManagerDataSource implements DataSource {

        private final String url;

        private final String username;

        private final String password;

        DriverManagerDataSource(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }

        @Override
        public java.sql.Connection getConnection() throws SQLException {
            return DriverManager.getConnection(this.url, this.username, this.password);
        }

        @Override
        public java.sql.Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(this.url, username, password);
        }

        @Override
        public java.io.PrintWriter getLogWriter() throws SQLException {
            return DriverManager.getLogWriter();
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) throws SQLException {
            DriverManager.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            DriverManager.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return DriverManager.getLoginTimeout();
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return DriverManager.getParentLogger();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (isWrapperFor(iface)) {
                return iface.cast(this);
            }
            throw new SQLException("No wrapper for " + iface.getName());
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }
    }
}

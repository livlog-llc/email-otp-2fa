# otp-web-servlet

`otp-web-servlet` は、**Servlet ベースの Web アプリケーションにメールOTP（2FA / MFA）を組み込むための共通ライブラリ**です。

このモジュールは以下を提供します。

- OTP開始（発行 → 保存 → メール送信）Servlet
- OTP検証（検証 → 状態更新 → セッションに MFA 完了を記録）Servlet
- MFA 未完了ユーザーを OTP 画面へ誘導する Filter
- セッションキー定義、Web 設定、メールテンプレート

> 依存モジュール
> - `otp-core` : OTP生成・検証の純ロジック
> - `otp-storage` : OTP状態の永続化
> - `otp-mail` : メール送信（Simple Java Mail 実装）

---

## 1. 全体の動作フロー

1. アプリ側で **ID/パスワード認証に成功**
2. セッションに「パスワード認証済み」を記録
3. MFA 画面 (`/mfa`) で「確認コード送信」
4. メールで OTP を受信
5. OTP 入力 → 検証成功
6. セッションに `MFA_OK=true` が入り、保護URLへアクセス可能になる

---

## 2. セッションキー（重要）

### 2.1 アプリ側がセットするキー

ID/パスワード認証成功時に、**必ず**以下をセッションにセットしてください。

- `OtpSessionKeys.USER_ID`
- `OtpSessionKeys.USER_EMAIL`
- `OtpSessionKeys.PASSWORD_OK = true`

### 推奨：ヘルパーメソッドを使用

```java
import jakarta.servlet.http.HttpSession;
import jp.livlog.otp.web.servlet.OtpWebSupport;

public void onPasswordLoginSuccess(HttpSession session, String userId, String email) {
    OtpWebSupport.markPasswordOk(session, userId, email);
}
````

---

## 3. 提供される URL

デフォルト設定では以下の URL を使用します。

| 用途             | HTTP | パス            |
| -------------- | ---- | ------------- |
| MFA画面（アプリ側で用意） | GET  | `/mfa`        |
| OTP送信          | POST | `/mfa/start`  |
| OTP検証          | POST | `/mfa/verify` |

URL は `OtpWebConfig` で変更可能です。

---

## 4. Servlet / Filter の登録方法

本モジュールは `web.xml` を持たないため、**アプリ側で Servlet / Filter を登録**します。
最も簡単な方法は `ServletContextListener` を使う方法です。

---

### 4.1 アプリ側初期化クラス例

```java
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.FilterRegistration;

import javax.sql.DataSource;

import jp.livlog.otp.policy.Clock;
import jp.livlog.otp.policy.OtpPolicy;
import jp.livlog.otp.storage.OtpChallengeStore;
import jp.livlog.otp.storage.jdbc.JdbcOtpChallengeStore;
import jp.livlog.otp.mail.OtpMailer;
import jp.livlog.otp.mail.SmtpConfig;
import jp.livlog.otp.mail.simple.SimpleJavaMailOtpMailer;

import jp.livlog.otp.web.servlet.OtpWebConfig;
import jp.livlog.otp.web.servlet.filter.MfaEnforcerFilter;
import jp.livlog.otp.web.servlet.mail.DefaultOtpMailTemplate;
import jp.livlog.otp.web.servlet.service.ServletEmailOtp2faService;
import jp.livlog.otp.web.servlet.servlet.OtpStartServlet;
import jp.livlog.otp.web.servlet.servlet.OtpVerifyServlet;

public class OtpWebInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        var ctx = sce.getServletContext();

        // Web設定
        OtpWebConfig config = OtpWebConfig.defaultConfig();

        // OTPポリシー
        OtpPolicy policy = OtpPolicy.defaultPolicy();

        // 永続化
        DataSource ds = /* アプリ側で用意 */;
        OtpChallengeStore store = new JdbcOtpChallengeStore(ds);

        // メール送信（Simple Java Mail）
        SmtpConfig smtp = new SmtpConfig(
            "smtp.example.com", 587,
            "user", "pass",
            SmtpConfig.Transport.STARTTLS,
            "no-reply@example.com", "YourApp",
            10000, 10000, 10000
        );
        OtpMailer mailer = new SimpleJavaMailOtpMailer(smtp);

        // MFAサービス
        var service = new ServletEmailOtp2faService(
            "YourApp",
            policy,
            Clock.systemUTC(),
            store,
            mailer,
            new DefaultOtpMailTemplate()
        );

        // Servlet登録
        ctx.addServlet("otpStartServlet", new OtpStartServlet(service))
           .addMapping(config.startPath());

        ctx.addServlet("otpVerifyServlet", new OtpVerifyServlet(service, config))
           .addMapping(config.verifyPath());

        // Filter登録
        ctx.addFilter("mfaEnforcerFilter", new MfaEnforcerFilter(config))
           .addMappingForUrlPatterns(null, false, "/*");
    }
}
```

---

## 5. MFA画面（/mfa）の実装例

`/mfa` 画面は **アプリ側で自由に実装**します（JSP / HTML など）。

### 5.1 OTP送信ボタン

```html
<form action="${pageContext.request.contextPath}/mfa/start" method="post">
  <button type="submit">確認コードを送信</button>
</form>
```

### 5.2 OTP入力フォーム

```html
<form action="${pageContext.request.contextPath}/mfa/verify" method="post">
  <label>確認コード</label>
  <input name="otp"
         inputmode="numeric"
         pattern="[0-9]{6}"
         maxlength="6"
         required />
  <button type="submit">確認</button>
</form>
```

---

## 6. Filter の挙動（MfaEnforcerFilter）

`OtpWebConfig.protectedPathPrefixes` に含まれるパスに対して：

* `PASSWORD_OK=true` かつ `MFA_OK!=true`
  → `/mfa` へリダイレクト
* `MFA_OK=true`
  → 通過

以下の URL は常に通過します。

* `/mfa`
* `/mfa/start`
* `/mfa/verify`

---

## 7. セキュリティ上の注意

* OTPコードをログ出力しない
* OTP有効期限は短め（例：5分）
* 試行回数制限を必ず有効化
* 本番環境では HTTPS を使用
* ログイン成功時はセッション再生成（Session Fixation 対策）を推奨

---

## 8. よくあるミス

| 症状            | 原因                        |
| ------------- | ------------------------- |
| MFA画面に進まない    | Filter が登録されていない          |
| 400 / 401 エラー | USER_ID / USER_EMAIL が未設定 |
| 何度もOTP要求される   | MFA_OK をセットしていない          |
